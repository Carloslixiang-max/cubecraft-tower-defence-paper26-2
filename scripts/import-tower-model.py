#!/usr/bin/env python3
"""Convert an explicitly sourced Sponge v2 model to the block-state catalog format.

Does not infer original geometry, branch identity, firing origin or authenticity.
"""
import argparse
import gzip
import hashlib
import io
import json
from pathlib import Path
import runpy
from urllib.parse import urlparse


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--schematic", type=Path, required=True)
    parser.add_argument("--tower", choices=["archer", "artillery", "ice", "mage", "poison", "quake", "sorcerer", "zeus", "turret", "necromancer", "leach"], required=True)
    parser.add_argument("--level", type=int, required=True)
    parser.add_argument("--path", choices=["TOP", "BOTTOM"], required=True)
    parser.add_argument("--anchor", type=int, nargs=3, required=True, metavar=("X", "Y", "Z"))
    parser.add_argument("--source-url", required=True)
    parser.add_argument("--evidence-status", choices=["MATURE_DIRECT", "MATURE_CONTEXT", "HISTORICAL_REFERENCE", "COMMUNITY_RECONSTRUCTION"], required=True)
    parser.add_argument("--block-states-only", action="store_true", help="Explicitly accept that NBT/entity payloads and firing origins are not imported")
    parser.add_argument("--output", type=Path, required=True, help="New JSON model file (JSON is accepted by YAML loader)")
    args = parser.parse_args()
    if not args.block_states_only:
        parser.error("--block-states-only is required; this format cannot preserve source NBT/entities")
    url = urlparse(args.source_url)
    if url.scheme != "https" or not url.netloc:
        parser.error("Source must be an explicit HTTPS URL")
    footprint = 5 if args.tower in {"turret", "necromancer", "leach"} else 3
    maximum_level = 3 if footprint == 5 else 4
    if not 1 <= args.level <= maximum_level:
        parser.error("Level is outside this recreation's tower stages; no automatic five-stage remapping")
    if not 0 < args.schematic.stat().st_size <= 1_048_576:
        raise ValueError("Source must be at most 1 MiB")
    source = args.schematic.read_bytes()
    raw = gzip.GzipFile(fileobj=io.BytesIO(source)).read(8_388_609) if source[:2] == b"\x1f\x8b" else source
    if len(raw) > 8_388_608:
        raise ValueError("Inflated model exceeds 8 MiB")
    read_nbt = runpy.run_path(str(Path(__file__).with_name("package-playtest.py")))["read_nbt"]
    nbt = read_nbt(raw)
    if nbt.get("Version") != 2:
        raise ValueError("Only Sponge v2 is supported")
    width, height, length = (nbt[k] for k in ("Width", "Height", "Length"))
    if not (1 <= width <= footprint and 1 <= length <= footprint and 1 <= height <= 64):
        raise ValueError("Model volume does not fit the tower footprint and bounded height")
    if not all(0 <= a < bound for a, bound in zip(args.anchor, (width, height, length))):
        raise ValueError("Explicit anchor must be a local coordinate inside the source volume")
    inverse = {v: k for k, v in nbt["Palette"].items()}
    if len(inverse) != len(nbt["Palette"]):
        raise ValueError("Duplicate palette IDs")
    ids, value, shift = [], 0, 0
    for byte in nbt["BlockData"]:
        value |= (byte & 127) << shift
        if byte & 128:
            shift += 7
            if shift >= 35:
                raise ValueError("Oversized block VarInt")
        else:
            ids.append(value)
            value, shift = 0, 0
    if shift or len(ids) != width * height * length:
        raise ValueError("Incomplete model block array")
    blocks = []
    for index, palette_id in enumerate(ids):
        state = inverse[palette_id]
        if state in {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}:
            continue
        x, z, y = index % width, (index // width) % length, index // (width * length)
        x, y, z = x - args.anchor[0], y - args.anchor[1], z - args.anchor[2]
        radius = footprint // 2
        if not (-radius <= x <= radius and -radius <= z <= radius and 0 <= y <= 63):
            raise ValueError("Non-air model voxel exceeds the centre-relative envelope")
        blocks.append({"x": x, "y": y, "z": z, "block-data": state})
    if not blocks:
        raise ValueError("Empty model")
    model = {"format-version": 1, "tower-id": args.tower, "level": args.level, "path": args.path,
             "evidence-status": args.evidence_status, "evidence-refs": [args.source_url], "blocks": blocks,
             "source-schematic-sha256": hashlib.sha256(source).hexdigest(),
             "source-offset-not-runtime-origin": nbt.get("Offset", [0, 0, 0]), "source-anchor": args.anchor,
             "source-block-entity-count": len(nbt.get("BlockEntities", [])),
             "source-entity-count": len(nbt.get("Entities", [])), "payloads-imported": False,
             "original-client-fidelity-certified": False}
    data = (json.dumps(model, ensure_ascii=False, indent=2) + "\n").encode()
    with args.output.open("xb") as output:
        output.write(data)
    print(json.dumps({"file": args.output.name, "sha256": hashlib.sha256(data).hexdigest(),
                      "blocks": len(blocks), "evidence-status": args.evidence_status,
                      "payloads-imported": False, "original-client-fidelity-certified": False}, indent=2))


if __name__ == "__main__":
    main()
