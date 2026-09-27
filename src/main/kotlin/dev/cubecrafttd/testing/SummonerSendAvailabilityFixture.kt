package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.troop.*
import dev.cubecrafttd.ui.*
import java.util.UUID

object SummonerSendAvailabilityFixture {
    fun run(): List<FixtureResult> {
        val player=UUID.fromString(
            "00000000-0000-0000-0000-000000091001"
        )
        val ledger=EconomyLedger()
        val account=
            EconomyAccount(
                TeamId.RED,
                player,
                EconomyCurrency.MATCH_COINS
            )
        ledger.apply(
            EconomyTransaction(
                1L,0L,account,100L,
                EconomyReason
                    .MATCH_INITIALIZATION,
                "seed"
            )
        )

        val queue=
            TroopSendQueue(
                TeamId.BLUE,
                12
            )
        val cooldowns=
            DeterministicCooldownTracker()
        val draft=
            SummonerDraftState()
        val empty=
            SummonerSendAvailabilityEvaluator
                .evaluate(
                    draft,queue,
                    TeamId.RED,player,
                    ledger,cooldowns,0L
                )

        draft.setQuantity(
            SummonerDraftKey(
                "zombie",1
            ),
            2
        )
        val ready=
            SummonerSendAvailabilityEvaluator
                .evaluate(
                    draft,queue,
                    TeamId.RED,player,
                    ledger,cooldowns,0L
                )

        check(
            cooldowns.consume(
                CooldownKey(
                    "sender:$player"
                ),
                0L,
                NormalModeConstants
                    .SEND_COOLDOWN_TICKS
            )
        )
        val coolingDown=
            SummonerSendAvailabilityEvaluator
                .evaluate(
                    draft,queue,
                    TeamId.RED,player,
                    ledger,cooldowns,1L
                )

        val capacityQueue=
            TroopSendQueue(
                TeamId.BLUE,
                1
            )
        val noCapacity=
            SummonerSendAvailabilityEvaluator
                .evaluate(
                    draft,capacityQueue,
                    TeamId.RED,player,
                    ledger,
                    DeterministicCooldownTracker(),
                    0L
                )

        val poorLedger=
            EconomyLedger()
        val tooPoor=
            SummonerSendAvailabilityEvaluator
                .evaluate(
                    draft,queue,
                    TeamId.RED,player,
                    poorLedger,
                    DeterministicCooldownTracker(),
                    0L
                )

        return listOf(
            FixtureResult(
                "summoner-send-availability-empty-draft-blocked",
                !empty.canSend &&
                    empty.blockers==
                    setOf(
                        SummonerSendBlocker
                            .EMPTY_DRAFT
                    )
            ),
            FixtureResult(
                "summoner-send-availability-ready-matches-real-preflight",
                ready.canSend &&
                    ready.totalUnits==2 &&
                    ready.totalCost==30L &&
                    ready.blockers.isEmpty()
            ),
            FixtureResult(
                "summoner-send-availability-cooldown-blocked",
                !coolingDown.canSend &&
                    SummonerSendBlocker
                        .COOLDOWN_ACTIVE in
                        coolingDown.blockers
            ),
            FixtureResult(
                "summoner-send-availability-queue-capacity-blocked",
                !noCapacity.canSend &&
                    SummonerSendBlocker
                        .QUEUE_CAPACITY in
                        noCapacity.blockers
            ),
            FixtureResult(
                "summoner-send-availability-coins-blocked",
                !tooPoor.canSend &&
                    SummonerSendBlocker
                        .INSUFFICIENT_COINS in
                        tooPoor.blockers
            )
        )
    }
}
