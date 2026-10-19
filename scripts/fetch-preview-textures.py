#!/usr/bin/env python3
"""Fetch the official client only for CPU texture previews; never ship its JAR."""
import hashlib
import json
from pathlib import Path
import urllib.request

def read(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "CubeCraftTD-asset-QA/1.0"}), timeout=120) as response:
        return response.read()

manifest = json.loads(read("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
entry = next(v for v in manifest["versions"] if v["id"] == "26.2")
version = json.loads(read(entry["url"]))
client = version["downloads"]["client"]
raw = read(client["url"])
assert hashlib.sha1(raw).hexdigest() == client["sha1"]
target = Path("build/preview-client-26.2.jar")
target.write_bytes(raw)
print("Official 26.2 texture source verified; CPU previews are not native-client screenshots")
