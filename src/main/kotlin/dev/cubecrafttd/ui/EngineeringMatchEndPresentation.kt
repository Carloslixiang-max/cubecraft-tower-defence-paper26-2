package dev.cubecrafttd.ui

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.match.MatchOutcome
import dev.cubecrafttd.match.TimeoutTiePolicy

enum class EngineeringMatchEndTone {
    VICTORY,
    DEFEAT,
    DRAW,
    ENDED
}

data class EngineeringMatchEndView(
    val tone: EngineeringMatchEndTone,
    val title: String,
    val subtitle: String,
    val chatLine: String
)

/**
 * Engineering-only end-of-match presentation.
 *
 * Exact original CubeCraft title wording, colours and timings are not currently
 * recovered with enough confidence. Keep outcome semantics separate from the
 * replaceable presentation layer.
 */
object EngineeringMatchEndProjector {
    fun project(
        outcome: MatchOutcome,
        viewerTeam: TeamId
    ): EngineeringMatchEndView =
        when(outcome) {
            is MatchOutcome.Winner ->
                if(outcome.team==viewerTeam) {
                    EngineeringMatchEndView(
                        EngineeringMatchEndTone.VICTORY,
                        "ENG VICTORY",
                        winnerSubtitle(
                            outcome.reason
                        ),
                        "ENG match result: VICTORY"
                    )
                } else {
                    EngineeringMatchEndView(
                        EngineeringMatchEndTone.DEFEAT,
                        "ENG DEFEAT",
                        loserSubtitle(
                            outcome.reason
                        ),
                        "ENG match result: DEFEAT"
                    )
                }

            is MatchOutcome.Tie ->
                if(
                    outcome.policy==
                        TimeoutTiePolicy
                            .ENGINEERING_CUSTOM
                ) {
                    EngineeringMatchEndView(
                        EngineeringMatchEndTone.ENDED,
                        "ENG MATCH ENDED",
                        engineeringStopSubtitle(
                            outcome.reason
                        ),
                        "ENG match ended: " +
                            engineeringStopSubtitle(
                                outcome.reason
                            )
                    )
                } else {
                    EngineeringMatchEndView(
                        EngineeringMatchEndTone.DRAW,
                        "ENG DRAW",
                        "Castles finished level",
                        "ENG match result: DRAW"
                    )
                }

            MatchOutcome.Continue ->
                error(
                    "Cannot project unfinished match outcome"
                )
        }

    private fun winnerSubtitle(
        reason: String
    ): String =
        when(reason) {
            "red_castle_destroyed",
            "blue_castle_destroyed" ->
                "Enemy castle destroyed"

            "higher_castle_health_at_timeout" ->
                "40:00 timeout · higher castle health"

            else ->
                readable(reason)
        }

    private fun loserSubtitle(
        reason: String
    ): String =
        when(reason) {
            "red_castle_destroyed",
            "blue_castle_destroyed" ->
                "Your castle was destroyed"

            "higher_castle_health_at_timeout" ->
                "40:00 timeout · lower castle health"

            else ->
                readable(reason)
        }

    private fun engineeringStopSubtitle(
        reason: String
    ): String =
        when(reason) {
            "stage4_manual_stop" ->
                "Engineering test stopped"

            "stage4_runtime_failure" ->
                "Engineering runtime stopped the match"

            "all_players_departed_engineering_cleanup" ->
                "No active players remained"

            else ->
                readable(reason)
        }

    private fun readable(
        reason: String
    ): String =
        reason
            .replace('_',' ')
            .trim()
            .ifBlank {
                "Match ended"
            }
}
