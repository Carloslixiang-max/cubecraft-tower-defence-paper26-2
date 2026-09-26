# CubeCraft Tower Defence — Paper 26.2 recreation

An **unofficial, open-source reimplementation** of the classic CubeCraft Tower Defence gameplay loop for modern **Paper 26.2 / Java 25** servers.

> This project is not affiliated with, endorsed by, or sponsored by CubeCraft Games. CubeCraft names, branding, maps, textures, and other original assets belong to their respective owners. This repository does **not** bundle original CubeCraft proprietary assets.

## Current status

This repository is an active high-fidelity recreation, not a finished drop-in clone yet.

- Paper target: **26.2**
- Java target: **25**
- Kotlin/JVM plugin
- Current shell lineage: **v67 engineering playtest shell**
- Pure-domain baseline: **442/442 fixtures PASS**
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
- tracked-mob vanilla side-effect shielding: live TD mobs are non-collidable, cannot pick up items, ignore vanilla combustion visuals, and active-match arrows are cleaned after impact so deterministic core movement/combat is not visually polluted by ordinary survival mechanics;
- an Engineering Armageddon player-vote bridge for the concrete Wither / Lightning / Horde outcomes. A unique highest vote overrides the operator-selected default, while no-vote and tie states fall back to that default. This resolution policy is explicitly Engineering-only: recovered sources confirm the original menu also offered Random, but the original winner/tie/Random-resolution semantics are not claimed until stronger evidence is recovered;
- historical-style player departure handling: leaving does not automatically end the match, the departed player leaves the active live session and their pre-match recovery journal remains authoritative, while same-team active players may manage towers owned by the departed player. A released player UUID is removed from the arena's player reservation, and a completely empty live arena is closed only as an Engineering resource cleanup rather than a gameplay win;
- Engineering match-end presentation and UI teardown: live TD inventories are closed and the action-bar HUD is cleared before snapshot restore; after restoration, still-active participants receive explicit VICTORY / DEFEAT / DRAW / MATCH ENDED title + chat feedback. Exact original CubeCraft end-screen wording, styling and timing remain unresolved and are not claimed by this Engineering layer;
- a regular-player Engineering 1v1 FIFO queue: `/ctdjoin` waits for a second eligible online player and automatically starts the single configured Farm arena when it is free; extra players remain queued while the map is busy. Once paired, players receive the historically recovered Tower Defence 3 → 2 → 1 chat countdown at exact one-second intervals before arena creation. Leaving, disconnecting, or becoming recovery-ineligible during that countdown cancels the pair and returns the remaining eligible player to the front of the queue. `/ctdleave` also exits an active match and immediately restores the online player's pre-match state. FIFO order and first-player RED / second-player BLUE assignment remain explicit Engineering behavior, not recovered original matchmaking truth;
- pregame Armageddon voting for queued players through `/ctdvote armageddon <random|wither|lightning|horde>`. Historical evidence confirms these four options and that no votes resolve through Random; the concrete Random result is chosen from currently runnable modes immediately before arena creation. A unique highest concrete vote is honored, while an unresolved exact original tie rule remains an explicitly labelled Engineering Random fallback. Queue-started matches lock Armageddon after this pregame resolution; admin-started live tests retain the older engineering in-match vote path for debugging;
- pregame Pricing voting through `/ctdvote pricing <normal|double|quick>`. No-vote resolves to Normal as recovered from the historical client log. Double Income now flows through the real live runtime—double Goldmine income, mob-kill Coins and sent-mob EXP—while Quick Start uses the recovered Mature-era 1500 Coins / 100 EXP starting balance and otherwise Normal income. The exact original tie rule remains unresolved, so a tied highest vote uses an explicit Engineering Normal fallback rather than pretending the rule is known;
- a safe pregame voting GUI: queued/countdown players can now run `/ctdvote` with no arguments and click Armageddon/Pricing choices directly. The menu refreshes immediately and marks the player's current choices. It hides concrete Armageddon choices whose fallback composition is not runnable, while Random remains available over the runnable set. Historical evidence confirms an End Crystal inventory entry point in the original game, but the exact internal slot/icon layout is not recovered strongly enough, so this current GUI layout is explicitly Engineering-only and does not mutate the player's pre-queue inventory;
- a non-invasive Engineering queue HUD in the action bar: waiting players see their current FIFO position plus their Armageddon/Pricing choices (or the historically recovered no-vote defaults), while matched players see the 3 → 2 → 1 start countdown and the same vote state. The HUD is cleared on leave, removal, shutdown handoff, or live arena start and never occupies an inventory slot;
- stronger round teardown verification for repeated Farm reuse: after tower bodies are restored and all tracked mobs/guards/displays/projectiles receive their removal calls, teardown performs a second independent Paper-world presence scan over the original tracked UUID set. A tracked entity that still exists is reported separately as world residue and forces the teardown report out of `fullyCleanNow`, preventing a successful remove return from being treated as proof of a clean round;
- a persistent Farm reuse interlock backed by `farm-reuse-gate.yml`. New admin-started or queued matches are refused after tracked entity residue or tower-body restore conflicts. Tracked entity UUIDs are rechecked against the live server and automatically disappear from the gate once the entities are truly gone. Tower-body conflicts are hard residue and survive restarts;
- a crash-conscious verified Farm repair/reset path through `/ctdresetfarm <verified-sha-prefix>`. It is available only while the reuse gate is blocked, with no active TD arena/map operation and no still-live tracked entity residue. Before scheduling it persists a reset-in-progress maintenance lock; the reset scans the full verified schematic volume without mutation, snapshots only differing blocks, applies differences in bounded batches, then verifies the complete volume. APPLY/VERIFY failures attempt to roll back captured BlockState snapshots and keep the persistent gate locked. Only a fully verified completion clears tower-body residue and the maintenance lock;
- v62 hardens player-state transactions for repeated real-server rounds: a new match capture may replace only a fully RESTORED prior snapshot tombstone, so a player can safely enter a later round without weakening protection against unresolved CAPTURED/RESTORING snapshots. If `prepareForMatch` fails after a snapshot has been captured (and, for live play, durably journaled), recovery is attempted immediately. A successful rollback removes the durable journal entry; a failed rollback leaves the snapshot pending for reconnect/recovery instead of silently stranding partially-mutated player state;
- v63 makes restart recovery fail closed without making one damaged snapshot crash the entire plugin. Valid recovery files are still loaded, unreadable `.snapshot` files are preserved byte-for-byte and reported, and startup latches a `RECOVERY_JOURNAL_CORRUPT` readiness blocker. Normal queue countdown/start and admin live-start paths therefore cannot begin another TD match until the damaged recovery file is repaired/restored and the server is restarted;
- v64 closes the world-side crash-restart gap. Every live TD mob, Guard anchor and Engineering tower summon is marked with a persistent entity scoreboard tag. If the previous plugin process did not shut down cleanly, startup persists an unclean-restart Farm reuse hard gate instead of trusting an absent teardown report. The verified Farm reset now loads/scans the whole schematic volume, removes only persistently-tagged TD entities from that volume, applies block differences, fully verifies the map, verifies that no tagged TD entity survived, and only then clears the crash/reuse gate;
- v65 separates the 1v1 queue's pre-start transaction from post-start presentation. Vote resolution and controller start failures still restore the matched pair to the front of the queue with a short retry cooldown. Once `startOneVsOneResolvedTest` returns successfully, the arena is committed: votes are cleared and later chat/UI presentation failures are logged only, never requeueing players who are already inside a live match;
- v66 makes partial arena-start failure teardown authoritative for Farm reuse. A failed start now records the teardown report into the same persistent residue gate used by normal match end, including surviving tracked entities and tower-body conflicts. If teardown itself fails, or if its residue report cannot be persisted, the Farm is conservatively marked with unknown world integrity and remains hard-blocked until the verified Farm reset proves the world clean;
- v67 adds a formal Engineering real-server 60+ tower performance evidence gate on top of `/ctdperf`. `/ctdperf gate [arenaId]` requires at least 60 live towers and a sustained 1,200 profiled-tick window, then checks the TD live-tick profiler against explicit Engineering acceptance thresholds (average <= 10 ms, maximum < 50 ms, zero TD ticks >= 50 ms). PASS/FAIL is durably recorded in Stage-4 evidence; NOT READY does not mutate evidence. These thresholds are engineering acceptance criteria, not recovered CubeCraft gameplay truth, and the real-server gate remains uncertified until an actual live arena satisfies it.

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
9. During the match, each player can open Settings → **Armageddon vote (Engineering)** and vote for any currently runnable concrete mode. The operator's start argument remains the explicit Engineering fallback for no-vote/tie states.
10. Disconnecting a player no longer leaves a half-active session: the old arena continues for remaining participants, the departed player's snapshot restores on reconnect, and their towers become manageable by active teammates. If everybody leaves, the empty arena is cleaned up automatically on the next tick.
11. A natural win/loss or manual stop closes TD menus before restoration and then shows an Engineering result title/chat after the player's pre-match state is back.
12. For ordinary player-facing testing after the same setup/preflight, players can use `/ctdjoin` instead of an OP manually starting each round. While waiting or during the 3 → 2 → 1 countdown, run `/ctdvote` to open the safe clickable vote GUI, or use the direct `/ctdvote armageddon ...` / `/ctdvote pricing ...` forms. With no votes, Armageddon resolves through Random and Pricing resolves to Normal as in the recovered historical log. Both selections are locked into the queue arena when it starts. Later players wait until the single configured Farm arena is free. Use `/ctdleave` to leave the queue, cancel your pending countdown slot, or exit an active match.
13. End an admin-started test with `/ctdlivetest stop test`.

The setup command creates `config.before-engineering-playtest.yml` before its first overwrite. Generated numbers, route choice, player spawns, and Guard anchors are explicitly tagged **ENGINEERING** and are never promoted to original CubeCraft truth.

For cross-restart recovery certification, run `/ctdsnapshotcheck restart-arm` as an online OP who is not in a TD arena, then perform a real server restart and reconnect. The command intentionally puts that player into temporary match-prepared state after the durable snapshot is written. On the next process, the plugin restores and re-captures the player before deleting the journal; verify the result with `/ctdsnapshotcheck restart-status`.

The Farm schematic is not bundled in this public repository because its redistribution rights have not been verified. The community author publicly shared the adjusted Farm schematic in the CubeCraft forum thread [Farm Improvements](https://www.cubecraft.net/threads/%F0%9F%8C%BE-farm-improvements-%E2%9A%92%EF%B8%8F.309871/); obtain it from the original post rather than redistributing it through this repository.

## Paper test commands

The plugin currently exposes player-facing Engineering commands:

- `/ctdjoin`
- `/ctdleave`
- `/ctdvote` (open safe pregame voting GUI)
- `/ctdvote armageddon <random|wither|lightning|horde>`
- `/ctdvote pricing <normal|double|quick>`

Administrative/test commands include:

- `/ctdstatus`
- `/ctdfixtures`
- `/ctdmapcheck`
- `/ctdready`
- `/ctdlivegate`
- `/ctdreuse status`
- `/ctdresetfarm <verified-sha-prefix>`
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
