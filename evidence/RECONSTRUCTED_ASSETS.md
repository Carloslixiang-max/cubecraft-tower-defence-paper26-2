# v117 — Farm and historical tower silhouettes

## v117 visual corrections

All 11 families / 82 catalog entries now use a second silhouette pass against
the actual official lineup and gameplay images below. Archer has open legs,
a log balcony, a visible rail and stepped eaves; Ice has a squat windowed stone
casing rather than an invented tall ice spike. Zeus has a heavy four-support
quartz monument. Sorcerer has a rising bookshelf/oak staircase. Artillery has
recessed grey facades and chiseled battlements; Turret has arched windows,
cobbled insets, a wood mount and paired black barrels. Nether cornices/arches,
Poison's olive columns/wood cap and Quake's sculpted stone arms replace the
earlier generic boxes.

The [official May 2016 update](https://www.cubecraft.net/threads/112415/)
links the actual [Leach gameplay GIF](https://i.imgur.com/BsqI9NH.gif) and
[Quake gameplay GIF](https://i.imgur.com/bZqSagC.gif). Both were downloaded and
multiple native frames inspected. Leach's visible green prismarine body and
interlocking white quartz bands directly contradict the earlier guessed purple
beacon spire; the latter has been removed. Exact hidden surfaces, proportions
and upgrade-stage variations remain reconstruction, not recovered originals.
No numeric balance, footprint, route or Farm scenery changes are made here.

A network-client queue test also reproduced a previously untested real-player
startup failure: Paper 26.2 rejects serializing an empty offhand or cursor.
The snapshot adapter now encodes those as zero-length payloads, restores them
as empty items, and still reads old nonempty NBT payloads. Both Paper boots
check empty/AIR and nonempty round trips. Full network-match results are
reported separately from human visual certification.

The Paper rotation adapter now rotates directional BlockData with coordinates
(stairs, fence/pane connections and log axes). CI places and restores all 82
entries in all four quarter turns. The existing fixed-R0 gameplay placement
policy is unchanged; no original aiming/rotation behavior is inferred.

The preview renderer optionally reads an operator-supplied official client JAR
for block textures (`--client-jar`). These are CPU geometry QA images, not
client screenshots. No Minecraft textures/client files are bundled in the
plugin or source repository. Use packed ice/static light cores so models add
no melting ice, flowing lava or spreading fire to the arena.

On 17 October 2026 the user explicitly accepted an approximate recreation and
authorized filling missing geometry from historical visual references. This
supersedes the earlier requirement to wait for original extracted assets. It
does not authorize claiming that inferred geometry is an original extraction.

## Delivered assets

- A complete decorated Farm scene around the existing two-sided adjusted track:
  a giant striped red barn with two decorated gables/doors, a curved grey roof
  and enclosed interior; two banded white silos; branching apple trees; giant
  chickens, sheep and pigs; stacked red-banded hay; giant crops, pen/perimeter
  fences, continuous ground and a raised viewing platform.
- **11 tower families / 82 valid level/path catalog keys**, generated from the
  actual runtime stage declarations. Shared pre-branch stages are geometrically
  identical for both paths. Later differences are visual approximations. No
  costs, damage, range or speed is changed by this asset work. The Paper-side
  visual/LOS firing point is inferred from the model top, as described below.
- Bundled tower models load when no external catalog exists. Existing external
  catalogs retain precedence and their original strict/incomplete behavior.
- A private playtest kit can include `maps/Farm-Reconstructed.schem` with the
  verified track composed into the scenery, the full model directory and voxel
  previews. Original track bytes are not added to the public repository.

## Visual grounding and limits

The native 1920px [official Farm release images](https://www.cubecraft.net/threads/epic-tower-defence-update-%F0%9F%8F%B0.274681/)
ground the oversized farm theme, barn striping/white crosswork/grey roof, silo
bands, apple trees, animals, wheat and hay/fences. The
[official tower lineup](https://www.cubecraft.net/threads/introducing-tower-defence.96401/)
grounds the oak Archer, stone Artillery/Ice/Turret, Nether-brick Mage/Necromancer,
green Poison, bookshelf Sorcerer and quartz Zeus silhouettes. The
[official Quake example](https://www.cubecraft.net/threads/tower-defence-tower-design-contest.113397/)
and [2021 gameplay screenshot](https://www.cubecraft.net/threads/how-to-defend-in-tower-defense.283335/)
provide additional context. The official launch post also links Vikkstar's
historical gameplay video; video fetching was throttled, so no unviewed video
frames are claimed as measured evidence.

Dimensions, prop positions, rear/interior surfaces, several material choices,
Leach body details and later-tier/branch details are inferred. The 2016 lineup
is historical context, not proof of every 2021 stage. Static orange glass and
glowstone approximate the Mage core without adding flowing lava or spreading
fire to the deterministic arena. The viewing lobby is reconstructed for this
package and is not an exact original pregame lobby. No Fast Fly value is added.

Every model declares `COMMUNITY_RECONSTRUCTION`; a complete catalog means
coverage, not a percentage score for original visual fidelity. Preview PNGs
are CPU renders of the generated voxels/block shapes, not Minecraft screenshots.

The earlier base-corner firing point could start inside a full body and let the
tower obstruct its own ray. Paper now uses an explicitly inferred point above
the current model top (updated automatically on upgrade) for LOS and the visible
attack trace. Only this tower's ledger-owned blocks are skipped along that ray;
external walls, scenery and other towers still obstruct it. Numeric range still
uses the unchanged range origin. Exact original firing-origin fidelity remains
a separate real-client gate.

## Coordinates and map operations

The configured map origin remains the **track** origin. The scene volume is
**245 × 82 × 285**, offset **(-60,-3,-45)** from that origin. Thus origin
`(0,80,0)` reserves the visual box `(-60,77,-45)` to `(184,158,239)` inclusive.
The scene is generated as a Sponge v2 schematic during resource processing;
it is not a generic map name pointing at an empty volume.

Composition copies every non-air source track block exactly, shifts both
beacon entities into the larger volume, and removes scene blocks from track
columns plus one column of padding, through 18 blocks above the highest
source block. This prevents inferred props obstructing a lane or tower spot.
Source-only air gaps may receive reconstructed ground/props; the original
429,750-cell air-inclusive audit therefore remains a **track-only** check.

`farm-scene.enabled: false` is the default for existing servers. When explicitly
enabled, **both** `/ctdpastefarm` and `/ctdresetfarm` use the same composed
volume and shifted origin. Initial paste still requires the entire larger box
to be empty; reset retains its existing maintenance/reuse-gate interlocks and
verification/rollback. All scene bounds are validated before configuration
mutation or paste/reset. Live arena spatial reservations include the scenery
envelope, so another arena cannot overlap the large props while only its small
track is checked.

Use a new empty elevated volume or a fresh world. Enabling a large scene for an
old track does not turn an occupied destination into an authorized overwrite.
Do not swap scene mode while an existing world is in use; deploy the new scene
to an isolated volume and bind it explicitly.

## Validation

- Generator asserts all 11 families / 82 valid path/stage keys and bounded,
  nonempty model voxels; it uses a fixed random seed and fixed gzip timestamp.
- Paper adapter smoke loads every model with strict coverage, canonicalizes all
  model/scene BlockData states, places each family, upgrades through both final
  paths, removes it and checks the restoration ledger is empty.
- A live Paper raytrace from inside a supplied body proves self-occlusion is
  skipped while an independently placed external stone wall still blocks it.
- A separate synthetic composition check verifies source preservation,
  clearance, offset and source immutability on real Paper.
- Full compile, domain fixtures, shadowJar and both real Paper boot/shutdown
  gates remain mandatory. These checks do not certify a human-played round or
  prove unseen original geometry.

Build assets: `python3 scripts/build-reconstruction-assets.py --output <dir>`.
Package from a green artifact with `scripts/package-playtest.py
--reconstructed-scene`; optional `--visual-previews` includes the inspected
Farm/tower PNGs. Generator output is bundled in the JAR and CI artifact.

## v118 network-match recovery correction

After fixing empty-item capture, the real two-client test started a match but
the once-per-second recovery sweep immediately restored both active players to
the hub. A durable in-match snapshot is not an orphaned recovery record. The
automatic restore guard now checks active match ownership both before scheduling
and again inside the delayed callback. Departed players and old-process records
remain recoverable; an explicit restart probe is held until the next process.
Six domain regressions cover ownership transitions, delayed execution, probe
holding and fail-closed ownership errors. This fixes cross-dimension add-on
playability without removing crash journals or disabling recovery sweeps.

## v119 observed tower-menu feedback

Real network clicks placed an Archer, selected its path and upgraded it. The
old menu stayed titled Archer Tower I after its level became II, and the
rangefinder still said Enable after pinning. Upgrade now reopens the current
level title, rangefinder refreshes its actual pinned state in place, and Sell
closes the removed tower's menu. No historical slot/lore semantics are invented.
The existing-server guide no longer hardcodes an obsolete JAR version or
fixture count; the supplied artifact/manifest identifies the current build.

The same match then reproduced a real chunk-unload failure at normal 20 TPS:
`BukkitEntityRuntimeAdapter.move` could no longer find a sent Zombie. The exact
UUID was recovered from the saved entity Anvil region at chunk (0,10), position
(14,88,160.1), proving unload rather than normal core death. Live gameplay
chunks (routes, plots, guards and spawns with padding, excluding decorative
scene-only bounds) now hold reference-counted plugin tickets from before guard
spawn through teardown. Failed start, normal end and shutdown release them;
shared boundaries and pre-existing tickets retain their ownership. Both Paper
boots exercise real ticket reference/release behavior. An unexpected runtime
tick failure also marks Farm integrity unknown before teardown, so an unloaded
entity cannot cause a false clean-reuse result.
