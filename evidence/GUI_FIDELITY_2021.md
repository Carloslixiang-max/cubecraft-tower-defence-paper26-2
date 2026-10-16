# Official 2021 GUI evidence and implementation limits

Authority: [CubeCraft's 26 January 2021 update](https://www.cubecraft.net/threads/epic-tower-defence-update-%F0%9F%8F%B0.274681/).
All slots below are zero-based, counted from the top-left inventory cell.
These sources were visually inspected; inferred runtime states are labeled separately.

| Source | Direct observation | Implementation limit |
| --- | --- | --- |
| [Screenshot_173](https://www.cubecraft.net/attachments/screenshot_173-png.184956/) | Settings has five rows. Auto-centre hover is on the second row, third column (11), with a Torch icon. Enabled title is green; description is gray; click instruction is gold. Bottom-centre Book is 40. | Locked and disabled wording are runtime adaptations. `ENABLED` alone is bold; the disabled-state typography is still an adaptation. Other partially occluded controls, the green/red panes and their actions are not inferred from this hover. |
| [Screenshot_169](https://www.cubecraft.net/attachments/screenshot_169-png.184952/) | Inventory-layout editor has four rows; bottom row is the hotbar. AoE icons have the angled Splash Potion silhouette. Bazaar example is Bricks, distinct from Stone Bricks. | Individual potion color/identity and source ordering remain unknown. Example hotbar positions are not universal defaults. Upper-right Arrow's click semantics remain unknown. |
| [Screenshot_171](https://www.cubecraft.net/attachments/screenshot_171-png.184954/) | Zeus path selector has three rows. Path 2 at 15 uses Sugar; its hover has green name, a leading blank lore line, gray `Summons baby Zeus` with only `baby Zeus` bold, another blank line and red click instruction. There is no terminal period. | This lore is only applied to Zeus path 2. Item names/lore for other towers require their own evidence. |
| [Screenshot_172](https://www.cubecraft.net/attachments/screenshot_172-png.184955/) | Zeus management has six rows: Stick 45, Book 49, Barrier 53. Stick hover has yellow base text and only `Enable` green and bold. | Book/Barrier clicks stay `noop:`. Pinned/Disable text remains a runtime adaptation. Statistics/upgrade/sell locations remain Engineering fallback. |

## Verification boundary

- `HistoricalSettingsControls` shares presentation between queue-local preferences and match state, while preserving their separate action namespaces.
- Fixtures exercise the 19/20-win boundary, disabled state, both live projections, editor/loadout identities and per-tower style isolation.
- Paper's adapter smoke constructs the recovered item materials, checks Archer's enchant-glint override, and serializes/deserializes the recovered partial-bold name/lore on both CI starts.
- Partial-bold spans are projected with the queue and match menus, validated against text bounds, and rendered as Adventure child components with explicit non-italic/plain parent styling.
- These checks prove projection and item construction, not client visual accuracy. Production GUI fidelity remains a Stage-4 human-server gate.
- Fast Fly speed, movement semantics, click sounds, pane actions, other path lore and exact per-potion colors remain unresolved.

The eight supplied `IMG_6736`/`IMG_674x`/`IMG_060x`/`IMG_061x` frames show Hypixel TowerWars, including its server address and different menus. They are excluded from CubeCraft evidence; no TowerWars values were imported.

## 2026-10-09 direct typography recount

Reopened the two original attachment files at native resolution, and enlarged Screenshot_171 using nearest-neighbor inspection. This corrects the previously transcribed Zeus description punctuation and omitted first blank line. Only observed `ENABLED` and `baby Zeus` spans are bold; other towers and the disabled label receive no invented bold spans. No additional icon, pane action or click sound is inferred from this typography pass.

Screenshot_172 was also reopened: the rangefinder label is yellow (not gold), with only `Enable` green and bold. The same span renderer preserves that accent through Paper item-byte roundtrip. Visible colored panes remain unresolved; their positions conflict with current fallback upgrade controls, so this pass does not replace gameplay actions with guessed pane actions.

## v110 visible controls and empty cells

- Reopened Screenshot_173 at native resolution: top-row Blaze Powder silhouette at 4; Feather at 10; red/green pane silhouettes at 19/20. Their exact names, click actions and pane states are hidden or not hovered. Icons are restored as contextual visual evidence and all four remain `noop:`. The bright-green pane uses LIME_STAINED_GLASS_PANE as a contextual material interpretation, not proof of an original item ID. Existing particle-density runtime control moves from conflicting slot 10 to Engineering slot 16. Both queue and match use the same visual controls. No Fast Fly behavior is inferred from the Feather.
- Reopened [Screenshot_169](https://www.cubecraft.net/attachments/screenshot_169-png.184952/), SHA-256 `6f84b1a518054cf494a3265f17884a94f4d53c2094b7a65b74af1719640c25da`. Arrow is at row 1, column 9 (8); restored as `noop:` on both editors. Its original click semantics remain unknown.
- Empty example hotbar cells are visually empty. Renderer now uses AIR instead of gray panes for `empty-slot`. Actions remain keyed by raw inventory position, so an empty cell can still receive a selected AoE through the editor; no actual item drag is used.
- Existing Ice/Mage/Sorcerer builder icon interpretations are downgraded from MATURE_DIRECT to MATURE_CONTEXT, matching their documented inference. No new builder slot or item identity is invented.
- Tracked menus now cancel bottom-inventory transfers, number-key/offhand swaps, collection, outside drops and drags as well as top clicks. Previously active matches had an additional safety listener, but waiting-lobby menus did not share its protection. Real Paper event/inventory smoke checks exercise the production guard with synthetic viewers, including raw-position actions on visually empty cells. This is API dispatch evidence, not human/client packet certification.


## v111 typography inheritance and production projection

[Paper's Adventure FAQ](https://docs.papermc.io/adventure/faq/) explains that item lore inherits an italic parent style unless explicitly disabled. The previous menu renderer disabled italics only when a recovered `MenuTextStyle` existed; unstyled controls could therefore still become italic. All projected names and lore now start with explicit non-italic/non-bold styling. Unknown colors remain unset; recovered partial-bold spans retain their own decorations. This is a renderer compatibility correction, not new evidence for any unknown label or color.

`BukkitMenuItemProjection` is now used by both real inventory population/redraw and the dual-boot Paper smoke. Real item byte roundtrips cover builder, Archer/Zeus selector, tower management, waiting Settings and inventory editor, plus unstyled/fallback/blank-lore cases. Arrow glint stays independent of text bold; AIR cells bypass item metadata and retain position-based actions. The test uses real Paper items without a human client and cannot certify on-screen appearance.

Screenshot_172 was reopened again at native resolution. Its colored panes may represent level/path availability; state-dependent semantics remain unresolved, so they are not copied across every live tower/level. No new pane actions or path-close placement behavior is inferred.


## v112 menu access and interaction routing

This is a Paper adapter correction, not newly recovered historical gesture evidence. [Paper 26.2 PlayerInteractEvent](https://jd.papermc.io/paper/26.2/org/bukkit/event/player/PlayerInteractEvent.html) documents per-hand delivery and vanilla no-op air events arriving as cancelled. Its API source initializes a null-block event to block DENY/item DEFAULT; the deprecated combined cancellation test is based on the block result.

The hotbar listener previously ignored all cancelled events, preventing native right-click-air input from reaching Summoner/Bazaar/Settings. It now receives those events and applies a shared main-hand/right-click policy. The four match interaction listeners reject offhand, left and physical input before looking up/arming any AoE or changing a tower. Explicit item DENY, and block DENY for a world click, remain rejected; no event is uncancelled. Main-hand actions continue through existing active-session, held-slot, economy and placement checks.

The dual-boot smoke uses real Paper event constructors and `RegisteredListener.callEvent`, with synthetic Player/Block actors. It verifies air dispatch despite block-DENY, exactly one accepted input across a main/offhand pair, world-only filtering, explicit cancellation/denial rejection, and preservation of the incoming result flags. It also checks the production hotbar/AoE listener cancellation annotations. Human-client packet/interaction certification remains Stage-4.
