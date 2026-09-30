# Farm asset scope — 2026-09-30

The verified `ImprovedFarm.schem` is a **community-adjusted track and tower-spot asset**, not a complete original CubeCraft Farm world. Farm import, geometry, placement, paste and verified-reset code can be near implementation completion while full scene reconstruction remains unfinished. These are separate deliverables.

## Direct provenance

[Shotgun's Farm Improvements thread, 13 April 2022](https://www.cubecraft.net/threads/%F0%9F%8C%BE-farm-improvements-%E2%9A%92%EF%B8%8F.309871/) describes the author's earlier track design and proposes changed tower spots/Guard areas. The downloadable item is the adjusted track. Its claim that the track fits most of the existing map concerns replacement compatibility, not world completeness or recreation fidelity. Original and adjusted screenshots are separately labelled in the post.

The implementation's verified checksum is:

`28d24136afe80358b89556d0fbe3b08d00e518af38c7c518819fa7fc225e613e`

No original full-world download has been verified in the sources checked so far. This does not prove that one does not exist. Original screenshots can ground visible scene features, but hidden blocks and exact dimensions remain unverified until stronger evidence is obtained. The eight earlier local JPEG references show Hypixel TowerWars and cannot ground CubeCraft Farm reconstruction.

## Actual file audit

Counts below were computed from the hash-verified Sponge v2 NBT/VarInt block array, rather than inferred from its small compressed size or from screenshots. `scripts/package-playtest.py` reproduces the audit.

| Property | Result |
| --- | --- |
| Compressed file | 4,661 bytes |
| Volume X × Y × Z | 125 × 18 × 191 |
| Total cells | 429,750 |
| Non-air cells | 24,420 |
| Air cells | 405,330 |
| Occupied local Y layers | 2–9 |
| Palette states | 17 |
| Block entities | 2 |
| Schematic entities | 0 |
| Original full world/region files | Absent from this asset |

The palette comprises coloured path/spot underlays, five plank types, polished andesite, spruce logs, smooth-stone slabs, spawn-marker glass, iron blocks and two beacons. It provides the thin track/spot geometry used by the existing runtime. It does not contain the full original Farm terrain/landscape or a verified original lobby. Any scenery inferred only from a screenshot must retain its reconstruction provenance; it must not replace the verified track bytes or silently alter route/spot/reset assumptions.

## Playable flow versus completed map

| Deliverable | Current evidence |
| --- | --- |
| Plugin compile/domain fixtures/shadowJar/two clean Paper boots | CI PASS |
| Verified adjusted track, import/binding/paste/reset implementation | Available for Engineering playtest |
| Fresh-server install kit | Reproducible from green artifact + locally obtained track |
| Full two-player natural-win round | Human-server certification pending |
| Consecutive rounds and verified live reset/recovery | Human-server certification pending |
| Original Farm scene/terrain/buildings/castles/lobby | Full recreation not completed/certified |

The install kit includes a material-coloured top view generated from actual voxels. That diagram is an audit of this asset, not an artist's image of a finished Farm. It records X/Z axes and excludes blank space from any scenery claim.

## Remaining map work

1. Recover original full-world assets if a verifiable source becomes available; otherwise collect original Farm scene references with coordinates/dimensions where possible.
2. Compare original track/spot layout against the adjusted 2022 track before claiming original geometry fidelity.
3. Reconstruct visible structures/terrain with per-component provenance and explicit limits for unseen regions.
4. Bind the completed scene without changing route/spot semantics unnoticed; extend reset bounds/evidence if any gameplay-mutated scenery is added outside the current verified volume.
5. Complete a two-player natural-win round and a second consecutive round on the deployed map. Automated boot or a generated ZIP cannot substitute for those observations.
