package dev.cubecrafttd.testing

import dev.cubecrafttd.ui.*

object PregamePreferenceStateFixture {
    fun run(): List<FixtureResult> {
        val state=
            PregamePreferenceState()

        val particles=
            PregamePreferenceActionService
                .handle(
                    state,
                    "pregame-settings:particle-density"
                ) as
                PregamePreferenceActionResult
                    .SettingsChanged
        val digital=
            PregamePreferenceActionService
                .handle(
                    state,
                    "pregame-settings:digital-mob-health"
                ) as
                PregamePreferenceActionResult
                    .SettingsChanged
        val lockedAutoCentre=
            runCatching {
                PregamePreferenceActionService
                    .handle(
                        state,
                        "pregame-settings:auto-centre"
                    )
            }.isFailure

        val selected=
            PregamePreferenceActionService
                .handle(
                    state,
                    "pregame-hotbar:select:SUMMONER"
                ) as
                PregamePreferenceActionResult
                    .HotbarSelectionChanged
        val moved=
            PregamePreferenceActionService
                .handle(
                    state,
                    "pregame-hotbar:place:8"
                ) as
                PregamePreferenceActionResult
                    .HotbarLayoutChanged

        val aoeState=
            PregamePreferenceState()
        val aoeSelected=
            PregamePreferenceActionService
                .handle(
                    aoeState,
                    "pregame-hotbar:select-aoe:meteor"
                ) as
                PregamePreferenceActionResult
                    .HotbarSelectionChanged
        val aoePlaced=
            PregamePreferenceActionService
                .handle(
                    aoeState,
                    "pregame-hotbar:place:4"
                ) as
                PregamePreferenceActionResult
                    .HotbarLayoutChanged

        return listOf(
            FixtureResult(
                "pregame-preferences-settings-reuse-match-rules",
                particles.settings
                    .particleDensity==
                    ParticleDensitySetting
                        .REDUCED_100 &&
                    digital.settings
                        .digitalMobHealth
            ),
            FixtureResult(
                "pregame-preferences-do-not-fabricate-auto-centre-win-unlock",
                lockedAutoCentre &&
                    state.settings
                        .autoCentreTowers
            ),
            FixtureResult(
                "pregame-preferences-fixed-action-selection-and-swap",
                selected.selection==
                    HotbarEditorSelection
                        .Action(
                            HotbarAction.SUMMONER
                        ) &&
                    moved.layout
                        .slot(
                            HotbarAction.SUMMONER
                        )==8 &&
                    moved.layout
                        .slot(
                            HotbarAction.SETTINGS
                        )==2 &&
                    state.hotbarEditorSelection==
                        null
            ),
            FixtureResult(
                "pregame-preferences-known-aoe-can-fill-empty-slot",
                aoeSelected.selection==
                    HotbarEditorSelection
                        .AoE(
                            "meteor"
                        ) &&
                    aoePlaced.layout
                        .aoeSlot(
                            "meteor"
                        )==4 &&
                    aoeState.hotbarEditorSelection==
                        null
            )
        )
    }
}
