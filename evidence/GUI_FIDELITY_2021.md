# Official 2021 GUI evidence and implementation limits

Authority: [CubeCraft's 26 January 2021 update](https://www.cubecraft.net/threads/epic-tower-defence-update-%F0%9F%8F%B0.274681/).
All slots below are zero-based, counted from the top-left inventory cell.
These sources were visually inspected; inferred runtime states are labeled separately.

| Source | Direct observation | Implementation limit |
| --- | --- | --- |
| [Screenshot_173](https://www.cubecraft.net/attachments/screenshot_173-png.184956/) | Settings has five rows. Auto-centre hover is on the second row, third column (11), with a Torch icon. Enabled title is green; description is gray; click instruction is gold. Bottom-centre Book is 40. | Locked and disabled wording are runtime adaptations. Exact bold spans are not reproduced. Other partially occluded controls, the green/red panes and their actions are not inferred from this hover. |
| [Screenshot_169](https://www.cubecraft.net/attachments/screenshot_169-png.184952/) | Inventory-layout editor has four rows; bottom row is the hotbar. AoE icons have the angled Splash Potion silhouette. Bazaar example is Bricks, distinct from Stone Bricks. | Individual potion color/identity and source ordering remain unknown. Example hotbar positions are not universal defaults. Upper-right Arrow's click semantics remain unknown. |
| [Screenshot_171](https://www.cubecraft.net/attachments/screenshot_171-png.184954/) | Zeus path selector has three rows. Path 2 at 15 uses Sugar; its hover has green name, gray description, blank line and red click instruction. | This lore is only applied to Zeus path 2. Item names/lore for other towers require their own evidence. |
| [Screenshot_172](https://www.cubecraft.net/attachments/screenshot_172-png.184955/) | Zeus management has six rows: Stick 45, Book 49, Barrier 53. | Book/Barrier clicks stay `noop:`. Statistics/upgrade/sell locations remain Engineering fallback. |

## Verification boundary

- `HistoricalSettingsControls` shares presentation between queue-local preferences and match state, while preserving their separate action namespaces.
- Fixtures exercise the 19/20-win boundary, disabled state, both live projections, editor/loadout identities and per-tower style isolation.
- Paper's adapter smoke constructs the recovered item materials and checks Archer's enchant-glint override on both CI starts.
- These checks prove projection and item construction, not client visual accuracy. Production GUI fidelity remains a Stage-4 human-server gate.
- Fast Fly speed, movement semantics, click sounds, pane actions, other path lore and exact per-potion colors remain unresolved.

The eight supplied `IMG_6736`/`IMG_674x`/`IMG_060x`/`IMG_061x` frames show Hypixel TowerWars, including its server address and different menus. They are excluded from CubeCraft evidence; no TowerWars values were imported.
