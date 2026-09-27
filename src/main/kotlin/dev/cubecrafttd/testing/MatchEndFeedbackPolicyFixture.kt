package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.match.*
import dev.cubecrafttd.ui.MatchEndFeedbackPolicy

object MatchEndFeedbackPolicyFixture {
    fun run(): List<FixtureResult> =
        listOf(
            FixtureResult(
                "normal-match-results-do-not-show-engineering-overlay",
                !MatchEndFeedbackPolicy
                    .showEngineeringOverlay(
                        MatchOutcome.Winner(
                            TeamId.RED,
                            "blue_castle_destroyed"
                        )
                    ) &&
                    !MatchEndFeedbackPolicy
                        .showEngineeringOverlay(
                            MatchOutcome.Tie(
                                TimeoutTiePolicy.DRAW,
                                "equal_castle_health_at_timeout"
                            )
                        )
            ),
            FixtureResult(
                "engineering-stop-keeps-explicit-engineering-overlay",
                MatchEndFeedbackPolicy
                    .showEngineeringOverlay(
                        MatchOutcome.Tie(
                            TimeoutTiePolicy
                                .ENGINEERING_CUSTOM,
                            "stage4_manual_stop"
                        )
                    )
            )
        )
