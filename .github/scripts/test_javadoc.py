"""Regression tests for publication routing, preservation, and static navigation."""

import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

import javadoc


COMMIT = "a" * 40
OTHER_COMMIT = "b" * 40


class JavadocTest(unittest.TestCase):
    """Exercise publication in isolated site and artifact directories under root."""

    def setUp(self):
        """Create root, site, and docs paths that cannot affect the working checkout."""
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        self.site = self.root / "site"
        self.site.mkdir()
        self.docs = self.root / "docs"
        self.docs.mkdir()

    def release(self, version="1.0.0", commit=COMMIT, tag=None):
        """Create metadata matching a published GitHub release event."""
        tag = tag or version
        event = {"action": "published", "release": {
            "tag_name": tag, "draft": False, "prerelease": False,
        }}
        return javadoc.build_info(version, "release", f"refs/tags/{tag}", commit, 10, event)

    def master(self, run_number=10, commit=COMMIT):
        """Create metadata for one successful master workflow run."""
        return javadoc.build_info(
            "1.0.0-SNAPSHOT", "push", "refs/heads/master", commit, run_number, {}
        )

    def artifact(self, info, content="<html>API</html>"):
        """Create the required generated documentation and its build metadata."""
        (self.docs / "index.html").write_text(content, encoding="utf-8")
        (self.docs / "element-list").write_text("example\n", encoding="utf-8")
        (self.docs / javadoc.METADATA_FILE).write_text(json.dumps(info), encoding="utf-8")

    def test_buildInfo_shouldAcceptMasterAndManualMasterBuilds(self):
        # setup
        events = ("push", "workflow_dispatch")
        # execute
        results = [javadoc.build_info(
            "1.0.0-SNAPSHOT", event, "refs/heads/master", COMMIT, 20, {}
        ) for event in events]
        # verify
        self.assertTrue(all(info["channel"] == "master" for info in results))

    def test_buildInfo_shouldRejectPullRequestsAndReleaseBranchPushes(self):
        # setup
        events = (("pull_request", "refs/heads/master"), ("push", "refs/heads/release/1.0.x"))
        for event, ref in events:
            with self.subTest(event=event, ref=ref):
                # execute
                with self.assertRaisesRegex(ValueError, "requires a master build"):
                    javadoc.build_info("1.0.0", event, ref, COMMIT, 20, {})
                # verify
                self.assertEqual([], list(self.site.iterdir()))

    def test_buildInfo_shouldAcceptStableReleaseTagsWithOptionalVPrefix(self):
        # setup
        tags = ("1.0.2", "v1.0.2")
        # execute
        results = [self.release("1.0.2", tag=tag) for tag in tags]
        # verify
        self.assertTrue(all(info["channel"] == "release" for info in results))
        self.assertTrue(all(info["version"] == "1.0.2" for info in results))

    def test_buildInfo_shouldRejectVersionMismatchAndSnapshotReleases(self):
        # setup
        versions = (("1.0.1", "1.0.2"), ("1.0.2-SNAPSHOT", "1.0.2"))
        for version, tag in versions:
            with self.subTest(version=version):
                # execute
                with self.assertRaisesRegex(ValueError, "must match"):
                    self.release(version, tag=tag)
                # verify
                self.assertEqual([], list(self.site.iterdir()))

    def test_buildInfo_shouldRejectDraftsAndPrereleases(self):
        # setup
        for flag in ("draft", "prerelease"):
            event = {"action": "published", "release": {
                "tag_name": "1.0.0", "draft": False, "prerelease": False, flag: True,
            }}
            # execute
            with self.assertRaisesRegex(ValueError, "published stable releases"):
                javadoc.build_info("1.0.0", "release", "refs/tags/1.0.0", COMMIT, 20, event)
            # verify
            self.assertEqual([], list(self.site.iterdir()))

    def test_releaseVersion_shouldRejectUnsafeOrNonstableVersions(self):
        # setup
        versions = ("../1.0.0", "1.0.0/other", "1.0.0-rc1", "01.0.0", "1.0", "1.0.0\n")
        for version in versions:
            with self.subTest(version=version):
                # execute
                with self.assertRaises(ValueError):
                    javadoc.release_version(version)
                # verify
                self.assertEqual([], list(self.site.iterdir()))

    def test_publish_shouldRouteOnePointZeroAndFutureMajorReleases(self):
        # setup
        versions = ("1.0.3", "1.1.0", "2.0.0")
        # execute
        for version in versions:
            self.artifact(self.release(version))
            javadoc.publish(self.site, self.docs)
        # verify
        for version in versions:
            series = ".".join(version.split(".")[:2])
            self.assertEqual(version, javadoc.read_info(self.site / "javadoc/release" / series)["version"])

    def test_publish_shouldReplaceOnlyTheSelectedSeriesAndRemoveObsoleteClasses(self):
        # setup
        self.artifact(self.release("1.1.0"))
        javadoc.publish(self.site, self.docs)
        self.artifact(self.release("1.0.0"))
        javadoc.publish(self.site, self.docs)
        target = self.site / "javadoc/release/1.0"
        (target / "RemovedClass.html").write_text("old class", encoding="utf-8")
        self.artifact(self.release("1.0.1"))
        # execute
        javadoc.publish(self.site, self.docs)
        # verify
        self.assertFalse((target / "RemovedClass.html").exists())
        self.assertEqual("1.0.1", javadoc.read_info(target)["version"])
        self.assertEqual("1.1.0", javadoc.read_info(self.site / "javadoc/release/1.1")["version"])

    def test_publish_shouldSkipAnOlderPatchRelease(self):
        # setup
        self.artifact(self.release("1.0.10"), "new patch")
        javadoc.publish(self.site, self.docs)
        self.artifact(self.release("1.0.9"), "old patch")
        # execute
        changed = javadoc.publish(self.site, self.docs)
        # verify
        self.assertFalse(changed)
        self.assertEqual("new patch", (self.site / "javadoc/release/1.0/index.html").read_text())

    def test_publish_shouldRejectAReleaseWhoseTagChangedCommit(self):
        # setup
        self.artifact(self.release())
        javadoc.publish(self.site, self.docs)
        self.artifact(self.release(commit=OTHER_COMMIT))
        # execute
        with self.assertRaisesRegex(ValueError, "cannot change its source commit"):
            javadoc.publish(self.site, self.docs)
        # verify
        self.assertEqual(COMMIT, javadoc.read_info(self.site / "javadoc/release/1.0")["commit"])

    def test_publish_shouldAllowRerunningAnUnchangedRelease(self):
        # setup
        self.artifact(self.release())
        javadoc.publish(self.site, self.docs)
        # execute
        changed = javadoc.publish(self.site, self.docs)
        # verify
        self.assertTrue(changed)
        self.assertEqual(COMMIT, javadoc.read_info(self.site / "javadoc/release/1.0")["commit"])

    def test_publish_shouldSkipDelayedMasterBuildWithoutChangingReleaseDocs(self):
        # setup
        self.artifact(self.release())
        javadoc.publish(self.site, self.docs)
        self.artifact(self.master(20, OTHER_COMMIT), "new master")
        javadoc.publish(self.site, self.docs)
        self.artifact(self.master(19), "old master")
        # execute
        changed = javadoc.publish(self.site, self.docs)
        # verify
        self.assertFalse(changed)
        self.assertEqual("new master", (self.site / "javadoc/master/index.html").read_text())
        self.assertEqual("1.0.0", javadoc.read_info(self.site / "javadoc/release/1.0")["version"])

    def test_publish_shouldKeepLatestReleaseIndependentOfDeploymentOrder(self):
        # setup
        versions = ("0.10.0", "1.0.0", "0.9.1")
        # execute
        for version in versions:
            self.artifact(self.release(version))
            javadoc.publish(self.site, self.docs)
        # verify
        index = (self.site / "index.html").read_text()
        self.assertLess(index.index("1.0 — release series"), index.index("0.10 — release series"))
        self.assertLess(index.index("0.10 — release series"), index.index("0.9 — release series"))
        self.assertIn('url=1.0/', (self.site / "javadoc/release/index.html").read_text())

    def test_publish_shouldPreserveManuallyArchivedDocsAndOldClassLinksAndDomain(self):
        # setup
        legacy = self.site / "javadoc"
        legacy.mkdir()
        cached_index = '<title>animatedarchitecture 0.7.2 API</title><meta name="dc.created" content="2026-01-03">'
        (legacy / "index.html").write_text(cached_index, encoding="utf-8")
        (legacy / "Example.html").write_text("cached class", encoding="utf-8")
        (legacy / "stylesheet.css").write_text("cached styles", encoding="utf-8")
        cached = legacy / "release/0.7"
        cached.mkdir(parents=True)
        (cached / "index.html").write_text(cached_index, encoding="utf-8")
        (cached / "stylesheet.css").write_text("cached styles", encoding="utf-8")
        (cached / javadoc.METADATA_FILE).write_text(json.dumps({
            "channel": "release", "version": "0.7.2", "commit": None,
            "run_number": 0, "built_at": "2026-01-03",
        }), encoding="utf-8")
        (self.site / "CNAME").write_text("animatedarchitecture.pim16aap2.nl", encoding="utf-8")
        self.artifact(self.master())
        # execute
        javadoc.publish(self.site, self.docs)
        javadoc.publish(self.site, self.docs)
        # verify
        self.assertEqual(cached_index, (cached / "index.html").read_text())
        self.assertEqual("cached class", (legacy / "Example.html").read_text())
        self.assertEqual("cached styles", (cached / "stylesheet.css").read_text())
        self.assertEqual("2026-01-03", javadoc.read_info(cached)["built_at"])
        self.assertIn("0.7 — release series", (self.site / "index.html").read_text())
        self.assertEqual("animatedarchitecture.pim16aap2.nl", (self.site / "CNAME").read_text())

    def test_publish_shouldNotMigrateLegacyDocs(self):
        # setup
        legacy = self.site / "javadoc"
        legacy.mkdir()
        (legacy / "index.html").write_text("animatedarchitecture 0.7.2 API", encoding="utf-8")
        (legacy / "Example.html").write_text("cached class", encoding="utf-8")
        self.artifact(self.master())
        # execute
        javadoc.publish(self.site, self.docs)
        # verify
        self.assertEqual("cached class", (legacy / "Example.html").read_text())
        self.assertTrue((legacy / "master/index.html").exists())
        self.assertFalse((legacy / "release/0.7").exists())

    def test_publish_shouldRejectIncompleteArtifactWithoutChangingTheSite(self):
        # setup
        self.artifact(self.master())
        (self.docs / "element-list").unlink()
        # execute
        with self.assertRaisesRegex(ValueError, "Incomplete Javadocs"):
            javadoc.publish(self.site, self.docs)
        # verify
        self.assertEqual([], list(self.site.iterdir()))

    def test_publish_shouldRejectSymlinkedArtifactBeforeChangingTheSite(self):
        # setup
        self.artifact(self.master())
        (self.docs / "linked-file").symlink_to(self.root / "outside")
        # execute
        with self.assertRaisesRegex(ValueError, "symbolic links"):
            javadoc.publish(self.site, self.docs)
        # verify
        self.assertEqual([], list(self.site.iterdir()))

    def test_publish_shouldRejectUnsafeMetadataBeforeChangingTheSite(self):
        # setup
        info = self.release()
        info["version"] = "../escape"
        self.artifact(info)
        # execute
        with self.assertRaises(ValueError):
            javadoc.publish(self.site, self.docs)
        # verify
        self.assertEqual([], list(self.site.iterdir()))

    def test_renderIndex_shouldShowMasterFirstWithBuildProvenance(self):
        # setup
        self.artifact(self.release())
        javadoc.publish(self.site, self.docs)
        self.artifact(self.master())
        # execute
        javadoc.publish(self.site, self.docs)
        # verify
        index = (self.site / "index.html").read_text()
        self.assertLess(index.index("Master — recommended"), index.index("1.0 — release series"))
        self.assertIn(f"/commit/{COMMIT}", index)
        self.assertIn("1.0.0-SNAPSHOT", index)
        self.assertTrue((self.site / ".nojekyll").exists())
        self.assertIn("url=../", (self.site / "javadoc/index.html").read_text())

    def test_banner_shouldIdentifyVersionAndLinkBackFromAnyDepth(self):
        # setup
        info = self.master()
        # execute
        banner = javadoc.banner(info)
        # verify
        self.assertIn("Master — development build", banner)
        self.assertIn("1.0.0-SNAPSHOT", banner)
        self.assertIn(f'href="{javadoc.SITE_URL}"', banner)

    def test_main_shouldPrepareMetadataAndDocletOutputFromGitHubEnvironment(self):
        # setup
        pom = self.root / "pom.xml"
        pom.write_text('<project xmlns="http://maven.apache.org/POM/4.0.0"><version>1.0.0-SNAPSHOT</version></project>')
        event = self.root / "event.json"
        event.write_text("{}")
        metadata = self.root / "metadata.json"
        output = self.root / "github-output"
        env = dict(os.environ, GITHUB_EVENT_PATH=str(event), GITHUB_EVENT_NAME="push",
                   GITHUB_REF="refs/heads/master", GITHUB_SHA=COMMIT, GITHUB_RUN_NUMBER="20",
                   GITHUB_OUTPUT=str(output))
        # execute
        subprocess.run([sys.executable, str(Path(javadoc.__file__)), "prepare", "--pom", str(pom),
                        "--output", str(metadata)], env=env, check=True, capture_output=True)
        # verify
        self.assertEqual("master", json.loads(metadata.read_text())["channel"])
        self.assertIn("publish=true\ntop=", output.read_text())


if __name__ == "__main__":
    unittest.main()
