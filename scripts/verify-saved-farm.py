#!/usr/bin/env python3
"""Read-only comparison of a stopped Paper 26.2 world's Farm block-state volume.

No entity/block-entity NBT, player, match, reset or original-map gate is certified.
Supports standard gzip/zlib/uncompressed Anvil chunks; unsupported formats fail.
"""
import argparse
import gzip
import importlib.util
import json
from pathlib import Path
import struct
import zlib

spec = importlib.util.spec_from_file_location("pack", Path(__file__).with_name("package-playtest.py"))
pack = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pack)


def canonical(name, properties):
    return name + ("[" + ",".join(f"{k}={v}" for k, v in sorted(properties.items())) + "]" if properties else "")


def compare_saved_volume(farm, world, origin=(0, 80, 0)):
    audit, _ = pack.audit_farm(farm)
    source = pack.read_nbt(gzip.decompress(farm))
    assert all(v < 128 for v in source["BlockData"]), "Expected verified one-byte palette IDs"
    expected = {}
    for state, block_id in source["Palette"].items():
        name, _, raw = state.partition("[")
        properties = dict(p.split("=", 1) for p in raw.rstrip("]").split(",")) if raw else {}
        expected[block_id] = canonical(name, properties)
    region_path = world / "dimensions/minecraft/overworld/region"
    if not region_path.is_dir():
        raise ValueError("Expected Paper 26.2 overworld region directory; stop/save the server first")
    regions, chunks = {}, {}

    def chunk_at(cx, cz):
        if (cx, cz) in chunks:
            return chunks[cx, cz]
        region = (cx // 32, cz // 32)
        if region not in regions:
            regions[region] = (region_path / f"r.{region[0]}.{region[1]}.mca").read_bytes()
        data = regions[region]
        location = struct.unpack_from(">I", data, 4 * ((cx % 32) + 32 * (cz % 32)))[0]
        offset, sectors = location >> 8, location & 255
        if offset < 2 or sectors == 0:
            raise ValueError(f"Missing saved chunk {cx},{cz}")
        start = offset * 4096
        length = struct.unpack_from(">I", data, start)[0]
        if length < 1 or length + 4 > sectors * 4096 or start + length + 4 > len(data):
            raise ValueError("Invalid Anvil chunk length")
        compression = data[start + 4]
        raw = data[start + 5:start + 4 + length]
        if compression == 1:
            raw = gzip.decompress(raw)
        elif compression == 2:
            raw = zlib.decompress(raw)
        elif compression != 3:
            raise ValueError(f"Unsupported Anvil compression/external chunk type {compression}")
        nbt = pack.read_nbt(raw)
        if nbt["xPos"] != cx or nbt["zPos"] != cz:
            raise ValueError("Saved chunk coordinate mismatch")
        sections = {section["Y"]: section.get("block_states") for section in nbt["sections"]}
        chunks[cx, cz] = sections
        return sections

    def block_at(x, y, z):
        states = chunk_at(x // 16, z // 16).get(y // 16)
        if states is None:
            return "minecraft:air"
        palette = states["palette"]
        block_id = 0
        if len(palette) > 1:
            bits = max(4, (len(palette) - 1).bit_length())
            per_long = 64 // bits
            index = ((y & 15) << 8) | ((z & 15) << 4) | (x & 15)
            word = states["data"][index // per_long] & ((1 << 64) - 1)
            block_id = (word >> ((index % per_long) * bits)) & ((1 << bits) - 1)
        state = palette[block_id]
        return canonical(state["Name"], state.get("Properties", {}))

    mismatches, examples = 0, []
    width, length = source["Width"], source["Length"]
    for index, block_id in enumerate(source["BlockData"]):
        y, plane = divmod(index, width * length)
        z, x = divmod(plane, width)
        target = (origin[0] + x, origin[1] + y, origin[2] + z)
        actual = block_at(*target)
        if actual != expected[block_id]:
            mismatches += 1
            if len(examples) < 10:
                examples.append({"position": target, "expected": expected[block_id], "actual": actual})
    return {"status": "PASS" if mismatches == 0 else "FAIL", "farm_sha256": audit["sha256"],
            "scope": "SAVED_BLOCK_STATES_ONLY", "checked_cells": len(source["BlockData"]),
            "chunks_read": len(chunks), "origin": origin, "mismatches": mismatches, "examples": examples,
            "human_match_certified": False, "verified_reset_certified": False, "full_original_map": False}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--farm", type=Path, required=True)
    parser.add_argument("--world", type=Path, required=True, help="Stopped Paper 26.2 world directory")
    parser.add_argument("--origin", type=int, nargs=3, default=(0, 80, 0))
    args = parser.parse_args()
    result = compare_saved_volume(args.farm.read_bytes(), args.world, args.origin)
    print(json.dumps(result, indent=2))
    if result["status"] != "PASS":
        raise SystemExit(1)


if __name__ == "__main__":
    main()
