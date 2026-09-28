package dev.cubecrafttd.ui

data class PregamePreferenceState(
    val settings:
        PlayerMatchSettings =
        PlayerMatchSettings(
            lifetimeWins=0
        ),
    var hotbarLayout:
        HotbarLayout =
        HotbarLayout
            .ENGINEERING_RUNTIME_DEFAULT,
    var hotbarEditorSelection:
        HotbarEditorSelection? = null
)

sealed interface PregamePreferenceActionResult {
    data class SettingsChanged(
        val settings:
            PlayerMatchSettings
    ) : PregamePreferenceActionResult

    data class HotbarSelectionChanged(
        val selection:
            HotbarEditorSelection
    ) : PregamePreferenceActionResult

    data class HotbarLayoutChanged(
        val layout:
            HotbarLayout
    ) : PregamePreferenceActionResult
}

/**
 * Pure waiting-lobby preference actions.
 *
 * This deliberately mirrors the in-match Settings/Hotbar rules rather than
 * mutating any real Bukkit inventory. Paper only persists HotbarLayout after a
 * successful layout change and injects Settings into the match bootstrap.
 */
object PregamePreferenceActionService {
    fun handle(
        state: PregamePreferenceState,
        actionId: String
    ): PregamePreferenceActionResult {
        val parts=
            actionId.split(':')

        return when(
            parts.firstOrNull()
        ) {
            "pregame-settings" ->
                handleSettings(
                    state,
                    parts
                )
            "pregame-hotbar" ->
                handleHotbar(
                    state,
                    parts
                )
            else ->
                error(
                    "Unknown pregame preference action $actionId"
                )
        }
    }

    private fun handleSettings(
        state: PregamePreferenceState,
        parts: List<String>
    ): PregamePreferenceActionResult {
        check(parts.size==2) {
            "pregame-settings:<setting>"
        }
        val settings=
            state.settings

        when(parts[1]) {
            "particle-density" ->
                settings
                    .cycleParticleDensity()
            "auto-centre" ->
                settings.setAutoCentre(
                    !settings.autoCentreTowers
                )
            "digital-mob-health" ->
                settings.digitalMobHealth=
                    !settings.digitalMobHealth
            "damage-indicators" ->
                settings.damageIndicators=
                    !settings.damageIndicators
            else ->
                error(
                    "Unknown pregame setting " +
                        parts[1]
                )
        }

        return PregamePreferenceActionResult
            .SettingsChanged(
                settings
            )
    }

    private fun handleHotbar(
        state: PregamePreferenceState,
        parts: List<String>
    ): PregamePreferenceActionResult {
        check(parts.size>=2) {
            "pregame-hotbar:<select|select-aoe|place>:..."
        }

        return when(parts[1]) {
            "select" -> {
                check(parts.size==3) {
                    "pregame-hotbar:select:<action>"
                }
                val selection=
                    HotbarEditorSelection
                        .Action(
                            HotbarAction
                                .valueOf(
                                    parts[2]
                                        .uppercase()
                                )
                        )
                state.hotbarEditorSelection=
                    selection
                PregamePreferenceActionResult
                    .HotbarSelectionChanged(
                        selection
                    )
            }

            "select-aoe" -> {
                check(parts.size==3) {
                    "pregame-hotbar:select-aoe:<potionId>"
                }
                RecommendedMatureBazaarDefinitions
                    .potion(
                        parts[2]
                    )
                val selection=
                    HotbarEditorSelection
                        .AoE(
                            parts[2]
                        )
                state.hotbarEditorSelection=
                    selection
                PregamePreferenceActionResult
                    .HotbarSelectionChanged(
                        selection
                    )
            }

            "place" -> {
                check(parts.size==3) {
                    "pregame-hotbar:place:<slot>"
                }
                val slot=
                    parts[2].toInt()
                val selection=
                    state.hotbarEditorSelection
                        ?: error(
                            "Select a hotbar entry first"
                        )
                val next=
                    when(selection) {
                        is HotbarEditorSelection
                            .Action ->
                            state.hotbarLayout
                                .move(
                                    selection.action,
                                    slot
                                )
                        is HotbarEditorSelection
                            .AoE ->
                            state.hotbarLayout
                                .placeAoE(
                                    selection.potionId,
                                    slot
                                )
                    }

                state.hotbarLayout=
                    next
                state.hotbarEditorSelection=
                    null
                PregamePreferenceActionResult
                    .HotbarLayoutChanged(
                        next
                    )
            }

            else ->
                error(
                    "Unknown pregame hotbar action " +
                        parts[1]
                )
        }
    }
}
