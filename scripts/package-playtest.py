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
import tempfile
import runpy
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
    parser.add_argument("--reconstructed-scene", action="store_true", help="Include the generated full Farm approximation and complete tower catalog")
    parser.add_argument("--existing-server", action="store_true", help="Data-only add-on ZIP for an existing server; plugin is delivered separately, with no config/world overwrites")
    parser.add_argument("--network-evidence", type=Path, help="Optional successful two-match/restart report for the identical source and JAR")
    parser.add_argument("--visual-previews", type=Path, help="Optional directory with actual voxel preview PNGs")
    args = parser.parse_args()
    if args.existing_server and not args.reconstructed_scene:
        parser.error("--existing-server requires --reconstructed-scene")
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
        reconstruction = ({name: jar.read(name) for name in jar.namelist()
                           if name.startswith("reconstruction/") and not name.endswith("/")}
                          if args.reconstructed_scene else {})
    if "name: CubeCraftTowerDefence" not in metadata or "api-version: '26.2'" not in metadata:
        raise ValueError("Unexpected plugin metadata")
    if args.existing_server and "  ctdinstall:" not in metadata:
        raise ValueError("This plugin lacks the managed-world installer required by the add-on kit")
    paper_builds = []
    for name, raw in logs.items():
        log = raw.decode()
        if "paperAdapters=LIVE_ADAPTER_SMOKE_PASSED" not in log or not re.search(r"(?:shell v\d+|\d+\.\d+\.\d+(?:[-+][\w.-]+)?) disabled; clean=true", log):
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
                "deployment_type": "EXISTING_SERVER_ADD_ON" if args.existing_server else "FRESH_PLAYTEST_SERVER",
                "paper_version": "26.2", "paper_build": paper_builds[0], "java_required": 25,
                "domain_fixtures_passed": int(match[1]), "paper_smoke_boots": 2,
                "certification": "CI_BOOT_ONLY_HUMAN_MATCH_PENDING", "full_original_map": False,
                "plugin_sha256": digest(plugin), "farm_sha256": FARM_SHA,
                "ci_artifact_sha256": digest(args.artifact.read_bytes())}
    network_report = None
    if args.network_evidence:
        network_report = args.network_evidence.read_bytes()
        network = json.loads(network_report)
        if not (network.get("status") == "PASS" and network.get("source_head") == args.head
                and network.get("plugin_sha256") == digest(plugin)
                and network.get("natural_matches", 0) >= 2
                and network.get("consecutive_reuse") is True
                and network.get("cross_restart_lossless") is True
                and network.get("human_match_certified") is False):
            raise ValueError("Network report must pass two natural matches/reuse/restart for this exact source/JAR")
        manifest["certification"] = "AUTOMATED_NETWORK_MATCHES_HUMAN_VISUAL_PENDING"
        manifest["automated_network_matches"] = network["natural_matches"]
        manifest["human_match_certified"] = False
    template = Path(__file__).resolve().parent.parent / "playtest"
    entries = {
        "plugins/CubeCraftTowerDefence.jar": plugin,
        "plugins/CubeCraftTowerDefence/maps/ImprovedFarm.schem": farm,
        "evidence/farm-audit.json": json.dumps(report, ensure_ascii=False, indent=2).encode(),
        "evidence/farm-top-view.svg": svg.encode(),
        "evidence/build-manifest.json": json.dumps(manifest, indent=2).encode(),
        **{f"evidence/{name}": raw for name, raw in logs.items()},
        **({"README.zh-CN.md": (template.parent / "deployment/README.zh-CN.md").read_bytes()} if args.existing_server else
           {name: (template / name).read_bytes() for name in ("README.zh-CN.md", "start.sh", "start.bat", "server.properties")}),
    }
    if network_report is not None:
        entries["evidence/network-report.json"] = network_report
    if args.existing_server:
        del entries["plugins/CubeCraftTowerDefence.jar"]
        for i, token in ((1, "CREATED"), (2, "RELOADED")):
            if f"Managed Farm install smoke PASS: {token}" not in logs[f"paper-smoke-{i}.log"].decode():
                raise ValueError(f"Missing managed-world {token} CI evidence")
    if args.reconstructed_scene:
        source = reconstruction["reconstruction/FarmScenery.schem"]
        asset_manifest = json.loads(reconstruction["reconstruction/manifest.json"])
        if digest(source) != asset_manifest["scene"]["sha256"] or asset_manifest["catalog-stage-keys"] != 82:
            raise ValueError("Bundled reconstruction assets do not match their manifest")
        builder = runpy.run_path(str(Path(__file__).with_name("build-reconstruction-assets.py")))
        source_nbt = read_nbt(gzip.decompress(source))
        scene = builder["Voxels"]()
        w, h, length = (source_nbt[k] for k in ("Width", "Height", "Length"))
        inverse = {i: state for state, i in source_nbt["Palette"].items()}
        ox, oy, oz = source_nbt["Offset"]
        if any(i >= 128 for i in source_nbt["BlockData"]):
            raise ValueError("Expected one-byte reconstruction palette IDs")
        for i, block_id in enumerate(source_nbt["BlockData"]):
            if block_id:
                scene.blocks[i % w + ox, i // (w * length) + oy, (i // w) % length + oz] = inverse[block_id]
        track_nbt = read_nbt(gzip.decompress(farm))
        builder["compose_track"](scene, track_nbt)
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "Farm-Reconstructed.schem"
            complete = builder["write_schematic"](path, scene, block_entities=track_nbt.get("BlockEntities", []))
            entries[("optional-schematics/" if args.existing_server else "maps/") + "Farm-Reconstructed.schem"] = path.read_bytes()
        if not args.existing_server:
            for name, raw in reconstruction.items():
                if name.startswith("reconstruction/tower-models/"):
                    entries["plugins/CubeCraftTowerDefence/tower-models/" + name.rsplit("/", 1)[1]] = raw
            entries["plugins/CubeCraftTowerDefence/config.yml"] = (
                "farm-scene:\n  enabled: true\n"
                "tower-models:\n  require-complete: true\n"
                "map-binding:\n  paste-blocks-per-tick: 8000\n").encode()
        entries["evidence/reconstruction-assets.json"] = reconstruction["reconstruction/manifest.json"]
        manifest.update({"reconstructed_scene_included": True, "full_original_map": False,
                         "reconstruction_scene": complete, "tower_model_stage_keys": 82,
                         "tower_model_classification": "COMMUNITY_RECONSTRUCTION"})
        entries["evidence/build-manifest.json"] = json.dumps(manifest, indent=2).encode()
    if args.visual_previews:
        if not args.reconstructed_scene:
            parser.error("--visual-previews requires --reconstructed-scene")
        for name in ("Farm-preview.png", "Tower-models-preview.png"):
            entries["previews/" + name] = (args.visual_previews / name).read_bytes()
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
    if args.existing_server:
        deployable = {name for name in entries if name.startswith("plugins/")}
        if deployable != {"plugins/CubeCraftTowerDefence/maps/ImprovedFarm.schem"}:
            raise ValueError("Add-on kit would overwrite unexpected plugin data")
        if any(Path(name).name in {"server.properties", "eula.txt", "start.sh", "start.bat", "config.yml", "level.dat"} for name in entries):
            raise ValueError("Add-on kit contains server-wide configuration or an overwrite-prone world/config file")
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
