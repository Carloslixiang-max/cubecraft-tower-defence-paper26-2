# CubeCraft Tower Defence — Paper 26.2 recreation

An **unofficial, open-source reimplementation** of the classic CubeCraft Tower Defence gameplay loop for modern **Paper 26.2 / Java 25** servers.

> This project is not affiliated with, endorsed by, or sponsored by CubeCraft Games. CubeCraft names, branding, maps, textures, and other original assets belong to their respective owners. This repository does **not** bundle original CubeCraft proprietary assets.

## Current status

This repository is an active high-fidelity recreation, not a finished drop-in clone yet.

- Paper target: **26.2**
- Java target: **25**
- Kotlin/JVM plugin
- Current shell lineage: **v105 engineering playtest shell**
- Pure-domain baseline: **524/524 fixtures PASS**
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
- v67 adds a formal Engineering real-server 60+ tower performance evidence gate on top of `/ctdperf`. `/ctdperf gate [arenaId]` requires at least 60 live towers and a sustained 1,200 profiled-tick window, then checks the TD live-tick profiler against explicit Engineering acceptance thresholds (average <= 10 ms, maximum < 50 ms, zero TD ticks >= 50 ms). PASS/FAIL is durably recorded in Stage-4 evidence; NOT READY does not mutate evidence. These thresholds are engineering acceptance criteria, not recovered CubeCraft gameplay truth, and the real-server gate remains uncertified until an actual live arena satisfies it;
- v68 hardens durable recovery commit ordering. After a player snapshot has been restored and verified, the on-disk recovery journal must now be deleted successfully before the in-memory snapshot record may transition to RESTORED. If journal deletion fails, the authoritative snapshot is returned to pending CAPTURED state and can be retried safely; a stale snapshot can no longer survive on disk while memory incorrectly treats recovery as complete;
- v69 adds bounded automatic recovery retry for online pending players. The recovery listener sweeps once per second, de-duplicates scheduled restores, and allows up to three online attempts before stopping automatic retries for that connection; reconnecting resets the budget. This covers transient Paper/IO restore failures after `/ctdleave`, match end, or restart recovery without requiring an immediate manual relog, while the durable journal remains authoritative whenever a retry still fails;
- v70 separates automatically-recorded Stage-4 core evidence from full real-server certification. The existing durable core gate is now reported as `coreCertified`; `/ctdlivegate` also computes `fullCertified` and lists every still-unverified live gate. Snapshot round-trip, restart recovery, and 60+ tower evidence disappear from the remaining list only after their recorded gates pass, while GUI fidelity, queue/vote/countdown, departure/reconnect/tower takeover, verified Farm reset, consecutive reuse, movement/raytrace, and multi-arena certification remain visibly pending until dedicated real-server evidence exists;
- v71 turns repeated-round reuse into consecutive evidence instead of an accumulated counter. Only normally completed arena teardowns participate in certification; a partial start that later cleans up no longer counts as a round. Fully clean normal teardowns increment both total and consecutive clean-round counters, any dirty teardown resets the consecutive counter, and an unclean restart resets it as well. Stage-4 core certification and the dynamic consecutive-reuse live gate now require two consecutive fully clean rounds;
- v72 makes verified Farm reset a first-class real-server evidence gate and hardens reset completion semantics. Starting `/ctdresetfarm` invalidates any previous reset PASS; only a complete tagged-entity cleanup + block APPLY + full VERIFY + durable reuse-gate clear records PASS. Reset failure records/retains FAIL. Once the world has reached COMPLETE verification, a later control-plane callback failure no longer rolls verified blocks back to their pre-reset dirty snapshots. Reuse-gate clearing itself is persistence-first, so a failed durable clear cannot leave memory falsely reporting the Farm as reusable;
- v73 makes the regular-player queue path a first-class observed live gate. Stage-4 records six independent real interactions: a successful `/ctdjoin`, a live queue HUD projection, an Armageddon vote clicked through the pregame GUI action bridge, a Pricing vote clicked through that GUI bridge, a completed historical 3 -> 2 -> 1 countdown that successfully commits an arena start, and an active-match `/ctdleave`. Waiting-queue or countdown cancellation does not satisfy the active-leave evidence. Only after all six observations are durable does `/ctdlivegate` remove the queue/join/leave + GUI/HUD/vote/countdown certification item;
- v74 makes disconnect/reconnect and departed-owner tower takeover observable live evidence. A real active-match quit/kick durably marks that exact UUID as awaiting reconnect recovery; only a later successful pending-snapshot restore for the same UUID records reconnect PASS. Separately, Stage-4 records takeover only when an active teammate successfully upgrades or sells a tower whose owner is already in the arena-local departed set. Generic restores, voluntary queue leaves, owner self-management, stats/rangefinder actions, and failed/unauthorized tower actions do not satisfy the gate;
- v75 adds an admin-only Engineering team certification harness without changing regular 1v1 matchmaking. `/ctdlivetest teamstart <arenaId> <red1[,red2]> <blue1[,blue2]> <wither|lightning|horde>` starts a bounded 1-2 player-per-team live arena using the same bootstrap, economy, tower lifecycle, recovery, isolation and teardown stack as 1v1. This makes the v74 teammate-takeover gate actually testable: e.g. start 2v1, have RED1 place a tower, disconnect RED1, have RED2 upgrade/sell that departed-owner tower, then reconnect RED1 and wait for durable snapshot recovery. The command is certification-only and does not alter `/ctdjoin` FIFO 1v1 behavior;
- v76 adds pure-domain 2v2 bootstrap regression coverage beneath that live harness. The fixture proves all four players are captured and prepared, the two RED and two BLUE memberships are preserved in ArenaContext and MatchSessionState, one RED player can depart without removing the active teammate/opponents, and all four authoritative pre-match snapshots remain restorable. This keeps the certification-only team launcher backed by tested generic session/recovery behavior rather than only a Bukkit command path;
- v77 makes `/ctdlivegate` directly actionable during real playtests. In addition to core/full certification and the remaining gate list, it now prints all six queue observations individually, the reconnect-restore and teammate-takeover observations separately, and the durable snapshot/restart/60+ tower/Farm-reset/consecutive-round/clean-restart evidence counters. This does not auto-certify visual fidelity; it only exposes which objective live observations are already durable and which still need a real server run;
- v78 starts the post-stability fidelity pass by wiring evidence-backed `MenuSlot.iconHint` data all the way through `LiveMenuView` into the Bukkit renderer. The recovered 2021 builder projection can now render its known/inferred item identities instead of collapsing all tower entries to generic stone, while direct hints such as Archer/Bow, Artillery/TNT, Zeus/Beacon, Quake/Dirt, Poison/Potion and Tower information/Book are preserved end-to-end. The historically documented tower-upgrade action now renders with an Anvil icon. Exact unrecovered slots and weaker visual inferences remain explicitly tagged and are not promoted to direct CubeCraft truth;
- v79 corrects a concrete historical Summoner GUI mismatch. Direct guide evidence places the mob-upgrade Nether Star in the bottom-middle slot and the send-queue Mob Spawner in the bottom-right slot. The old engineering menu had those controls reversed. The live menu now uses slot 22 for `nav:progression` with a Nether Star and slot 26 for `summoner:send` with a Mob Spawner, with a dedicated regression fixture locking the recovered layout;
- v80 adds exact cumulative-cost instrumentation for the three historically visible match-end contribution categories. Successful tower placement records its real `TowerPlacementReceipt.cost`; Summoner sends record `TroopBatchSendReceipt.totalCost`; final mob deaths record the killed troop's own `mobId + level -> sendCoins` value. Kill statistics are finalized once per logical mob through the shared death-finalization path, so tower, sword, bow and player-potion final blows can contribute without the old direct-hit-only blind spot or duplicate finalization. These three cumulative-cost buckets later become the authoritative basis for the v88 Top-3 score correction;
- v81 was an intermediate community-evidence interpretation of Overall score as Coins spent during the round. That interpretation came from later player discussion rather than an official rule statement and is explicitly superseded by v88; it is retained in the history here only so the evidence correction is traceable rather than silently rewritten;
- v82 restores the historical end-of-match Top players presentation path. `HistoricalMatchEndLeaderboardProjector` selects the three highest `overallScore` values and preserves the recovered hover-detail fields `Towers placed`, `Troops sent`, `Enemies killed` and `Overall score`. Bukkit renders the heading `Top players:` and hoverable player names after a normal result, while Engineering manual-stop outcomes intentionally skip the historical leaderboard. Equal-score ordering still uses an explicitly labelled deterministic UUID fallback until direct tie evidence is recovered;
- v83 restores the directly evidenced in-match `Castle Health` bossbar. `HistoricalCastleHealthHudProjector` maps the viewer's own castle health to a safe 0..1 bossbar ratio with the recovered title, while the Bukkit HUD adapter owns one green solid bar per active player. Departure immediately removes that player's bar and match teardown clears every remaining bar before snapshot restoration, so historical HUD fidelity does not leave UI residue behind;
- v84 restores the historically evidenced right-side match sidebar with `Tower Defence`, live Coins, live EXP and `play.cubecraft.net`. The Bukkit adapter is deliberately conservative: it only replaces Bukkit's main scoreboard, never a custom scoreboard already owned by another plugin. Players with custom scoreboards keep the Engineering action-bar fallback instead. TD-owned scoreboards are restored to main on normal clear, and an offline clear is completed on the next join so disconnects cannot strand the temporary sidebar;
- v85 removes the normal mixed HUD state. When the historical Tower Defence sidebar successfully owns the player's scoreboard, the old ENG action-bar HUD is suppressed and cleared; when another plugin already owns a custom scoreboard, TD still refuses to steal it and keeps the ENG action-bar as a compatibility fallback. This preserves interoperability without showing two competing match HUDs to ordinary players;
- v86 extends the authoritative player snapshot to include Bukkit `flySpeed` before any historical Fast Fly work is enabled. Recovery journal format advances to v3 and persists the exact speed, while legacy v1/v2 files remain readable with the Minecraft default `0.1f`. Capture/restore and the live round-trip comparator now include `flySpeed`, with fixtures proving current-format preservation, legacy compatibility and explicit drift detection. This keeps future Settings fidelity subordinate to lossless player-state recovery rather than changing movement state unsafely;
- v87 removes the last normal-match Engineering result overlay. Historical evidence supports the end-of-match Top players presentation, but the exact original victory/defeat/draw title wording and timing remain unrecovered. Normal Winner/Draw outcomes therefore no longer display `ENG VICTORY`, `ENG DEFEAT`, `ENG DRAW` or the corresponding Engineering chat line; they keep the recovered Top players UI only. Explicit `ENGINEERING_CUSTOM` stop/failure outcomes still show the clearly-labelled Engineering overlay so administrators retain useful diagnostics without exposing placeholder UI to ordinary matches;
- v88 corrects the Top-3 score evidence hierarchy. The official 2017 CubeCraft Tower Defence update states that the game-end Top 3 is based on towers built, mobs sent and mobs killed using cumulative cost. `HistoricalOverallScoreCalculator` therefore now sums the three already-recorded authoritative buckets `towersBuiltCumulativeCost + troopsSentCumulativeCost + troopsKilledCumulativeCost`. The later community claim that Overall score was generic Coins spent no longer drives runtime behavior. Exact equal-score tie ordering remains unresolved and keeps the explicit deterministic fallback;
- v89 restores the officially described progression affordability feedback without pretending the exact menu layout is recovered. The live progression menu now reads the player's authoritative `MATCH_EXP` balance and compares it with the actual next-level `unlockOrUpgradeExp`. Affordable upgrades render an orange stained-glass pane, while unaffordable or max-level entries use a plain glass pane. Max-level entries are emitted as `noop:` actions and swallowed by the Bukkit menu bridge, preventing the previous invalid L6 upgrade click from reaching the progression router. Slot positions remain explicitly Engineering fallback;
- v90 makes the progression rollback controls reflect the real 10-second rollback window instead of always exposing a clickable action. The controller asks `TroopProgressionService.mayRollback(player,mob,tick)` for each mob when projecting the menu. Eligible rows keep the real `progression:rollback:*` action; ineligible rows become `noop:` entries while retaining the same fallback visual identity, so expired or never-purchased upgrades can no longer throw a user-facing rollback exception merely because the menu still looked clickable;
- v91 restores the official 2017 Summoner send-cage glow behavior. The arena now owns an explicit `sendCooldowns` tracker and passes that exact instance into `MatchMenuActionRouter`, eliminating the previous hidden router-local tracker. `SummonerSendAvailabilityEvaluator` mirrors the real send preflight: the draft must be non-empty, the sender cooldown must be ready, the attacked-team queue must have enough remaining capacity, and the player's authoritative `MATCH_COINS` balance must cover the draft. Only when every condition passes does the bottom-right spawner project `spawner-glow`, rendered with an enchantment glint; otherwise it remains the normal spawner. This ties the historical visual cue to the same runtime state that actually permits sending;
- v92 keeps stateful dynamic menus visually current without close/reopen flicker. `BukkitMenuBridge` can now refresh an inventory it still owns in place, and the main match bridge enables a bounded 10-tick Engineering refresh cadence for stable-title menus: Summoner, troop upgrades, Bazaar, Settings and the hotbar editor. This means send cooldown expiry, queue/Coins changes, newly earned EXP and rollback-window expiry become visible while the GUI stays open. Inventory close immediately removes refresh ownership, volatile Armageddon/tower/pregame menus are excluded, and same-menu action refreshes also redraw in place before falling back to a normal reopen when the menu identity changes;
- v93 expands the Settings model toward the official 2017 surface without inventing the unresolved Fast Fly semantics. Player settings now include the recovered particle modes `500/sec`, `100/sec` and `minimum`, plus Digital mob health and Damage indicators toggles alongside the existing in-game purchase toggle. The Settings GUI routes those controls through real match-session state and refreshes in place through v92. The particle preference also drives the per-player Engineering tower-rangefinder sampling density; this is an adapter mapping for the current rangefinder, not a claim that the original global particle-cap implementation used the same sample counts.
- v94 completes the Paper projection for Digital mob health and Damage indicators. Digital health is rendered as viewer-scoped numeric TextDisplays above live tracked mobs only for players who enabled the setting. Damage indicators are derived from authoritative TD health deltas rather than vanilla entity HP, are viewer-scoped through the same per-player setting, and are bounded to at most one active indicator display per tracked mob so this fidelity layer does not create an unbounded transient-entity burst under heavy tower fire. Exact original CubeCraft text styling, vertical offset and indicator lifetime remain unrecovered, so those presentation details are explicitly Engineering fallback rather than claimed historical truth.
- v95 removes the remaining fake Settings control. The official 2017 changelog confirms that an In-game purchase preference existed, but it does not recover the exact in-match catalogue, profile-point persistence or transaction semantics. Until that backend is implemented from stronger evidence, the Settings entry is still visible for historical surface fidelity but is explicitly unavailable and projects a `noop:` action; forged legacy `settings:point-purchases` actions fail closed instead of toggling inert state. Auto-centre remains a real tower-placement input, so it is not treated as a fake control.
- v96 tightens Damage indicators to the recovered player-facing meaning instead of showing every HP loss to every player who enabled the option. Tower attacks (including Leach), player sword/bow hits, AoE potion damage and player-owned Poison/Burn ticks now emit authoritative `PlayerMobDamageEvent` records with the actual health lost after overkill clipping. The Paper layer keeps at most one active indicator per mob/player pair and shows that display only to the owning player. Castle Guard/system damage is excluded, and simultaneous teammate hits on the same mob no longer leak into each other's number. Exact original colour, offset and lifetime remain Engineering fallback.
- v97 corrects the tower-path placement UI from stronger official 2021 screenshot evidence. Normal placement keeps the separate 27-slot `Select an upgrade path` inventory, with the two path controls directly recovered at zero-based slots 11 and 15; the selected path is then committed and remembered for shift quick-placement, while no explicit selection still defaults to the top path as documented by CubeCraft. The same screenshot pass also corrects the visible Tower Builder hints for Mage to coal and Sorcerer to an Ender Eye. Exact per-tower path item identities, names and lore remain contextual where the screenshots do not fully recover them.
- v98 extends the 2021 tower UI fidelity pass. The live tower-management inventory now projects the real tower display name and Roman-numeral level, uses the recovered 54-slot shell, and places the rangefinder toggle at zero-based slot 45 with its stick icon and enabled/disabled wording while unrecovered stats/upgrade/sell positions remain explicitly weaker evidence. The upgrade-path selector is now contextual per tower, transports recovered item lore through the Paper menu bridge, and exposes the directly visible Zeus path-2 `Summons baby Zeus.` description. Towers with no TOP/BOTTOM branch no longer receive a fake two-path selector; they place through the documented default path behavior. Artillery level II is also corrected to a shared pre-branch stage, matching its cumulative-cost table and allowing both later branches to upgrade through it.
- v99 restores the official January-2021 `Change inventory layout` shell instead of the earlier 54-slot Engineering matrix. The live editor is now a 36-slot inventory with the bottom row mirroring the player's nine hotbar positions; a transient select-then-place workflow lets the player choose Sword, Bow, Mob Summoner, Castle Bazaar or Settings and then place that action into a bottom-row slot. Existing hotbar persistence remains authoritative across matches/restarts, and the old direct `hotbar:move:*` action stays compatibility-only rather than being projected by the normal UI. The 36-slot shell/title and bottom-row concept come from official 2021 evidence; unrecovered palette positions remain explicitly Engineering fallback. Multiple AoE hotbar entries are not yet fabricated from this shell and remain a separate inventory-model task.
- v100 separates AoE purchase from AoE use so the runtime can represent the officially evidenced ability to keep multiple AoEs available at once. Purchased AoEs now enter a match-local `AoEPotionInventory` with stackable quantities; Bazaar uses a `purchase` action and shows the owned count, while the legacy `use` action remains compatibility-only. Buying another potion is no longer blocked by an already armed potion or by the throw cooldown. A successful throw consumes exactly one owned unit, keeps the same potion armed while more of that type remain, and clears it only when the stack is exhausted. The first purchased potion still auto-arms as a temporary live-compatibility bridge until the hotbar item model is connected; this auto-arm behavior is Engineering compatibility, not claimed original CubeCraft behavior.
- v101 connects owned AoEs to the persistent 2021 hotbar model. `HotbarLayout` now carries optional `aoeSlots` beside the five legacy controls and persists them with backward-compatible `AOE__<potionId>` keys, so existing `player-hotbars.yml` files remain readable. The 36-slot editor exposes currently owned AoEs and only allows them into empty slots until exact original replacement/swap gestures are recovered. Live hotbar projection shows the owned stack count, omits a configured AoE when its stock is zero without forgetting the saved slot, and refreshes immediately after purchase/throw. Right-click AoE targeting is now gated by the actually held AoE slot rather than a global armed flag, preventing ordinary menu interactions from accidentally committing an AoE. Exact original potion appearance and editor AoE palette positions remain explicitly unresolved.
- v102 makes the five critical GUI surfaces observable in the durable Stage-4 real-server gate without pretending that observation alone proves visual fidelity. Tower Builder, path selector, tower menu, Settings and inventory-layout opens are classified by the Paper adapter and recorded independently; `/ctdlivegate` can now distinguish “this surface was exercised on a real server” from the still-separate production icon/lore fidelity requirement.
- v103 restores two stronger official-2021 GUI shells. Settings now projects the recovered 45-slot inventory instead of the old 27-slot Engineering shell and preserves the directly visible bottom-centre book at zero-based slot 40 while leaving its unrecovered click semantics fail-closed. `Change inventory layout` keeps the recovered 36-slot shell but no longer invents a separate fixed-action palette: the bottom row is the actual current hotbar and its existing entries can be selected for rearrangement, while owned AoEs are sourced from the upper three rows using an explicitly Engineering per-potion mapping until exact original source positions/gestures are recovered.
- v104 makes the officially evidenced pre-match Settings and `Change inventory layout` surfaces usable from the existing `/ctdvote` waiting-lobby hub. Queue-local `PregamePreferenceState` reuses the same Settings and Hotbar rules as live matches without touching the player's real waiting inventory; hotbar changes persist through the existing `player-hotbars.yml`, while Settings are copied into the match session inside the arena bootstrap transaction. Failed starts/requeues keep those preferences, successful starts and genuine queue departures clear the temporary state, and exact waiting-lobby entry slots remain Engineering fallback because the original CubeCraft positions are not recovered. Auto-centre disabling also remains fail-closed pregame because a persistent lifetime-win source has not yet been recovered.
- v105 resolves the lifetime-win gap behind the recovered Auto-centre rule. A minimal `player-progress.yml` stores only `<UUID>.wins`; authoritative `MatchOutcome.Winner(team)` results increment the winning active players exactly once, while ties, manual stops and start failures do not. Waiting-lobby Settings and match bootstrap both rehydrate `PlayerMatchSettings.lifetimeWins` from the same store, so the adopted 20-win Auto-centre-disable threshold now works across restarts. The threshold is centralized in `LifetimeWinProgress`; unrelated profile data such as losses, games played, points or ranks remain intentionally unimplemented rather than inferred.

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
