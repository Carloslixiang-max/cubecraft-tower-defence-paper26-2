# CubeCraft Tower Defence — Paper 26.2 recreation

An **unofficial, open-source reimplementation** of the classic CubeCraft Tower Defence gameplay loop for modern **Paper 26.2 / Java 25** servers.

> This project is not affiliated with, endorsed by, or sponsored by CubeCraft Games. CubeCraft names, branding, maps, textures, and other original assets belong to their respective owners. This repository does **not** bundle original CubeCraft proprietary assets.

## Current status

This repository is an active high-fidelity recreation, not a finished drop-in clone yet.

- Paper target: **26.2**
- Java target: **25**
- Kotlin/JVM plugin
- Current shell lineage: **v49 engineering playtest shell**
- Pure-domain baseline: **368/368 fixtures PASS**
- Java 25 / Paper 26.2 compile, fixture tests, shaded-JAR, and **two consecutive live boots + clean shutdowns PASS in GitHub Actions**

The implementation deliberately separates:

1. **direct/recovered gameplay evidence**,
2. **historical/community references**, and
3. **explicit engineering fallbacks** for values that have not been recovered with enough confidence.

Unknown original values are not silently invented.

## What is implemented

The codebase already contains substantial runtime work, including:

- arena-local runtime/indexing and deterministic tick scheduling;
- map/schematic parsing and placement geometry;
- economy, Goldmine, troop sending, progression and rollback;
- route movement, Castle attacks and Castle Guards;
- 11 tower families with upgrade paths and runtime combat abstractions;
- tower placement, management, quick upgrade, sell and rangefinder state;
- Summoner, Bazaar, progression and settings menu projections;
- player snapshot/recovery journal;
- Normal match timing and Armageddon runtime scaffolding;
- Paper-side adapters for entities, LOS, menus, map binding and Stage-4 live tests;
- live Castle Guard geometry anchors and an operable Engineering Playtest hotbar/loadout;
- an explicit Engineering action-bar HUD for castle HP, Coins, EXP, match clock and Armageddon state;
- Engineering match-safety guards that keep the temporary playtest inventory intact and suppress unrelated vanilla damage/hunger during controlled 1v1 testing;
- a live particle rangefinder projection for hovered, shift-nearest and permanently pinned tower ranges, while keeping particle style explicitly Engineering-only;
- status-aware live movement plus periodic Poison/Burn damage, with Poison cadence coming from stage data and unresolved Burn cadence remaining an explicit Engineering fallback;
- world-targeted AoE potion execution for Engineering Playtest: Bazaar purchase → armed state → right-click target area → deterministic arena-tick pulses, including damage, heal, Freeze/Speed status effects, cooldowns, visual feedback, and idempotent final death economy settlement;
- persistent player-custom hotbar layouts: Settings → Engineering hotbar editor → live reprojection, with layouts saved in `player-hotbars.yml` and reloaded on later matches/restarts;
- a live player-state snapshot round-trip gate (`/ctdsnapshotcheck`) that verifies capture → match preparation → restore across inventory, armor, offhand, cursor, location, game mode, XP, health/food, potion effects, velocity, flight and related state before recording Stage-4 evidence;
- a per-arena low-overhead tick profiler (`/ctdperf`) covering full Paper live ticks plus every core phase, with last/average/max timings, >=50 ms slow-tick counts and current tower/mob/guard/entity counts;
- a same-world arena reservation guard that rejects shared players and overlapping spatial envelopes before any match snapshot/state mutation, while permitting non-overlapping arenas in the same world or identical coordinates in different worlds. The 16-block safety padding is explicitly Engineering-only;
- a cross-restart recovery probe path: `/ctdsnapshotcheck restart-arm` durably captures the real player before mutation, a real server restart/rejoin triggers restore + recapture comparison, and the journal is deleted only after lossless verification. `/ctdsnapshotcheck restart-status` reports the durable evidence;
- route-facing live mob orientation: AI-disabled living entities keep the deterministic core position but their Paper teleport yaw now follows the actual horizontal route delta, removing sideways/backwards sliding through turns without changing speed, routing or combat geometry;
- tracked-mob vanilla side-effect shielding: live TD mobs are non-collidable, cannot pick up items, ignore vanilla combustion visuals, and active-match arrows are cleaned after impact so deterministic core movement/combat is not visually polluted by ordinary survival mechanics.

## Build

Paper 26.2 requires Java 25. The project is configured for:

```text
Java toolchain: 25
Kotlin JVM target: 25
Paper API: io.papermc.paper:paper-api:26.2.build.+
```

Because this source snapshot does not currently include a complete Gradle Wrapper distribution, CI installs a pinned Gradle version explicitly.

### GitHub Actions

Every push to `main`, pull request, or manual workflow run executes:

```text
Java 25
Gradle 9.7.0
clean
DomainFixtureSuite via JUnit
shadowJar
artifact upload
```

The generated plugin JAR appears in the workflow run under **Artifacts**.

### Local build

With Java 25 and Gradle 9.1+ installed:

```bash
gradle clean test shadowJar
```

Output:

```text
build/libs/
```

## Engineering playtest quick start

Unknown original values remain unknown in the strict configuration. For actual testing, operators can explicitly opt into a separate engineering-only profile:

1. Put the verified `ImprovedFarm.schem` in `plugins/CubeCraftTowerDefence/maps/ImprovedFarm.schem`. Expected SHA-256: `28d24136afe80358b89556d0fbe3b08d00e518af38c7c518819fa7fc225e613e`.
2. Join as an operator and stand at the intended minimum X/Z map corner in a large clear area. Setup places the schematic origin 8 blocks above your feet.
3. Run `/ctdplaytestsetup apply`.
4. Restart the server. This is intentional so every live service receives one immutable configuration snapshot.
5. As an online OP who is not inside a TD arena, run `/ctdsnapshotcheck` once to certify the live capture/restore adapter on the actual server.
6. Run `/ctdmapcheck`, then `/ctdpastefarm 28d24136afe8`. Safe paste aborts before mutation if any destination block is non-air.
7. Run `/ctdpreflight`. All non-live blockers should be gone.
8. With two online players, run `/ctdlivetest start test <redPlayer> <bluePlayer> wither`.
9. End the test with `/ctdlivetest stop test`.

The setup command creates `config.before-engineering-playtest.yml` before its first overwrite. Generated numbers, route choice, player spawns, and Guard anchors are explicitly tagged **ENGINEERING** and are never promoted to original CubeCraft truth.

For cross-restart recovery certification, run `/ctdsnapshotcheck restart-arm` as an online OP who is not in a TD arena, then perform a real server restart and reconnect. The command intentionally puts that player into temporary match-prepared state after the durable snapshot is written. On the next process, the plugin restores and re-captures the player before deleting the journal; verify the result with `/ctdsnapshotcheck restart-status`.

The Farm schematic is not bundled in this public repository because its redistribution rights have not been verified. The community author publicly shared the adjusted Farm schematic in the CubeCraft forum thread [Farm Improvements](https://www.cubecraft.net/threads/%F0%9F%8C%BE-farm-improvements-%E2%9A%92%EF%B8%8F.309871/); obtain it from the original post rather than redistributing it through this repository.

## Paper test commands

The plugin currently exposes engineering/admin commands such as:

- `/ctdstatus`
- `/ctdfixtures`
- `/ctdmapcheck`
- `/ctdready`
- `/ctdlivegate`
- `/ctdsnapshotcheck [restart-arm|restart-status]`
- `/ctdperf [arenaId|reset [arenaId]]`
- `/ctdpreflight`
- `/ctdfallbacks`
- `/ctdarmageddonfallbacks`
- `/ctdlivetest`
- `/ctdmenu`
- `/ctdplaytestsetup apply`

These are development/test interfaces and may change before a stable release.

## Maps and assets

The current Farm pipeline expects an external `ImprovedFarm.schem` engineering fixture for validation. It is intentionally not redistributed here unless its redistribution rights are separately verified.

Production-quality tower bodies, particles, projectiles, sounds, and several Mature-era visual details remain active development work.

## Fidelity policy

The project does **not** treat guessed numbers as original CubeCraft truth. Unresolved values stay behind explicit configuration/TruthGate boundaries until they are supported by sufficient evidence or deliberately selected as engineering fallbacks.

See the source under `dev.cubecrafttd.truth` for the runtime fallback model.

## License

Source code in this repository is licensed under **GPL-3.0-only**. See [`LICENSE`](LICENSE).

Third-party names, trademarks, and assets are not granted by this code license. See [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
