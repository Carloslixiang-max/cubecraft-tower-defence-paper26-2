# Original map and tower asset recovery — 2026-10-17

**Later update:** the user subsequently authorized approximate reconstruction.
v114 now supplies a generated Farm scene and all tower-family models; see
[the reconstruction delivery](RECONSTRUCTED_ASSETS.md). The findings below are
the earlier original-file acquisition audit, not the current approximation status.

## Actual delivery status

The current plugin does **not** include a complete original CubeCraft Tower
Defence world or a verified original tower-model pack. A green build, a working
track importer, or support for external tower models cannot establish that these
assets have been recovered. No asset-completion percentage is justified by those
implementation milestones.

The available Farm asset is still the hash-verified **2022 adjusted track and
tower spots**, not the original 2021 full scene. Its changed spots must also be
distinguished from the original track layout. See [Farm scope](FARM_MAP_SCOPE.md),
[original scene references](FARM_SCENE_REFERENCES_2021.md), and
[tower integration](TOWER_MODEL_INTEGRATION.md).

This investigation did not add original map/model files, change gameplay, or
certify reconstruction. It records new sources and exclusions so subsequent
work can continue from the evidence instead of repeating the same searches.

## What the DeathRun comparison actually establishes

The user's [DeathRun-Classic26 research record](https://github.com/Carloslixiang-max/DeathRun-Classic26/blob/main/docs/maps/TO_BEE_RESEARCH.md)
was read from its current main branch. It identifies a pinned community archive
with `level.dat` and four actual Anvil region files for To Bee Or Not To Bee,
then records successful parsing of 202 chunks and investigation of the surviving
mechanisms. Those world bytes make geometry/topology extraction possible.

The useful method is therefore **recover world bytes, inspect their actual
blocks, establish provenance/version, then bind the gameplay**. An archive URL,
a preview image, or a statement that someone has maps is only a lead until the
files are obtained and inspected. DeathRun's source world is a different game
and cannot supply CubeCraft Farm or its tower structures. Its current research
record also distinguishes ephemeral archive inspection from shipping those
third-party binaries; do not infer a distributable CubeCraft world from it.

## Highest-value unresolved leads

### A reported holder of the old maps

In the [8 February 2024 Tower Defense thread](https://www.cubecraft.net/threads/tower-defense.360551/),
BlueFresa says they have the old maps. The public thread contains no download;
the original poster asks how to receive them, with no public file reply shown.
The claim alone does not verify which maps, versions, completeness, or bytes
survive. The profile was not readable in this investigation. No account login,
private conversation, or message was used.

This is a concrete acquisition lead. A supplied/public world archive would need
the inspections below before being labelled original or added to a playtest.

### A real downloadable tower showcase, explicitly redesigned

[CmdData's 7 March 2019 tower rework thread](https://www.cubecraft.net/threads/idea-rework-towers-prototypes-included.216827/)
links to [`BeachTD.zip`](https://www.mediafire.com/file/1ou1de7wx5mbwxi/BeachTD.zip/file).
The author explicitly describes their own redesigned towers, with substantial
changes to several families. This is **not an original-model pack** merely
because it covers the same tower names.

The public file page reported about 559 KB, but its download redirected to a
repair page; the local request returned a short HTML Site Unavailable response.
No ZIP was successfully retrieved or inspected. It remains unknown whether the
world also contains original comparison models. Do not report any model count,
NBT audit, or integration from that unreceived file. If recovered later, keep
the redesigns separate unless independent evidence identifies original models.

## Sources inspected and excluded as original assets

| Source | Actual observation | Consequence |
| --- | --- | --- |
| [klierdev/CubecraftMapsCDN](https://github.com/klierdev/CubecraftMapsCDN) | Repository tree contains map-preview PNGs and an index, no world/region/schematic files | Preview references only; not a world archive |
| [CutesMouse/Minecraft-TowerDefense](https://github.com/CutesMouse/Minecraft-TowerDefense) | `TowerStructureBank.java` embeds voxel definitions, but uses its own tower roster and three-stage progression; includes Darius, Snowball, Potion, Minigun and other unrelated families | Code-embedded geometry is a useful search avenue, but this source is not verified as CubeCraft models; no remapping/import performed |
| [TheLegendaryPro/TowerDefenseGUI](https://github.com/TheLegendaryPro/TowerDefenseGUI) | README describes a Python automation tool for sending troops on CubeCraft | No map/model archive |
| [Rinter-7/CubeCraft](https://github.com/Rinter-7/CubeCraft) | Unreal Engine project with `.uasset` files | A different voxel game, not the server's maps |
| [Borf/TowerDefence](https://github.com/Borf/TowerDefence) | A different game's source/assets | Not Minecraft world geometry |
| [kreashenz/MCTD](https://github.com/kreashenz/MCTD) | Legacy plugin source; no world/region/schematic files in inspected tree | No recovered original assets |
| [LMiljak/Dower-Tefence](https://github.com/LMiljak/Dower-Tefence) and [DemiLovato69/towerdefenseplugin](https://github.com/DemiLovato69/towerdefenseplugin) | Small plugin projects with no matching world/model pack | No original assets recovered |
| [rdmbtc/cubecraftarc](https://github.com/rdmbtc/cubecraftarc), [av-builds/cubecraftold](https://github.com/av-builds/cubecraftold), [Chocolaic/Cubecraft](https://github.com/Chocolaic/Cubecraft) | Web/voxel-engine, HTML, and Unity projects respectively | Similar repository names are not map provenance |
| [Dwarven Mine submission](https://www.cubecraft.net/threads/tower-defence-dwarven-mine.269033/) | Initial submission describes a track; later staff reply shows the decorated released map | Acceptance and a finished-scene image do not supply a complete world file |
| [Shotgun's Parkour submission](https://www.cubecraft.net/threads/tower-defence-shotguns-parkour.268208/) | Track submission, with scene theme to be added | Not a verified full Farm world |
| [Official current map-submission guidance](https://help.cubecraft.net/en-au/article/map-submissions-kmz3v2/) | Downloadable layout templates are offered for currently listed games | A template is not an archived Tower Defence map; no original TD world obtained |
| [Xmas Room author's showcase](https://www.planetminecraft.com/project/xmas-room---cubecraft-games-map/) | Author's indexed comment says no download is offered | Original visual reference, not an obtained world |
| [2026 recreation post](https://www.reddit.com/r/cubecraftgames/comments/1u2dn5i/recreating_tower_defense_from_cubecraft/) | Discusses a recreation and possible future GitHub publication | No inspected public original asset pack recovered from the post |

The earlier ZakShearman candidate and the non-CubeCraft JPEG references remain
excluded for the reasons already recorded in the existing integration/scope
documents. Do not reclassify later-material or unrelated models as Mature direct
evidence just to fill catalog coverage.

## Newly recovered original visual reference

The [official 21 May 2016 tower-design contest](https://www.cubecraft.net/threads/tower-defence-tower-design-contest.113397/)
uses a Quake tower lineup as its example entry. Its full-resolution image was
retrieved and inspected locally:

- Original image: `http://i.imgur.com/hLLy6Ka.png?1`
- [Forum-proxied image](https://www.cubecraft.net/proxy.php?hash=e0fc943a0a58297b0f5a526519acb982&image=http%3A%2F%2Fi.imgur.com%2FhLLy6Ka.png%3F1)
- Dimensions: **1920 × 1017**
- SHA-256 of retrieved bytes:
  `496439c230f8618f18d3fa5e81756f7134b52103a84e39887bae8b3cdb53126b`

This is a **historical visual reference**, not a recovered schematic or proof
of unchanged 2021 bodies. Visible smoke/web-like features and scattered dirt
must not be silently baked into a static body without identifying which are
geometry, effects, or display staging. The image does not establish hidden
surfaces, all block states, or current stage mapping. No runtime model was
fabricated from it.

The contest's other entrants are proposals for new towers; submission does not
establish that a design became an original production tower. A request to email
for a contestant's world is not a public downloadable original asset pack.

## Acceptance work once an actual asset is obtained

1. Record public/source provenance, date/version, byte length and SHA-256; inspect
   archive entries and NBT rather than relying on the file name.
2. For a world, inventory the actual regions/chunks, palette, non-air envelope,
   entities and legacy mechanics. Compare the complete scene against the
   official map views and identify both sides, surrounding structures and lobby
   separately. Preserve a source copy during any version conversion.
3. For Farm, compare original route/tower spots with the deliberately changed
   2022 track. Resolve anchors, offsets, bounds and reset implications before
   changing runtime binding. Keep original and adjusted assets distinct.
4. For towers, identify each family, stage and branch from evidence; crop actual
   blocks and preserve stairs/slabs/facing. Distinguish entities and effects
   from the static model. Do not automatically remap another project's stages.
5. Import through the existing catalog with per-model hashes/source status,
   then verify placement, upgrades, sell/teardown and block restoration. Model
   coverage is separate from historical geometry fidelity.
6. Run the full Java 25/Paper build and both boot gates for implementation
   changes. Real-client visual comparison and consecutive-round map/reset
   observations remain required before claiming original fidelity.

If only pictures/video are available, reconstruct visible components with
explicit inferred/unknown surfaces; do not relabel those results as extracted
original assets or a certified block-for-block map.
