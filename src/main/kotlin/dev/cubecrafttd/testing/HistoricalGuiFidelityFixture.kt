package dev.cubecrafttd.testing

import dev.cubecrafttd.match.PlayerMatchSessionState
import dev.cubecrafttd.paper.bukkit.toLiveView
import dev.cubecrafttd.progression.TroopProgressionState
import dev.cubecrafttd.ui.*
import java.util.UUID

object HistoricalGuiFidelityFixture {
    fun run(): List<FixtureResult> {
        val player = PlayerMatchSessionState(UUID.fromString("00000000-0000-0000-0000-000000106001"), TroopProgressionState())
        fun surfaces(settings: PlayerMatchSettings) = listOf(
            PregamePreferenceMenus.settings(settings).toLiveView(),
            DynamicMatchMenus.settings(player.copy(
                interaction = player.interaction.copy(settings = settings)
            )).toLiveView()
        )
        val unlocked = surfaces(PlayerMatchSettings(lifetimeWins = 20))
        val locked = surfaces(PlayerMatchSettings(lifetimeWins = 19))
        val disabledSettings = PlayerMatchSettings(lifetimeWins = 20)
        disabledSettings.setAutoCentre(false)
        val disabled = surfaces(disabledSettings)
        val layout = HotbarLayout.OFFICIAL_2021_SCREENSHOT_EXAMPLE
        player.interaction.hotbarLayout = layout
        player.interaction.aoeInventory.add("meteor", 1)
        val editors = listOf(
            PregamePreferenceMenus.hotbar(layout, null).toLiveView(),
            DynamicMatchMenus.hotbarEditor(player).toLiveView()
        )
        fun invalidSpansRejected(spans: List<MenuBoldSpan>, line: Int = 0): Boolean = runCatching {
            MenuSlot(0, "noop:invalid-style", UiEvidenceStatus.ENGINEERING_FALLBACK,
                "name", lore = listOf("short"), textStyle = MenuTextStyle(
                    MenuTextColor.GRAY, loreBoldSpans = mapOf(line to spans)))
        }.isFailure
        return listOf(
            FixtureResult("2021-settings-visible-powder-feather-and-panes-stay-non-functional",
                unlocked.all { view -> mapOf(4 to "blaze-powder", 10 to "feather", 19 to "glass-pane-red", 20 to "glass-pane-lime").all { (slot, hint) ->
                    view.slotIconHints[slot] == hint && view.slotActionIds[slot]?.startsWith("noop:") == true
                } }),
            FixtureResult("settings-particle-runtime-remains-reachable-away-from-observed-feather",
                unlocked[0].slotActionIds[16] == "pregame-settings:particle-density" &&
                    unlocked[1].slotActionIds[16] == "settings:particle-density"),
            FixtureResult("2021-hotbar-editor-arrow-slot-eight-is-visual-only-on-both-surfaces",
                editors.all { it.slotIconHints[8] == "arrow" && it.slotActionIds[8] == "noop:hotbar:2021-arrow-unresolved" }),
            FixtureResult("2021-hotbar-empty-cell-keeps-position-based-place-action",
                PregamePreferenceMenus.hotbar(layout, HotbarEditorSelection.AoE("meteor")).toLiveView().let {
                    it.slotIconHints[30] == "empty-slot" && it.slotActionIds[30] == "pregame-hotbar:place:3"
                } && player.interaction.let { interaction ->
                    interaction.hotbarEditorSelection = HotbarEditorSelection.AoE("meteor")
                    DynamicMatchMenus.hotbarEditor(player).toLiveView().let {
                        it.slotIconHints[30] == "empty-slot" && it.slotActionIds[30] == "hotbar:place:3"
                    }
                }),
            FixtureResult("builder-inferred-icon-identities-remain-contextual-evidence",
                TowerBuilderMenus.threeByThree2021.slots.filter { it.actionId in setOf("tower:ice", "tower:mage", "tower:sorcerer") }
                    .all { it.evidenceStatus == UiEvidenceStatus.MATURE_CONTEXT }),
            FixtureResult("2021-rangefinder-name-keeps-yellow-base-and-only-enable-green-bold",
                TowerManagementMenus.mature2021(1, "Zeus Tower", 1, false).toLiveView().let { view ->
                    view.slotTextStyles[45]?.nameColor == MenuTextColor.YELLOW &&
                        view.slotTextStyles[45]?.nameBoldSpans == listOf(MenuBoldSpan(9, 15, MenuTextColor.GREEN)) &&
                        view.slotDisplayNames.getValue(45).substring(9, 15) == "Enable" &&
                        view.slotActionIds[45] == "tower-manage:1:rangefinder"
                } && TowerManagementMenus.mature2021(1, "Zeus Tower", 1, true).slots.first { it.slot == 45 }.let {
                    it.evidenceStatus == UiEvidenceStatus.MATURE_CONTEXT &&
                        it.textStyle?.nameBoldSpans?.isEmpty() == true
                }),
            FixtureResult("2021-partial-bold-spans-preserve-target-substrings-and-blank-lines",
                unlocked.all { view ->
                    view.slotTextStyles[11]?.nameBoldSpans?.map {
                        view.slotDisplayNames.getValue(11).substring(it.start, it.endExclusive)
                    } == listOf("ENABLED")
                } && disabled.all { it.slotTextStyles[11]?.nameBoldSpans?.isEmpty() == true } &&
                    TowerBuilderMenus.pathSelector2021("zeus").toLiveView().let { view ->
                        view.slotLore[15] == listOf("", "Summons baby Zeus", "", "Click to select path!") &&
                            view.slotTextStyles[15]?.loreBoldSpans?.get(1)?.map {
                                view.slotLore.getValue(15)[1].substring(it.start, it.endExclusive)
                            } == listOf("baby Zeus")
                    }),
            FixtureResult("menu-text-style-rejects-overlapping-out-of-bounds-and-missing-line-spans",
                invalidSpansRejected(listOf(MenuBoldSpan(0, 6))) &&
                    invalidSpansRejected(listOf(MenuBoldSpan(0, 3), MenuBoldSpan(2, 4))) &&
                    invalidSpansRejected(listOf(MenuBoldSpan(0, 1)), 1)),
            FixtureResult("2021-settings-auto-centre-text-style-survives-both-live-projections",
                unlocked.all {
                    it.slotIconHints[11] == "torch" &&
                        it.slotDisplayNames[11] == "Auto centre towers - ENABLED" &&
                        it.slotLore[11]?.last() == "Click to disable" &&
                        it.slotTextStyles[11]?.nameColor == MenuTextColor.GREEN &&
                        it.slotTextStyles[11]?.loreColors?.get(2) == MenuTextColor.GOLD
                } && unlocked[0].slotActionIds[11] == "pregame-settings:auto-centre" &&
                    unlocked[1].slotActionIds[11] == "settings:auto-centre"),
            FixtureResult("2021-settings-auto-centre-lore-respects-19-vs-20-win-boundary",
                locked.all { it.slotLore[11]?.last() == "Disable locked: requires 20 wins" } &&
                    disabled.all {
                        it.slotDisplayNames[11] == "Auto centre towers - DISABLED" &&
                            it.slotLore[11]?.last() == "Click to enable" &&
                            it.slotTextStyles[11]?.nameColor == MenuTextColor.RED
                    }),
            FixtureResult("2021-editor-item-identities-match-live-loadout-without-claiming-source-order",
                editors.all { editor ->
                    editor.slotIconHints[33] == "bricks" &&
                        editor.slotActionIds.filterValues { it.contains("select-aoe:") }
                            .keys.all { editor.slotIconHints[it] == "splash-potion" }
                } && MatchHotbarProjector.project(player).first {
                    it.action == HotbarAction.CASTLE_BAZAAR
                }.visual == MatchHotbarVisualKind.CASTLE_BAZAAR_BRICKS),
            FixtureResult("2021-zeus-path2-style-preserves-unresolved-paths-as-plain-fallback",
                TowerBuilderMenus.pathSelector2021("zeus").toLiveView()
                    .slotTextStyles[15]?.loreColors?.get(3) == MenuTextColor.RED &&
                    TowerBuilderMenus.pathSelector2021("archer").toLiveView()
                        .slotTextStyles.isEmpty())
        )
    }
}
