package dev.cubecrafttd.testing

import dev.cubecrafttd.ui.*

object PregamePreferenceMenuFixture {
    fun run(): List<FixtureResult> {
        val settings=
            PregamePreferenceMenus
                .settings(
                    PlayerMatchSettings(
                        lifetimeWins=0
                    )
                )
        val layout=
            HotbarLayout
                .OFFICIAL_2021_SCREENSHOT_EXAMPLE
        val hotbar=
            PregamePreferenceMenus
                .hotbar(
                    layout,
                    null
                )
        val selected=
            PregamePreferenceMenus
                .hotbar(
                    layout,
                    HotbarEditorSelection
                        .Action(
                            HotbarAction.SUMMONER
                        )
                )

        return listOf(
            FixtureResult(
                "pregame-settings-reuses-recovered-45-slot-shell-with-fail-closed-book",
                settings.size==45 &&
                    settings.evidenceStatus==
                        UiEvidenceStatus
                            .MATURE_CONTEXT &&
                    settings.actionAt(40)==
                        "noop:settings:2021-navigation-book-unresolved" &&
                    settings.slots
                        .first {
                            it.slot==40
                        }
                        .evidenceStatus==
                        UiEvidenceStatus
                            .MATURE_DIRECT
            ),
            FixtureResult(
                "pregame-settings-auto-centre-does-not-fabricate-win-unlock",
                settings.slots
                    .first {
                        it.actionId==
                            "pregame-settings:auto-centre"
                    }
                    .lore
                    .any { it.contains("Disable locked") }
            ),
            FixtureResult(
                "pregame-hotbar-projects-known-aoes-without-fake-ownership-counts",
                hotbar.size==36 &&
                    hotbar.slots
                        .filter {
                            it.slot<27 &&
                                it.actionId
                                    .startsWith(
                                        "pregame-hotbar:select-aoe:"
                                    )
                        }
                        .size==
                        RecommendedMatureBazaarDefinitions
                            .aoePotions.size &&
                    hotbar.slots.none {
                        it.displayName
                            ?.contains(
                                "owned"
                            )==true
                    }
            ),
            FixtureResult(
                "pregame-hotbar-bottom-row-selects-and-places-existing-actions",
                hotbar.actionAt(31)==
                    "pregame-hotbar:select:SUMMONER" &&
                    selected.actionAt(31)==
                        "pregame-hotbar:place:4"
            )
        )
    }
}
