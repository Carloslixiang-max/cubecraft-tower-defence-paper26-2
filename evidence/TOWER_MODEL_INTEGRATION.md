# Tower model catalog and original-fidelity boundary

v114 now bundles a complete **11-family / 82-key visual reconstruction** when no
external catalog exists. It is generated from historical silhouettes with
inferred tier details; see [reconstruction delivery](RECONSTRUCTED_ASSETS.md).
The absence of an extracted original pack below remains true, but no longer
means that the default playable models are the two-block placeholders.

The live controller previously always selected the two-block stone/glass placeholder. It now loads `plugins/CubeCraftTowerDefence/tower-models/catalog.yml` before player capture or world mutation. A supplied model is selected by exact tower ID, level and path. Prices, ranges, combat and upgrade stages continue to come from the existing authoritative tower definitions.

## Catalog

```yaml
format-version: 1
models:
  - file: archer-top-1.yml
    sha256: '<SHA-256 of the model file bytes>'
```

Each filename must be one relative filename in the catalog directory; checksum mismatch, bad block data, duplicate stage, invalid stage, missing provenance or out-of-footprint coordinates aborts match start. Model files are bounded to 1 MiB; the catalog permits at most 100 entries. Every block string is canonicalized using real Paper `BlockData` before reaching the tower ledger.

Model schema (the blocks below are a **synthetic example, not an original Archer**):

```yaml
format-version: 1
tower-id: archer
level: 1
path: TOP
evidence-status: COMMUNITY_RECONSTRUCTION
evidence-refs: [https://example.invalid/synthetic-example]
blocks:
  - {x: 0, y: 0, z: 0, block-data: 'minecraft:oak_planks'}
  - {x: 0, y: 1, z: 0, block-data: 'minecraft:dispenser[facing=north]'}
```

Coordinates are relative to the resolved tower placement **centre**, with the same Y origin as existing tower placement. X/Z must fit the 3x3 or 5x5 footprint, and Y is bounded to 0..63 as an engineering resource envelope, not an original tower-height claim. World-height checks run before placement debit. Runtime rotation remains the existing fixed R0 policy; exact original facing is not recovered by this change.

Missing stages use the explicit engineering placeholder. `tower-models.require-complete: true` refuses incomplete coverage, including an absent catalog. Completeness is **stage coverage**, not proof that supplied geometry is original. Evidence labels/source URLs are recorded declarations; SHA-256 checks establish byte identity, not authorship or historical authenticity. No original model pack is bundled or claimed recovered yet. Model files contain block states only; original block-entity payloads, entities and firing origins are not imported by this format.

## Explicit schematic conversion

`scripts/import-tower-model.py` converts an explicitly sourced Sponge v2 schematic into a new JSON model file accepted by the YAML loader:

```bash
python3 scripts/import-tower-model.py --schematic supplied.schem --tower archer \
  --level 1 --path TOP --anchor 1 0 1 --source-url https://example.org/model-source \
  --evidence-status COMMUNITY_RECONSTRUCTION --block-states-only --output archer-top-1.json
```

The anchor must be chosen from the actual source volume. Schematic `Offset` is retained as source metadata and never silently becomes the runtime placement origin. The converter refuses oversized volumes, invalid stage/anchor, malformed block arrays and output overwrite. It requires explicit acceptance of block-state-only conversion; entity and block-entity counts are retained but their payloads are not imported. Its output includes SHA-256 for the catalog, without certifying original appearance. Five-stage community assets are not automatically remapped to this recreation's branches.

## Owned block-entity rollback

An arena-local snapshot handle retains a placed Paper BlockState for tile blocks already owned by the tower ledger. Upgrade/removal rollback uses that snapshot and releases its handle once the transaction finishes. Unowned map TileStates still refuse placement; this change does not weaken protection for map containers. These handles are not serialized crash journals. Restart residue/reuse remains the existing verified Farm-reset flow.

On both CI boots, a synthetic dispenser model is checksum-loaded and its block data canonicalized. The smoke refuses a tampered model and incomplete strict catalog, injects an upgrade failure after the dispenser has been replaced, verifies the restored inventory and persistent custom data, then performs a successful upgrade and complete block cleanup. This is specific real-Paper evidence for the tested state; it is not certification of all tile types or original tower appearance.

## Newly recovered original reference — 2026-10-09

- Primary gameplay attachment: [2021-04-10_20.38.00.png](https://www.cubecraft.net/attachments/2021-04-10_20-38-00-png.190783/).
- Context: [How To Defend In Tower Defense](https://www.cubecraft.net/threads/how-to-defend-in-tower-defense.283335/), 10 April 2021.
- Retrieved PNG SHA-256: `12a0350e1add2ae9d6a167142a0c29640a95866f1fcdfaa3393c2b2f6b3eb31a`.
- Native image is 854x480. It shows actual post-update tower structures, name labels and combat effects. Wooden Archer rows, compact stone/TNT structures, an ice structure and a nether-brick/fire structure are visible. Occlusion, resolution and unknown branch mapping prevent a complete block-by-block definition.
- It is a gameplay photograph, unlike the 2020 Almanac GUI icons. It reopens one previously unavailable visual-reference lead without changing unsupported numerical fields.

The audit also fetched the third-party `ZakShearman/mc-tower-defence` tree at `c04b1791aa87a7613168dc487e7ce2fbfd7e2d04`. Its Archer assets have five stages and later materials including deepslate/blackstone/netherite; they are not a source of original Mature CubeCraft bodies. They were inspected as candidates and are not installed or redistributed as an original model pack.
