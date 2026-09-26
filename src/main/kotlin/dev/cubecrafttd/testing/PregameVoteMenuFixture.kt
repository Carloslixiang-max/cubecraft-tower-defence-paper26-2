package dev.cubecrafttd.testing

import dev.cubecrafttd.match.HistoricalPregameArmageddonVoteOption
import dev.cubecrafttd.match.HistoricalPregamePricingVoteOption
import dev.cubecrafttd.ui.*

object PregameVoteMenuFixture {
    fun run(): List<FixtureResult> {
        val waiting=
            PregameVoteMenuProjection.menu(
                PregameVoteMenuState(
                    armageddonVote=
                        HistoricalPregameArmageddonVoteOption
                            .LIGHTNING,
                    pricingVote=
                        HistoricalPregamePricingVoteOption
                            .DOUBLE_INCOME,
                    waitingPosition=3,
                    countdownRunning=false
                )
            )
        val starting=
            PregameVoteMenuProjection.menu(
                PregameVoteMenuState(
                    armageddonVote=null,
                    pricingVote=null,
                    waitingPosition=null,
                    countdownRunning=true
                )
            )

        val actions=
            waiting.slots
                .map { it.actionId }
                .toSet()

        return listOf(
            FixtureResult(
                "pregame-vote-menu-projects-all-runnable-vote-actions",
                actions==
                    setOf(
                        "pregame:armageddon:random",
                        "pregame:armageddon:wither",
                        "pregame:armageddon:lightning",
                        "pregame:armageddon:horde",
                        "pregame:pricing:normal",
                        "pregame:pricing:double_income",
                        "pregame:pricing:quick_start"
                    )
            ),
            FixtureResult(
                "pregame-vote-menu-marks-own-selections",
                waiting.slots.any {
                    it.actionId==
                        "pregame:armageddon:lightning" &&
                    it.displayName
                        ?.contains(
                            "YOUR VOTE"
                        )==true
                } &&
                    waiting.slots.any {
                        it.actionId==
                            "pregame:pricing:double_income" &&
                        it.displayName
                            ?.contains(
                                "YOUR VOTE"
                            )==true
                    }
            ),
            FixtureResult(
                "pregame-vote-menu-shows-queue-or-countdown-status",
                waiting.title.contains(
                    "Queue #3"
                ) &&
                    starting.title.contains(
                        "Starting"
                    )
            ),
            FixtureResult(
                "pregame-vote-menu-layout-remains-explicit-engineering-fallback",
                waiting.evidenceStatus==
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK &&
                    waiting.slots.all {
                        it.evidenceStatus==
                            UiEvidenceStatus
                                .ENGINEERING_FALLBACK
                    }
            )
        )
    }
}
