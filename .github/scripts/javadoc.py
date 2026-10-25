"""Prepare and publish versioned Javadocs; see README.md for the publishing contract."""

import argparse
from datetime import datetime, timezone
from html import escape
import json
import os
from pathlib import Path
import re
import shutil
import xml.etree.ElementTree as ET


SITE_URL = "https://animatedarchitecture.pim16aap2.nl/"
REPOSITORY_URL = "https://github.com/PimvanderLoos/AnimatedArchitecture"
METADATA_FILE = "javadoc-build.json"
VERSION_PATTERN = r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)"


def release_version(version):
    """Parse a stable major.minor.patch version, rejecting unsafe path components."""
    if not isinstance(version, str) or not re.fullmatch(VERSION_PATTERN, version):
        raise ValueError(f"Expected a stable major.minor.patch version, got {version!r}")
    return tuple(map(int, version.split(".")))


def validate_build(info):
    """Validate incoming build metadata before modifying the published site."""
    channel = info["channel"]
    if channel not in ("master", "release"):
        raise ValueError(f"Unsupported documentation channel: {channel!r}")
    version = info["version"]
    release_version(version.removesuffix("-SNAPSHOT") if channel == "master" else version)
    if not re.fullmatch(r"[0-9a-f]{40}", info["commit"]):
        raise ValueError("A full source commit SHA is required")
    if type(info["run_number"]) is not int or info["run_number"] < 1:
        raise ValueError("A positive workflow run number is required")
    datetime.fromisoformat(info["built_at"])


def build_info(version, event_name, ref, commit, run_number, event):
    """Identify a master build or published stable release and validate its source version."""
    if event_name in ("push", "workflow_dispatch") and ref == "refs/heads/master":
        channel = "master"
    elif event_name == "release" and event.get("action") == "published":
        release = event["release"]
        if release["draft"] or release["prerelease"]:
            raise ValueError("Only published stable releases can update release documentation")
        tag = release["tag_name"]
        tag_version = tag.removeprefix("v")
        release_version(tag_version)
        if version != tag_version or ref != f"refs/tags/{tag}":
            raise ValueError("The release tag, checked-out ref, and Maven version must match")
        channel = "release"
    else:
        raise ValueError("Documentation requires a master build or a published release")

    info = {
        "channel": channel,
        "version": version,
        "commit": commit,
        "run_number": run_number,
        "built_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
    }
    validate_build(info)
    return info


def banner(info):
    """Return the standard doclet's top banner, shared by class and overview pages."""
    label = "Master — development build" if info["channel"] == "master" else "Release"
    return (
        '<div style="padding:0.75em;border-bottom:1px solid #888">'
        f'<strong>{label}: {escape(info["version"])}</strong> · '
        f'{info["commit"][:7]} · <a href="{SITE_URL}">All versions</a></div>'
    )


def read_info(directory):
    """Read provenance for one successfully published documentation directory."""
    return json.loads((directory / METADATA_FILE).read_text(encoding="utf-8"))


def redirect(destination):
    """Create a static redirect with an ordinary link as a no-JavaScript fallback."""
    url = escape(destination, quote=True)
    return (
        '<!doctype html>\n<html lang="en"><head><meta charset="utf-8">'
        f'<meta http-equiv="refresh" content="0; url={url}">'
        '<title>AnimatedArchitecture Javadocs</title></head>'
        f'<body><p><a href="{url}">Open documentation</a></p></body></html>\n'
    )


def index_entry(url, label, info):
    """Render a version link with the exact Maven version, source commit, and build date."""
    details = f'Version {escape(info["version"])}'
    if info["commit"]:
        commit = info["commit"]
        details += f' · <a href="{REPOSITORY_URL}/commit/{commit}">{commit[:7]}</a>'
    if info["built_at"]:
        details += f' · {escape(info["built_at"])}'
    return f'<li><a href="{url}">{label}</a><p>{details}</p></li>'


def render_index(site):
    """Generate navigation from published versions, ordering releases numerically."""
    javadoc = site / "javadoc"
    master = javadoc / "master"
    master_html = "<p>Master documentation has not been published yet.</p>"
    if (master / METADATA_FILE).is_file() and (master / "index.html").is_file():
        master_html = "<ul>" + index_entry(
            "javadoc/master/", "Master — recommended for current development", read_info(master)
        ) + "</ul>"

    releases = []
    for metadata in (javadoc / "release").glob(f"*/{METADATA_FILE}"):
        info = read_info(metadata.parent)
        version = release_version(info["version"])
        series = ".".join(map(str, version[:2]))
        if info["channel"] != "release" or metadata.parent.name != series:
            raise ValueError(f"Release metadata does not match its directory: {metadata}")
        if not (metadata.parent / "index.html").is_file():
            raise ValueError(f"Missing published Javadoc index: {metadata.parent}")
        releases.append((version, series, info))
    releases.sort(reverse=True)
    release_html = "".join(
        index_entry(f"javadoc/release/{series}/", f"{series} — release series", info)
        for _, series, info in releases
    )
    latest_html = ""
    if releases:
        latest_html = '<p><a href="javadoc/release/">Latest release documentation</a></p>'
        (javadoc / "release/index.html").write_text(
            redirect(f"{releases[0][1]}/"), encoding="utf-8"
        )
    (site / "index.html").write_text(f'''<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>AnimatedArchitecture Javadocs</title>
<style>
body {{ font: 1.1rem/1.6 system-ui, sans-serif; max-width: 52rem; margin: 3rem auto; padding: 0 1rem; }}
a {{ color: #075cad; }}
li {{ margin: 1.25rem 0; }}
li > a {{ font-weight: 600; }}
li p {{ margin: 0; font-size: 0.9rem; color: #444; overflow-wrap: anywhere; }}
</style>
</head>
<body>
<main>
<h1>AnimatedArchitecture Javadocs</h1>
<h2>Current development</h2>
{master_html}
<p>Master documentation follows successful builds and may describe unreleased API changes.</p>
<h2>Released versions</h2>
{latest_html}
<ul>{release_html}</ul>
<p>Each release series contains its latest published patch release.</p>
</main>
</body>
</html>
''', encoding="utf-8")
    (javadoc / "index.html").write_text(redirect("../"), encoding="utf-8")
    (site / ".nojekyll").touch()


def publish(site, docs):
    """Replace one channel in a staged site, preserving all other versions."""
    info = read_info(docs)
    validate_build(info)
    for name in ("index.html", "element-list"):
        if not (docs / name).is_file():
            raise ValueError(f"Incomplete Javadocs: missing {name}")
    if any(path.is_symlink() for path in docs.rglob("*")):
        raise ValueError("Javadoc artifacts must not contain symbolic links")
    version = release_version(info["version"].removesuffix("-SNAPSHOT"))
    path = "master" if info["channel"] == "master" else f"release/{version[0]}.{version[1]}"
    target = site / "javadoc" / path
    if (not target.resolve().is_relative_to(site.resolve())
            or docs.resolve().is_relative_to(site.resolve())
            or site.resolve().is_relative_to(docs.resolve())):
        raise ValueError("The incoming artifact and published destination must be separate safe directories")
    if (target / METADATA_FILE).exists():
        previous = read_info(target)
        if info["channel"] == "master":
            stale = info["run_number"] < previous["run_number"]
        else:
            previous_version = release_version(previous["version"])
            stale = version < previous_version
            if version == previous_version and previous["commit"] not in (None, info["commit"]):
                raise ValueError("A published release version cannot change its source commit")
        if stale:
            print(f"Skipping superseded {info['channel']} documentation: {info['version']}")
            return False

    if target.exists():
        shutil.rmtree(target)
    shutil.copytree(docs, target)
    render_index(site)
    return True


def main():
    """Run metadata preparation during the build or site assembly during deployment."""
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    prepare = commands.add_parser("prepare", help="Validate the build and write provenance")
    prepare.add_argument("--pom", type=Path, default=Path("pom.xml"))
    prepare.add_argument("--output", type=Path, required=True)
    deploy = commands.add_parser("publish", help="Update a checkout of gh-pages")
    deploy.add_argument("--site", type=Path, required=True)
    deploy.add_argument("--docs", type=Path, required=True)
    args = parser.parse_args()
    if args.command == "publish":
        publish(args.site, args.docs)
        return

    version = ET.parse(args.pom).findtext("{http://maven.apache.org/POM/4.0.0}version")
    event = json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text(encoding="utf-8"))
    info = build_info(
        version, os.environ["GITHUB_EVENT_NAME"], os.environ["GITHUB_REF"],
        os.environ["GITHUB_SHA"], int(os.environ["GITHUB_RUN_NUMBER"]), event,
    )
    args.output.write_text(json.dumps(info, indent=2) + "\n", encoding="utf-8")
    with Path(os.environ["GITHUB_OUTPUT"]).open("a", encoding="utf-8") as output:
        output.write(f"publish=true\ntop={banner(info)}\n")


if __name__ == "__main__":
    main()
