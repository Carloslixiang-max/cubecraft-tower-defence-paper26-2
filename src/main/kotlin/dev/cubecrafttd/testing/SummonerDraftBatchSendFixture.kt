package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.progression.*
import dev.cubecrafttd.troop.*
import dev.cubecrafttd.ui.*
import java.util.UUID

object SummonerDraftBatchSendFixture {
    fun run():List<FixtureResult> {
        val player=UUID.fromString(
            "00000000-0000-0000-0000-000000028001"
        )
        val ledger=EconomyLedger()
        val account=EconomyAccount(
            TeamId.RED,player,
            EconomyCurrency.MATCH_COINS
        )
        ledger.apply(
            EconomyTransaction(
                1,0,account,1000,
                EconomyReason
                    .MATCH_INITIALIZATION,
                "seed"
            )
        )

        val progression=
            TroopProgressionState(
                linkedMapOf(
                    "zombie" to 2,
                    "spider" to 1
                )
            )
        val queue=
            TroopSendQueue(
                TeamId.BLUE,12
            )
        val draft=
            SummonerDraftState()
        val edit=
            SummonerDraftService(
                RecommendedMatureMobDefinitions,
                ledger
            )

        val zombie=
            SummonerDraftKey(
                "zombie",2
            )
        val spider=
            SummonerDraftKey(
                "spider",1
            )

        edit.edit(
            draft,progression,queue,
            TeamId.RED,player,
            zombie,ClickKind.LEFT
        )
        edit.edit(
            draft,progression,queue,
            TeamId.RED,player,
            spider,ClickKind.LEFT
        )
        edit.edit(
            draft,progression,queue,
            TeamId.RED,player,
            zombie,ClickKind.LEFT
        )

        val ordered=
            draft.snapshot()

        val cooldowns=
            DeterministicCooldownTracker()
        val send=
            TroopBatchSendService(
                RecommendedMatureMobDefinitions,
                ledger,cooldowns
            )
        val receipt=
            send.execute(
                queue,
                TroopBatchSendCommand(
                    senderPlayerUuid=player,
                    senderTeam=TeamId.RED,
                    selections=
                        ordered.mapIndexed {
                            index,line ->
                            TroopBatchSelection(
                                line.key.mobId,
                                line.key.level,
                                line.quantity,
                                TroopQueueEntryId(
                                    (index+1).toLong()
                                ),
                                10L+index,
                                "batch-$index"
                            )
                        },
                    cooldownKey=
                        CooldownKey(
                            "sender:$player"
                        )
                ),
                gameTick=100
            )
        draft.clear()

        val queueOrder=
            queue.snapshot()

        return listOf(
            FixtureResult(
                "summoner-draft-preserves-first-selection-order",
                ordered.map {
                    it.key.mobId
                }==
                    listOf(
                        "zombie","spider"
                    ) &&
                    ordered.first()
                        .quantity==2
            ),
            FixtureResult(
                "summoner-batch-send-one-cooldown",
                receipt.totalUnits==3 &&
                    receipt.cooldownReadyAtTick==
                        400L &&
                    cooldowns
                        .readyAtTick(
                            CooldownKey(
                                "sender:$player"
                            )
                        )==400L
            ),
            FixtureResult(
                "summoner-batch-fifo-entry-order",
                queueOrder.map {
                    it.mobId
                }==
                    listOf(
                        "zombie","spider"
                    ) &&
                    queueOrder[0].quantity==2
            ),
            FixtureResult(
                "summoner-batch-charges-total-cost",
                ledger.balance(account)==
                    1000L-
                    (18L*2L+25L)
            ),
            FixtureResult(
                "summoner-draft-clears-after-commit",
                draft.isEmpty()
            )
        )
    }
}
