#!/usr/bin/env python3
"""Mirror published Modrinth versions to CurseForge.

Downloads every version that exists on Modrinth but is not yet recorded in the
state file, and uploads it to the configured CurseForge project using the
official Upload API (https://minecraft.curseforge.com/api).

Configuration via environment variables:
  MODRINTH_SLUG           Modrinth project slug (required)
  CURSEFORGE_PROJECT_ID   numeric CurseForge project id (required)
  CURSEFORGE_TOKEN        CurseForge API token (required)
  MIRROR_STATE_FILE       state file path
                          (default: scripts/curseforge-mirror.json)
  MIRROR_DRY_RUN          "1" to only report what would be uploaded

The state file records the Modrinth version ids that were already mirrored.
The CI workflow commits it back to the repository after each run, so the next
run only uploads genuinely new versions.
"""

import json
import os
import sys
import tempfile
import time
import urllib.error
import urllib.request
import uuid

MODRINTH_API = "https://api.modrinth.com/v2"
CF_API = "https://minecraft.curseforge.com/api"
USER_AGENT = "modrinth-curseforge-mirror/1.0 (+github-actions)"

# Modrinth loader slugs -> CurseForge game version names.
LOADER_NAMES = {
    "forge": "Forge",
    "fabric": "Fabric",
    "neoforge": "NeoForge",
    "quilt": "Quilt",
    "paper": "Paper",
    "spigot": "Spigot",
    "bukkit": "Bukkit",
    "purpur": "Purpur",
}

# Modrinth version_type -> CurseForge releaseType (same strings, kept explicit).
RELEASE_TYPES = {"release": "release", "beta": "beta", "alpha": "alpha"}

UPLOAD_INTERVAL_SECONDS = 5


def log(message: str) -> None:
    print(message, flush=True)


def warn(message: str) -> None:
    # Surface as an annotation in GitHub Actions.
    print(f"::warning::{message}", flush=True)


def request(url: str, data: bytes | None = None, headers: dict | None = None,
            timeout: int = 120, method: str | None = None) -> tuple[int, bytes]:
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("User-Agent", USER_AGENT)
    for key, value in (headers or {}).items():
        req.add_header(key, value)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.read()
    except urllib.error.HTTPError as exc:
        return exc.code, exc.read()


def fetch_json(url: str) -> object:
    status, body = request(url)
    if status != 200:
        raise RuntimeError(f"GET {url} failed with HTTP {status}: {body[:300]!r}")
    return json.loads(body)


def load_state(path: str) -> dict:
    if os.path.exists(path):
        with open(path, encoding="utf-8") as fh:
            state = json.load(fh)
    else:
        state = {}
    state.setdefault("mirrored_version_ids", [])
    return state


def save_state(path: str, state: dict) -> None:
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(state, fh, indent=2, sort_keys=True)
        fh.write("\n")


def build_metadata(version: dict, slug: str) -> dict:
    loaders = [LOADER_NAMES[l] for l in version["loaders"] if l in LOADER_NAMES]
    changelog = (version.get("body") or "").strip()
    if not changelog:
        changelog = (
            f"Released on Modrinth: "
            f"https://modrinth.com/mod/{slug}/version/{version['id']}"
        )
    return {
        "changelog": changelog,
        "changelogType": "markdown",
        "displayName": version["name"],
        "gameVersionNames": loaders + version["game_versions"],
        "releaseType": RELEASE_TYPES.get(version["version_type"], "release"),
    }


def upload_file(project_id: str, token: str, filename: str, blob: bytes,
                metadata: dict) -> tuple[int, bytes]:
    boundary = uuid.uuid4().hex

    def field(name: str, value: str) -> bytes:
        return (
            f"--{boundary}\r\n"
            f'Content-Disposition: form-data; name="{name}"\r\n\r\n'
            f"{value}\r\n"
        ).encode("utf-8")

    body = b"".join([
        field("metadata", json.dumps(metadata)),
        (
            f"--{boundary}\r\n"
            f'Content-Disposition: form-data; name="file"; '
            f'filename="{filename}"\r\n'
            f"Content-Type: application/java-archive\r\n\r\n"
        ).encode("utf-8"),
        blob,
        b"\r\n",
        f"--{boundary}--\r\n".encode("utf-8"),
    ])
    return request(
        f"{CF_API}/projects/{project_id}/upload-file",
        data=body,
        headers={
            "X-Api-Token": token,
            "Content-Type": f"multipart/form-data; boundary={boundary}",
        },
        timeout=600,
        method="POST",
    )


def main() -> int:
    slug = os.environ.get("MODRINTH_SLUG", "").strip()
    project_id = os.environ.get("CURSEFORGE_PROJECT_ID", "").strip()
    token = os.environ.get("CURSEFORGE_TOKEN", "").strip()
    state_file = os.environ.get("MIRROR_STATE_FILE", "scripts/curseforge-mirror.json")
    dry_run = os.environ.get("MIRROR_DRY_RUN") == "1"

    missing = [name for name, value in {
        "MODRINTH_SLUG": slug,
        "CURSEFORGE_PROJECT_ID": project_id,
        "CURSEFORGE_TOKEN": token,
    }.items() if not value]
    if missing:
        warn(
            "CurseForge mirroring is not configured yet; missing: "
            + ", ".join(missing)
            + ". Set the repository variables/secrets to enable it."
        )
        return 0

    versions = fetch_json(f"{MODRINTH_API}/project/{slug}/version")
    versions.sort(key=lambda v: v["date_published"])
    state = load_state(state_file)
    mirrored = state["mirrored_version_ids"]

    pending = [v for v in versions if v["id"] not in mirrored]
    log(f"Modrinth project '{slug}': {len(versions)} versions, "
        f"{len(pending)} to mirror.")

    failures = 0
    for version in pending:
        metadata = build_metadata(version, slug)
        ok = True
        for file_info in version["files"]:
            filename = file_info["filename"]
            if dry_run:
                log(f"[dry-run] would upload {filename} "
                    f"({version['name']})")
                continue
            log(f"Downloading {filename} ...")
            status, blob = request(file_info["url"], timeout=600)
            if status != 200:
                log(f"ERROR: download failed with HTTP {status}")
                ok = False
                break
            log(f"Uploading {filename} to CurseForge project "
                f"{project_id} ...")
            status, resp = upload_file(project_id, token, filename, blob,
                                       metadata)
            if status not in (200, 201):
                log(f"ERROR: upload failed with HTTP {status}: "
                    f"{resp[:500]!r}")
                ok = False
                break
            log(f"Uploaded {filename} -> CurseForge file id "
                f"{resp.decode('utf-8', 'replace').strip()}")
            time.sleep(UPLOAD_INTERVAL_SECONDS)
        if ok:
            mirrored.append(version["id"])
            if not dry_run:
                save_state(state_file, state)
        else:
            failures += 1

    if dry_run:
        log("[dry-run] state file not modified.")
    log(f"Done: {len(pending) - failures}/{len(pending)} versions mirrored "
        f"(CurseForge moderation applies before files go live).")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
