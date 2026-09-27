package dev.cubecrafttd.ui

import dev.cubecrafttd.match.MatchOutcome
import dev.cubecrafttd.match.TimeoutTiePolicy

/**
 * Normal match results must not display Engineering placeholder victory/defeat
 * UI. Historical evidence currently supports the Top players presentation, but
 * not the exact original victory/defeat title wording/timing.
 *
 * Engineering overlays remain available only for explicit Engineering stop
 * outcomes where their non-historical nature is intentional.
 */
object MatchEndFeedbackPolicy {
    fun showEngineeringOverlay(
        outcome: MatchOutcome
    ): Boolean =
        outcome is MatchOutcome.Tie &&
            outcome.policy==
                TimeoutTiePolicy
                    .ENGINEERING_CUSTOM
}
