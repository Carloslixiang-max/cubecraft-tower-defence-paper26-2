# v122 — live forms and tower child ownership

## Evidence and limits

- The [official September 2017 balance update](https://www.cubecraft.net/threads/tower-defence-update.197385/)
  explicitly removes full armor from Zombies/Giants and keeps helmets only.
- The firsthand [2019 Tower Defence Guide/Info](https://www.cubecraft.net/threads/tower-defence-guide-info.216409/)
  describes Zombie levels I–V as no helmet, leather, chain, iron and diamond.
  The adapter uses this historical visual baseline; this is not a direct
  measurement of every 2021 client frame. Natural random gear/baby variation
  is suppressed for this troop. Helmet durability/drop behavior is adapter
  policy, not claimed original gameplay. Explicit zero-value item modifiers
  suppress helmet armor bonuses so cosmetic tiers cannot reduce the vanilla
  `finalDamage` forwarded into player weapon combat.
- The [official 2021 update](https://www.cubecraft.net/threads/epic-tower-defence-update-%F0%9F%8F%B0.274681/)
  changes Slimes from splitting to shrinking, losing 25% max HP per step.
  The core already implements that mechanic. Paper now projects the same
  logical troop's phase into its existing Slime/MagmaCube entity. Sizes
  **4 → 2 → 1** are an explicit visual approximation, not recovered original
  sizes, a new phase cap or a change to core health/speed/damage.
- Paper 26.2 Slime/MagmaCube share `AbstractCubeMob`; MagmaCube no longer
  extends Slime. `setSize` resets attributes and heals the entity (MagmaCube
  additionally resets armor).
  Projection preserves their base values and current vanilla health, changes
  size only when needed, and never writes core combat state.

Other old guide details are deliberately not imported wholesale: Creeper
mechanics changed in 2021; horse riders need composite ownership; Baby Zeus
and Sorcerer child exact bodies/animation remain unresolved. No Fast Fly
speed, unverified sound or invented extra combat is introduced.

## Bugs fixed

1. Zeus I/II and the chain branch incorrectly required `baby_zeus` and threw
   during ordinary arena ticks. They now have zero children; only stages
   carrying the existing ability tag get one.
2. Child UUIDs lived only in a private phase registry, so match-end teardown
   omitted them. A dedicated arena-local `towerSummons` index now participates
   in teardown, residue reporting, index emptiness and clearing.
3. Necromancer's Iron Golem → Shulker/Snowman transition retained the old child
   when its count stayed one. A changed kind now removes/replaces that child.
4. Failed child removal used to unregister the UUID anyway. It now retains
   both ownership records and fails into the existing conservative arena
   cleanup path; teardown can retry and report the real residue.
5. Visual children used the tower's base/range corner and vanilla gravity,
   burying or dropping them inside the reconstructed body. They now stage
   above the current model, follow upgrades on the same UUID, and disable
   vanilla gravity/collision/pickup/damage/AI. ArmorStand equipment is locked.
   Placement/spacing is engineering presentation, not recovered original
   child pathing. Unresolved child identities retain their explicit fallback.
6. TD-marked entity block-formation/change events are cancelled so Snowman
   children cannot leave snow outside the body ledger. Unmarked survival
   entities retain their event permissions; no host mobGriefing rule changes.

## Verification

Nine added domain fixtures exercise zero-child Zeus stages, stable projection,
kind replacement, failed-removal ownership/retry, and teardown success/failure.
The total domain suite is 599 fixtures.

Both opt-in owned-world Paper boots run production-port checks for five Zombie
helmets, Slime and MagmaCube shrink on the same UUID, attribute/health
preservation, dead-form exclusion, all six child kinds and model-height refresh.
Native block events check TD child cancellation and unmarked-entity isolation.
They additionally place actual bundled tower bodies, tick real children through
Necromancer/Zeus upgrades and Sorcerer sale, then verify match-end cleanup with
zero remaining child UUIDs. These checks are not run in the host's main world.

The full network workflow separately repeats two natural matches, Farm reuse,
restart recovery and the complete saved-scene comparison. Human native-client
visual matching, 60+ tower performance, and exact child models remain pending.
