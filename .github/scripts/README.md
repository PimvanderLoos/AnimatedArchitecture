# Javadoc publishing

This directory owns documentation publication for the build workflow. `javadoc.py`
uses only the Python standard library; `test_javadoc.py` exercises version routing,
publication ordering, archived-document preservation, and generated navigation.

## Publication contract

- Successful `master` push builds and manual builds on `master` publish to
  `/javadoc/master/`. Pull requests and other branch builds never publish.
- Publishing a stable GitHub release builds its tag and publishes to
  `/javadoc/release/<major>.<minor>/`. Tags may be `1.0.2` or `v1.0.2`; the tag
  version must match the root Maven project version exactly. Tag pushes alone,
  drafts, and prereleases do not publish documentation.
- Future releases must contain this workflow and its publishing script. No changes
  are needed on `release/0.7.x`: archive its existing 0.7.2 Javadocs manually on
  `gh-pages` before merging this workflow, as described below. CI does not migrate
  or regenerate old documentation.
- Every patch replaces only its release series. An older patch cannot replace a
  newer patch; an older master workflow run cannot replace a newer run. Rerunning
  a release with a different source commit is rejected.
- The publishing job queues all versions in one concurrency group. It reads the
  latest `gh-pages` after acquiring the group, stages the update, regenerates the
  index, and publishes the entire site together. Only this job has write access.

`javadoc-build.json` records the channel, full Maven version, source commit,
workflow run number, and UTC build time. Manually archived documentation may have
an unknown source commit and workflow run number (`null` and `0`, respectively).
Master ordering uses run numbers from this single build workflow, so preserve
the workflow's identity when changing the build configuration.

## Navigation and compatibility

The site root is a static HTML version selector with master listed first. Release
series are sorted numerically, and `/javadoc/release/` redirects to the highest
published release series, independent of publication order. `/javadoc/` redirects
to the selector. These are HTML redirects with normal links as a fallback.

Retain the old individual class pages and their assets at `/javadoc/...` during
the manual migration, so existing deep links still refer to the frozen 0.7.2 docs.
New versioned builds include a standard Javadoc `-top` banner identifying the
version and linking back to the selector. `CNAME` is retained, and `.nojekyll`
ensures the generated site is served directly.

Javadoc errors now fail the Maven build instead of publishing partial output.
Only the root aggregate report is generated: individual module reports cannot
resolve every dependency in the shared delomboked source directory. The deploy
step additionally requires `index.html` and `element-list`.

## One-time manual archive of 0.7.2

Before merging this workflow, use a clean checkout of `gh-pages` to copy the
current Javadocs. Export them first so the destination is not copied into itself:

```bash
migration_dir="$(mktemp -d)"
git archive HEAD javadoc | tar -x -C "$migration_dir"
mkdir -p javadoc/release/0.7
cp -a "$migration_dir/javadoc/." javadoc/release/0.7/
```

Add `javadoc/release/0.7/javadoc-build.json` with the version and the date already
present in the generated 0.7.2 HTML:

```json
{
  "channel": "release",
  "version": "0.7.2",
  "commit": null,
  "run_number": 0,
  "built_at": "2026-01-03"
}
```

Commit and publish the added `javadoc/release/0.7/` directory on `gh-pages`.
Keep the original `javadoc/` files and `CNAME`. No Maven build or edits to the
0.7 release branch are needed. The next master publication discovers the archived
version through its metadata, includes it in the selector, and replaces only
`javadoc/index.html` with the selector redirect. Later publications preserve it.

## Local verification

Run the publishing tests from the repository root:

```bash
python3 -m unittest discover -s .github/scripts -p 'test_*.py' -v
```

The build was verified with Temurin JDK `25.0.4+7` and Maven `3.9.9`. Local Maven
runs need `-Dactions.run.id=0 -Dactions.run.number=0` for filtered build resources.
Reproduce the clean CI build and documentation generation in this order:

```bash
mvn --batch-mode -P=errorprone clean test install checkstyle:checkstyle pmd:check -Dactions.run.id=0 -Dactions.run.number=0
mvn --batch-mode -P=docs -DskipTests install antrun:run lombok:delombok javadoc:aggregate -Dactions.run.id=0 -Dactions.run.number=0
```

`actionlint` 1.7.12 does not yet recognize the documented `concurrency.queue`
property. Validate the rest of the workflow with this narrow exception:

```bash
actionlint -ignore 'unexpected key "queue" for "concurrency"' .github/workflows/build.yml
```

To preview publication without pushing, check out or export `gh-pages` into one
temporary directory and place generated Javadocs plus `javadoc-build.json` in
another. The `publish` command updates only the temporary site:

```bash
python3 .github/scripts/javadoc.py publish --site /tmp/javadoc-site --docs /tmp/javadoc-build
python3 -m http.server 8000 --directory /tmp/javadoc-site
```

Open `http://localhost:8000/`. Deployment requires no Pages settings changes:
continue publishing the root of `gh-pages` with the existing custom domain.
