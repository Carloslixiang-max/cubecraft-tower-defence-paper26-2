package dev.cubecrafttd.ui

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.mob.MobDefinitionRepository
import dev.cubecrafttd.progression.TroopProgressionState
import dev.cubecrafttd.troop.TroopSendQueue
import java.util.UUID

data class SummonerDraftEditReceipt(
    val key: SummonerDraftKey,
    val quantity: Int,
    val totalDraftUnits: Int
)

class SummonerDraftService(
    private val definitions:
        MobDefinitionRepository,
    private val ledger:
        EconomyLedger
) {
    fun edit(
        draft: SummonerDraftState,
        progression:
            TroopProgressionState,
        queue: TroopSendQueue,
        team: TeamId,
        playerUuid: UUID,
        key: SummonerDraftKey,
        click: ClickKind
    ): SummonerDraftEditReceipt {
        check(
            progression.level(
                key.mobId
            ) >= key.level
        ) {
            "${key.mobId} L${key.level} is not unlocked"
        }

        val unitCost=
            definitions.get(key.mobId)
                .level(key.level)
                .sendCoins
        check(unitCost>=0L)

        val current=
            draft.quantity(key)
        val otherLinesCost=
            draft.snapshot()
                .filter {
                    it.key!=key
                }
                .sumOf {
                    line ->
                    Math.multiplyExact(
                        definitions
                            .get(
                                line.key.mobId
                            )
                            .level(
                                line.key.level
                            )
                            .sendCoins,
                        line.quantity
                            .toLong()
                    )
                }
        val balance=
            ledger.balance(
                EconomyAccount(
                    team,
                    playerUuid,
                    EconomyCurrency
                        .MATCH_COINS
                )
            )
        val spendable=
            (balance-otherLinesCost)
                .coerceAtLeast(0L)
        val affordableTotal=
            if(unitCost==0L)
                Int.MAX_VALUE
            else
                (spendable/unitCost)
                    .coerceAtMost(
                        Int.MAX_VALUE
                            .toLong()
                    )
                    .toInt()

        val otherDraftUnits=
            draft.totalUnits()-
                current
        val totalCapacityForKey=
            (
                queue.capacityUnits-
                    queue.usedUnits()-
                    otherDraftUnits
            ).coerceAtLeast(0)

        val maxTotalForKey=
            minOf(
                totalCapacityForKey,
                affordableTotal
            )

        val desired=
            when(click) {
                ClickKind.LEFT ->
                    (current+1)
                        .coerceAtMost(
                            maxTotalForKey
                        )
                ClickKind.RIGHT ->
                    (current-1)
                        .coerceAtLeast(0)
                ClickKind.SHIFT_LEFT ->
                    maxTotalForKey
                ClickKind.SHIFT_RIGHT ->
                    0
            }

        draft.setQuantity(
            key,desired
        )

        return SummonerDraftEditReceipt(
            key,desired,
            draft.totalUnits()
        )
    }
}
