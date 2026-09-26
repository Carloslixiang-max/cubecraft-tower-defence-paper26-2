package dev.cubecrafttd.testing

import dev.cubecrafttd.match.HistoricalPregameArmageddonVoteOption
import dev.cubecrafttd.match.HistoricalPregamePricingVoteOption
import dev.cubecrafttd.ui.PregameQueueHudProjection
import dev.cubecrafttd.ui.PregameQueueHudState

object PregameQueueHudFixture {
    fun run(): List<FixtureResult> {
        val defaults=
            PregameQueueHudProjection.text(
                PregameQueueHudState(
                    waitingPosition=2,
                    countdownSeconds=null,
                    armageddonVote=null,
                    pricingVote=null
                )
            )
        val voted=
            PregameQueueHudProjection.text(
                PregameQueueHudState(
                    waitingPosition=1,
                    countdownSeconds=null,
                    armageddonVote=
                        HistoricalPregameArmageddonVoteOption
                            .LIGHTNING,
                    pricingVote=
                        HistoricalPregamePricingVoteOption
                            .DOUBLE_INCOME
                )
            )
        val countdown=
            PregameQueueHudProjection.text(
                PregameQueueHudState(
                    waitingPosition=null,
                    countdownSeconds=3,
                    armageddonVote=
                        HistoricalPregameArmageddonVoteOption
                            .RANDOM,
                    pricingVote=
                        HistoricalPregamePricingVoteOption
                            .QUICK_START
                )
            )

        return listOf(
            FixtureResult(
                "pregame-queue-hud-shows-no-vote-defaults",
                defaults.contains(
                    "TD queue #2"
                ) &&
                    defaults.contains(
                        "A:Random(default)"
                    ) &&
                    defaults.contains(
                        "P:Normal(default)"
                    )
            ),
            FixtureResult(
                "pregame-queue-hud-shows-player-votes",
                voted.contains(
                    "A:LIGHTNING"
                ) &&
                    voted.contains(
                        "P:DOUBLE INCOME"
                    )
            ),
            FixtureResult(
                "pregame-queue-hud-shows-start-countdown",
                countdown.contains(
                    "TD starts in 3s"
                ) &&
                    countdown.contains(
                        "/ctdvote"
                    )
            )
        )
    }
}
