#!/usr/bin/env python3
"""Download all runtime assets into ./vendor for offline / self-hosted use.

By default the app pulls Pyodide, ONNX Runtime Web, OSMD, Tone.js, the oemer
wheel and the two model checkpoints from public CDNs at runtime (see
config.js). Run this script once if you'd rather serve everything yourself;
the app auto-detects a populated vendor/ directory.

Needs ~700 MB of disk (the Pyodide distribution is large). Only the standard
library is used.
"""

import io
import json
import shutil
import sys
import tarfile
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
VENDOR = ROOT / "vendor"

PYODIDE_VERSION = "0.28.3"
ORT_VERSION = "1.27.0"
OSMD_VERSION = "2.0.0"
TONE_VERSION = "15.1.22"
OEMER_VERSION = "0.1.8"

MODELS = {
    "1st_model.onnx": "https://github.com/BreezeWhite/oemer/releases/download/checkpoints/1st_model.onnx",
    "2nd_model.onnx": "https://github.com/BreezeWhite/oemer/releases/download/checkpoints/2nd_model.onnx",
}


def fetch(url, dest: Path):
    if dest.exists():
        print(f"  ok (cached) {dest.relative_to(ROOT)}")
        return
    dest.parent.mkdir(parents=True, exist_ok=True)
    print(f"  {url}")
    with urllib.request.urlopen(url) as resp, open(dest, "wb") as out:
        shutil.copyfileobj(resp, out)


def fetch_npm(package, version, subdirs):
    """Download an npm tarball and extract selected top-level dirs/files."""
    target = VENDOR / package
    if target.exists():
        print(f"  ok (cached) {target.relative_to(ROOT)}")
        return
    meta = json.load(
        urllib.request.urlopen(f"https://registry.npmjs.org/{package}/{version}")
    )
    url = meta["dist"]["tarball"]
    print(f"  {url}")
    data = urllib.request.urlopen(url).read()
    with tarfile.open(fileobj=io.BytesIO(data), mode="r:gz") as tf:
        for member in tf.getmembers():
            # npm tarballs prefix everything with "package/"
            rel = member.name.split("/", 1)[1] if "/" in member.name else ""
            if any(rel == s or rel.startswith(s + "/") for s in subdirs):
                member.name = rel
                tf.extract(member, target)


def fetch_pyodide():
    target = VENDOR / "pyodide"
    if target.exists():
        print(f"  ok (cached) {target.relative_to(ROOT)}")
        return
    url = (
        "https://github.com/pyodide/pyodide/releases/download/"
        f"{PYODIDE_VERSION}/pyodide-{PYODIDE_VERSION}.tar.bz2"
    )
    print(f"  {url} (~350 MB, be patient)")
    data = urllib.request.urlopen(url).read()
    with tarfile.open(fileobj=io.BytesIO(data), mode="r:bz2") as tf:
        tf.extractall(VENDOR)  # extracts into vendor/pyodide/


def fetch_oemer_wheel():
    meta = json.load(urllib.request.urlopen(f"https://pypi.org/pypi/oemer/{OEMER_VERSION}/json"))
    for f in meta["urls"]:
        if f["filename"].endswith(".whl"):
            fetch(f["url"], VENDOR / "wheels" / f["filename"])
            return
    sys.exit("no oemer wheel found on PyPI")


def main():
    VENDOR.mkdir(exist_ok=True)
    print("Pyodide distribution:")
    fetch_pyodide()
    print("ONNX Runtime Web:")
    fetch_npm("onnxruntime-web", ORT_VERSION, ["dist"])
    print("OpenSheetMusicDisplay:")
    fetch_npm("opensheetmusicdisplay", OSMD_VERSION, ["build"])
    print("Tone.js:")
    fetch_npm("tone", TONE_VERSION, ["build"])
    print("oemer wheel:")
    fetch_oemer_wheel()
    print("Model checkpoints:")
    for name, url in MODELS.items():
        fetch(url, VENDOR / "models" / name)
    print("\nDone. Serve with: python3 serve.py")


if __name__ == "__main__":
    main()
