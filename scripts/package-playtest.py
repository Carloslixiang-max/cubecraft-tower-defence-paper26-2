#!/usr/bin/env python3
"""Assemble a private Farm playtest kit from a green CI artifact and owned map.

Only stdlib is required. The public repository never receives the map bytes.
This packages existing runtime evidence; it does not certify a played match.
"""
import argparse
from collections import Counter
import gzip
import hashlib
import io
import json
from pathlib import Path
import re
import struct
import zipfile

FARM_SHA = "28d24136afe80358b89556d0fbe3b08d00e518af38c7c518819fa7fc225e613e"
FARM_SOURCE = "https://www.cubecraft.net/threads/309871/"


def digest(data):
    return hashlib.sha256(data).hexdigest()


def read_nbt(data):
    stream = io.BytesIO(data)

    def take(size):
        value = stream.read(size)
        if len(value) != size:
            raise ValueError("Truncated NBT")
        return value

    def number(fmt):
        return struct.unpack(">" + fmt, take(struct.calcsize(fmt)))[0]

    def string():
        return take(number("H")).decode("utf-8")

    def payload(tag):
        if tag in {1, 2, 3, 4, 5, 6}:
            return number({1: "b", 2: "h", 3: "i", 4: "q", 5: "f", 6: "d"}[tag])
        if tag == 8:
            return string()
        if tag in {7, 9, 11, 12}:
            child = number("B") if tag == 9 else None
            count = number("i")
            if count < 0 or count > len(data):
                raise ValueError("Invalid NBT array length")
            if tag == 7:
                return take(count)
            if tag == 9:
                return [payload(child) for _ in range(count)]
            return [number("i" if tag == 11 else "q") for _ in range(count)]
        if tag == 10:
            result = {}
            while (child := number("B")) != 0:
                name = string()
                result[name] = payload(child)
            return result
        raise ValueError(f"Unsupported NBT tag {tag}")

    if number("B") != 10:
        raise ValueError("Schematic root is not a compound")
    string()
    result = payload(10)
    if stream.read(1):
        raise ValueError("Trailing NBT bytes")
    return result


def audit_farm(data):
    if digest(data) != FARM_SHA:
        raise ValueError("Farm checksum does not match the verified runtime asset")
    nbt = read_nbt(gzip.decompress(data))
    width, height, length = (nbt[k] for k in ("Width", "Height", "Length"))
    if nbt["Version"] != 2 or (width, height, length) != (125, 18, 191):
        raise ValueError("Unexpected verified Farm format/dimensions")
    inverse = {v: k for k, v in nbt["Palette"].items()}
    ids, value, shift = [], 0, 0
    for byte in nbt["BlockData"]:
        value |= (byte & 127) << shift
        if byte & 128:
            shift += 7
            if shift >= 35:
                raise ValueError("Invalid block VarInt")
        else:
            ids.append(value)
            value, shift = 0, 0
    if shift or len(ids) != width * height * length or set(ids) - inverse.keys():
        raise ValueError("Invalid schematic block array")
    counts = Counter(inverse[v] for v in ids)
    layers = []
    for y in range(height):
        layer = ids[y * width * length:(y + 1) * width * length]
        layers.append(sum(inverse[v] != "minecraft:air" for v in layer))
    report = {
        "asset": "ImprovedFarm.schem", "sha256": FARM_SHA,
        "source": FARM_SOURCE, "classification": "COMMUNITY_ADJUSTED_TRACK_AND_TOWER_SPOTS",
        "full_original_farm_world": False,
        "dimensions": {"x": width, "y": height, "z": length},
        "volume_blocks": len(ids), "non_air_blocks": len(ids) - counts["minecraft:air"],
        "non_air_by_local_y": layers, "palette_counts": dict(sorted(counts.items())),
        "block_entity_count": len(nbt.get("BlockEntities", [])),
        "entity_count": len(nbt.get("Entities", [])),
        "schematic_offset": nbt.get("Offset"),
        "limits": ["Modified track, not evidence of original spot layout",
                   "No complete original world/region files",
                   "Scenery/castles/lobby are not certified as reconstructed",
                   "Full human-server match and Stage-4 gates remain pending"],
    }
    colors = {"air": "#182030", "blue_terracotta": "#5365ac", "blue_wool": "#315bd4",
              "red_wool": "#bf3038", "red_terracotta": "#a35647", "oak_planks": "#b89b60",
              "dark_oak_planks": "#4c3823", "birch_planks": "#ddd1a1", "jungle_planks": "#b18259",
              "spruce_planks": "#725535", "polished_andesite": "#a2a7aa",
              "spruce_log": "#6a4f31", "smooth_stone_slab": "#b9b9b9",
              "red_stained_glass": "#ff6565", "blue_stained_glass": "#65b4ff"}
    svg = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {width} {length}">',
           '<title>Verified Improved Farm: schematic top view, X right, Z down</title>',
           f'<rect width="{width}" height="{length}" fill="#182030"/>']
    for z in range(length):
        for x in range(width):
            for y in reversed(range(height)):
                state = inverse[ids[x + z * width + y * width * length]]
                if state != "minecraft:air":
                    key = state.removeprefix("minecraft:").split("[")[0]
                    svg.append(f'<rect x="{x}" y="{z}" width="1" height="1" fill="{colors.get(key, "#aaaaaa")}"/>')
                    break
    return report, "\n".join(svg + ["</svg>"])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--artifact", type=Path, required=True, help="Downloaded green Actions artifact ZIP")
    parser.add_argument("--farm", type=Path, required=True, help="Locally owned verified ImprovedFarm.schem")
    parser.add_argument("--head", required=True, help="Artifact's source commit, not necessarily latest HEAD")
    parser.add_argument("--run", type=int, required=True, help="Green Actions run producing that artifact")
    parser.add_argument("--output", type=Path, required=True, help="New ZIP path; existing file is never replaced")
    parser.add_argument("--deployment-evidence", type=Path, help="Optional successful console deployment directory for the identical plugin bytes")
    args = parser.parse_args()
    if not re.fullmatch("[0-9a-f]{40}", args.head) or args.run <= 0:
        parser.error("A full commit SHA and positive Actions run ID are required")
    farm = args.farm.read_bytes()
    report, svg = audit_farm(farm)
    with zipfile.ZipFile(args.artifact) as artifact:
        names = [n for n in artifact.namelist() if n.startswith("libs/") and n.endswith(".jar")]
        if len(names) != 1:
            raise ValueError("Expected exactly one plugin JAR in CI artifact")
        plugin = artifact.read(names[0])
        logs = {f"paper-smoke-{i}.log": artifact.read(f"paper-smoke-{i}.log") for i in (1, 2)}
    with zipfile.ZipFile(io.BytesIO(plugin)) as jar:
        metadata = jar.read("plugin.yml").decode()
    if "name: CubeCraftTowerDefence" not in metadata or "api-version: '26.2'" not in metadata:
        raise ValueError("Unexpected plugin metadata")
    paper_builds = []
    for name, raw in logs.items():
        log = raw.decode()
        if "paperAdapters=LIVE_ADAPTER_SMOKE_PASSED" not in log or not re.search(r"shell v\d+ disabled; clean=true", log):
            raise ValueError(f"Missing clean Paper adapter smoke evidence in {name}")
        build = re.search(r"Loading Paper 26\.2-(\d+)-", log)
        if not build:
            raise ValueError(f"Missing Paper 26.2 boot identity in {name}")
        paper_builds.append(int(build[1]))
    if len(set(paper_builds)) != 1:
        raise ValueError("The two Paper boot builds differ")
    match = re.search(r"domain fixtures: (\d+)/(\d+) PASS", logs["paper-smoke-1.log"].decode())
    if not match or match[1] != match[2]:
        raise ValueError("Missing fully passing domain fixture evidence")
    manifest = {"source_commit": args.head, "actions_run": args.run,
                "paper_version": "26.2", "paper_build": paper_builds[0], "java_required": 25,
                "domain_fixtures_passed": int(match[1]), "paper_smoke_boots": 2,
                "certification": "CI_BOOT_ONLY_HUMAN_MATCH_PENDING", "full_original_map": False,
                "plugin_sha256": digest(plugin), "farm_sha256": FARM_SHA,
                "ci_artifact_sha256": digest(args.artifact.read_bytes())}
    template = Path(__file__).resolve().parent.parent / "playtest"
    entries = {
        "plugins/CubeCraftTowerDefence.jar": plugin,
        "plugins/CubeCraftTowerDefence/maps/ImprovedFarm.schem": farm,
        "evidence/farm-audit.json": json.dumps(report, ensure_ascii=False, indent=2).encode(),
        "evidence/farm-top-view.svg": svg.encode(),
        "evidence/build-manifest.json": json.dumps(manifest, indent=2).encode(),
        **{f"evidence/{name}": raw for name, raw in logs.items()},
        **{name: (template / name).read_bytes() for name in ("README.zh-CN.md", "start.sh", "start.bat", "server.properties")},
    }
    if args.deployment_evidence:
        directory = args.deployment_evidence
        evidence_bytes = (directory / "deployment-report.json").read_bytes()
        evidence = json.loads(evidence_bytes)
        comparison = evidence.get("saved_block_state_comparison", {})
        if (evidence.get("status") != "PASS" or evidence.get("plugin_sha256") != digest(plugin) or
                evidence.get("farm_sha256") != FARM_SHA or comparison.get("status") != "PASS" or
                comparison.get("checked_cells") != report["volume_blocks"] or comparison.get("mismatches") != 0 or
                evidence.get("human_match_certified") is not False or evidence.get("full_original_map") is not False):
            raise ValueError("Deployment evidence does not match this plugin/map or its limited certification scope")
        manifest["console_deployment_source_commit"] = evidence["source_head"]
        manifest["saved_block_state_comparison_cells"] = comparison["checked_cells"]
        entries["evidence/build-manifest.json"] = json.dumps(manifest, indent=2).encode()
        entries["evidence/deployment/deployment-report.json"] = evidence_bytes
        if set(evidence["logs"]) != {f"deployment-boot-{i}.log" for i in (1, 2, 3)}:
            raise ValueError("Expected exactly three deployment boot logs")
        for name, expected_sha in evidence["logs"].items():
            raw = (directory / name).read_bytes()
            if digest(raw) != expected_sha:
                raise ValueError(f"Deployment log checksum mismatch: {name}")
            entries[f"evidence/deployment/{name}"] = raw
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(args.output, "x", compression=zipfile.ZIP_DEFLATED) as output:
        for name, data in entries.items():
            info = zipfile.ZipInfo(name, (2026, 9, 30, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = (0o100755 if name == "start.sh" else 0o100644) << 16
            output.writestr(info, data)
    print(json.dumps({"output": str(args.output.resolve()), "sha256": digest(args.output.read_bytes()),
                      "farm": report, "build": manifest}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
