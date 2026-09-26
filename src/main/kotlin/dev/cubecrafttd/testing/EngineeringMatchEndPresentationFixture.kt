package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.match.*
import dev.cubecrafttd.ui.*

object EngineeringMatchEndPresentationFixture {
    fun run(): List<FixtureResult> {
        val win=
            MatchOutcome.Winner(
                TeamId.RED,
                "blue_castle_destroyed"
            )
        val redWin=
            EngineeringMatchEndProjector
                .project(
                    win,
                    TeamId.RED
                )
        val blueLose=
            EngineeringMatchEndProjector
                .project(
                    win,
                    TeamId.BLUE
                )
        val draw=
            EngineeringMatchEndProjector
                .project(
                    MatchOutcome.Tie(
                        TimeoutTiePolicy.DRAW,
                        "equal_castle_health_at_timeout"
                    ),
                    TeamId.RED
                )
        val stopped=
            EngineeringMatchEndProjector
                .project(
                    MatchOutcome.Tie(
                        TimeoutTiePolicy
                            .ENGINEERING_CUSTOM,
                        "stage4_manual_stop"
                    ),
                    TeamId.BLUE
                )

        return listOf(
            FixtureResult(
                "engineering-match-end-winner-perspective",
                redWin.tone==
                    EngineeringMatchEndTone
                        .VICTORY &&
                    redWin.title==
                        "ENG VICTORY" &&
                    redWin.subtitle==
                        "Enemy castle destroyed"
            ),
            FixtureResult(
                "engineering-match-end-loser-perspective",
                blueLose.tone==
                    EngineeringMatchEndTone
                        .DEFEAT &&
                    blueLose.title==
                        "ENG DEFEAT" &&
                    blueLose.subtitle==
                        "Your castle was destroyed"
            ),
            FixtureResult(
                "engineering-match-end-draw-perspective",
                draw.tone==
                    EngineeringMatchEndTone
                        .DRAW &&
                    draw.title==
                        "ENG DRAW"
            ),
            FixtureResult(
                "engineering-match-end-manual-stop-not-fake-draw",
                stopped.tone==
                    EngineeringMatchEndTone
                        .ENDED &&
                    stopped.subtitle==
                        "Engineering test stopped"
            )
        )
    }
}
