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
        return listOf(
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
                    .slotTextStyles[15]?.loreColors?.get(2) == MenuTextColor.RED &&
                    TowerBuilderMenus.pathSelector2021("archer").toLiveView()
                        .slotTextStyles.isEmpty())
        )
    }
}
