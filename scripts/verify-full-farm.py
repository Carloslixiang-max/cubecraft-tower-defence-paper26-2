#!/usr/bin/env python3
"""Read-only stopped-world proof: complete reconstructed Farm and tracked entity residue."""
import gzip
import importlib.util
import struct
import tempfile
from pathlib import Path
import zlib

def module(name, filename):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(filename))
    value = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(value)
    return value

pack = module("full_pack", "package-playtest.py")
saved = module("saved", "verify-saved-farm.py")
build = module("build", "build-reconstruction-assets.py")

def entities_in(region):
    data = region.read_bytes()
    for index in range(1024):
        location = struct.unpack_from(">I", data, 4 * index)[0]
        offset, sectors = location >> 8, location & 255
        if offset == 0 and sectors == 0:
            continue
        if offset < 2 or sectors == 0:
            raise ValueError("Invalid entity region location")
        start = offset * 4096
        size = struct.unpack_from(">I", data, start)[0]
        if size < 1 or size + 4 > sectors * 4096 or start + size + 4 > len(data):
            raise ValueError("Invalid entity chunk length")
        compression = data[start + 4]
        raw = data[start + 5:start + 4 + size]
        if compression == 1:
            raw = gzip.decompress(raw)
        elif compression == 2:
            raw = zlib.decompress(raw)
        elif compression != 3:
            raise ValueError("Unsupported entity chunk compression")
        for entity in pack.read_nbt(raw).get("Entities", []):
            yield entity

def verify(root, farm):
    track = pack.read_nbt(gzip.decompress(farm))
    scene = build.compose_track(build.build_scene(), track)
    dimension = root / "ctd-network-verify/dimensions/minecraft/ctd_farm"
    with tempfile.TemporaryDirectory() as temporary:
        schematic = Path(temporary) / "complete.schem"
        build.write_schematic(schematic, scene, block_entities=track.get("BlockEntities", []))
        result = saved.compare_saved_volume(farm, root, (-60, 77, -45),
            schematic=schematic.read_bytes(), region_path=dimension / "region",
            explicit_properties_only=True)
    assert result["status"] == "PASS", str(result)
    assert result["checked_cells"] == 5725650
    tagged = []
    for region in (dimension / "entities").glob("*.mca"):
        for entity in entities_in(region):
            if "cubecrafttd_tracked" in entity.get("Tags", []):
                tagged.append({"id": entity.get("id"), "position": entity.get("Pos")})
    assert not tagged, f"Tracked TD entities survived two rounds/restart: {tagged[:10]}"
    return {"complete_scene": result, "remaining_tracked_entities": len(tagged),
        "scope": "MATERIALS_EXPLICIT_PROPERTIES_AND_TRACKED_ENTITY_RESIDUE",
        "block_entity_payloads_certified": False}
