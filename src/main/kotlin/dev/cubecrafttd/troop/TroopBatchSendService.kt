package dev.cubecrafttd.troop

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.mob.MobDefinitionRepository
import java.util.UUID

data class TroopBatchSelection(
    val mobId: String,
    val level: Int,
    val quantity: Int,
    val entryId: TroopQueueEntryId,
    val transactionId: Long,
    val correlationId: String
) {
    init {
        require(quantity > 0)
    }
}

data class TroopBatchSendCommand(
    val senderPlayerUuid: UUID,
    val senderTeam: TeamId,
    val selections:
        List<TroopBatchSelection>,
    val cooldownKey: CooldownKey
)

data class TroopBatchSendReceipt(
    val attackedTeam: TeamId,
    val totalUnits: Int,
    val totalCost: Long,
    val queueUsedUnits: Int,
    val cooldownReadyAtTick: Long,
    val entryIds: List<TroopQueueEntryId>
)

class TroopBatchSendService(
    private val definitions:
        MobDefinitionRepository,
    private val ledger:
        EconomyLedger,
    private val cooldowns:
        DeterministicCooldownTracker
) {
    fun execute(
        queue: TroopSendQueue,
        command: TroopBatchSendCommand,
        gameTick: Long,
        cooldownTicks: Long =
            NormalModeConstants
                .SEND_COOLDOWN_TICKS
    ): TroopBatchSendReceipt {
        check(command.selections.isNotEmpty()) {
            "Summoner draft is empty"
        }
        check(
            cooldowns.isReady(
                command.cooldownKey,
                gameTick
            )
        ) {
            "Troop send cooldown active"
        }

        val attackedTeam=
            opposite(command.senderTeam)
        check(
            queue.attackedTeam==
                attackedTeam
        )

        val ids=
            command.selections.map {
                it.entryId
            }
        check(ids.distinct().size==ids.size) {
            "Duplicate queue entry IDs"
        }

        val correlations=
            command.selections.map {
                it.correlationId
            }
        check(
            correlations.distinct().size==
                correlations.size
        ) {
            "Duplicate batch correlation IDs"
        }
        check(
            correlations.none(
                ledger::hasAppliedCorrelation
            )
        ) {
            "Batch correlation already applied"
        }

        val totalUnits=
            command.selections.sumOf {
                it.quantity
            }
        check(
            queue.usedUnits()+
                totalUnits <=
                queue.capacityUnits
        ) {
            "Troop queue cannot accept batch"
        }

        val priced=
            command.selections.map {
                selection ->
                val level=
                    definitions
                        .get(
                            selection.mobId
                        )
                        .level(
                            selection.level
                        )
                val cost=
                    Math.multiplyExact(
                        level.sendCoins,
                        selection.quantity
                            .toLong()
                    )
                selection to cost
            }

        val totalCost=
            priced.fold(0L) {
                acc,(_,cost) ->
                Math.addExact(acc,cost)
            }
        val payer=
            EconomyAccount(
                command.senderTeam,
                command.senderPlayerUuid,
                EconomyCurrency.MATCH_COINS
            )
        check(
            ledger.balance(payer)>=
                totalCost
        ) {
            "Insufficient MATCH_COINS"
        }

        val entries=
            command.selections.map {
                selection ->
                TroopQueueEntry(
                    entryId=
                        selection.entryId,
                    senderPlayerUuid=
                        command.senderPlayerUuid,
                    attackedTeam=
                        attackedTeam,
                    mobId=
                        selection.mobId,
                    level=
                        selection.level,
                    quantity=
                        selection.quantity,
                    enqueuedAtTick=
                        gameTick,
                    purchaseCorrelationId=
                        selection
                            .correlationId
                )
            }

        // All deterministic failure points are checked above. Arena mutation is
        // single-threaded from here.
        priced.forEach {
            (selection,cost) ->
            check(
                ledger.apply(
                    EconomyTransaction(
                        transactionId=
                            selection
                                .transactionId,
                        arenaTick=
                            gameTick,
                        account=payer,
                        delta=-cost,
                        reason=
                            EconomyReason
                                .TROOP_PURCHASE,
                        correlationId=
                            selection
                                .correlationId,
                        truthFieldKey=
                            "troops.${selection.mobId}." +
                                "level${selection.level}.send_coins",
                        metadata=mapOf(
                            "mobId" to
                                selection.mobId,
                            "level" to
                                selection.level
                                    .toString(),
                            "quantity" to
                                selection.quantity
                                    .toString(),
                            "batch" to "true"
                        )
                    )
                )
            )
        }

        entries.forEach(
            queue::enqueueAfterPreflight
        )

        check(
            cooldowns.consume(
                command.cooldownKey,
                gameTick,
                cooldownTicks
            )
        ) {
            "Cooldown state changed after batch preflight"
        }

        return TroopBatchSendReceipt(
            attackedTeam=attackedTeam,
            totalUnits=totalUnits,
            totalCost=totalCost,
            queueUsedUnits=
                queue.usedUnits(),
            cooldownReadyAtTick=
                cooldowns.readyAtTick(
                    command.cooldownKey
                ),
            entryIds=ids
        )
    }

    private fun opposite(
        team: TeamId
    ): TeamId =
        when(team) {
            TeamId.RED -> TeamId.BLUE
            TeamId.BLUE -> TeamId.RED
        }
}
