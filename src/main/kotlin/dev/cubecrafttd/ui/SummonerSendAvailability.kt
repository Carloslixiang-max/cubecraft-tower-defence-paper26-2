package dev.cubecrafttd.ui

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.mob.MobDefinitionRepository
import dev.cubecrafttd.mob.RecommendedMatureMobDefinitions
import dev.cubecrafttd.troop.TroopSendQueue
import java.util.UUID

enum class SummonerSendBlocker {
    EMPTY_DRAFT,
    COOLDOWN_ACTIVE,
    QUEUE_CAPACITY,
    INSUFFICIENT_COINS
}

data class SummonerSendAvailability(
    val canSend: Boolean,
    val totalUnits: Int,
    val totalCost: Long,
    val blockers:
        Set<SummonerSendBlocker>
)

object SummonerSendAvailabilityEvaluator {
    fun evaluate(
        draft: SummonerDraftState,
        queue: TroopSendQueue,
        senderTeam: TeamId,
        playerUuid: UUID,
        ledger: EconomyLedger,
        cooldowns:
            DeterministicCooldownTracker,
        gameTick: Long,
        definitions:
            MobDefinitionRepository =
            RecommendedMatureMobDefinitions
    ): SummonerSendAvailability {
        val attackedTeam=
            if(senderTeam==TeamId.RED)
                TeamId.BLUE
            else
                TeamId.RED
        check(
            queue.attackedTeam==
                attackedTeam
        ) {
            "Summoner queue belongs to wrong attacked team"
        }

        val lines=
            draft.snapshot()
        val totalUnits=
            lines.sumOf {
                it.quantity
            }
        val totalCost=
            lines.fold(0L) {
                acc,line ->
                val unitCost=
                    definitions
                        .get(
                            line.key.mobId
                        )
                        .level(
                            line.key.level
                        )
                        .sendCoins
                Math.addExact(
                    acc,
                    Math.multiplyExact(
                        unitCost,
                        line.quantity
                            .toLong()
                    )
                )
            }

        val blockers=
            linkedSetOf<
                SummonerSendBlocker
            >()
        if(lines.isEmpty()) {
            blockers +=
                SummonerSendBlocker
                    .EMPTY_DRAFT
        }

        if(
            !cooldowns.isReady(
                CooldownKey(
                    "sender:$playerUuid"
                ),
                gameTick
            )
        ) {
            blockers +=
                SummonerSendBlocker
                    .COOLDOWN_ACTIVE
        }

        if(
            totalUnits>0 &&
            queue.usedUnits()+
                totalUnits >
                queue.capacityUnits
        ) {
            blockers +=
                SummonerSendBlocker
                    .QUEUE_CAPACITY
        }

        val payer=
            EconomyAccount(
                senderTeam,
                playerUuid,
                EconomyCurrency
                    .MATCH_COINS
            )
        if(
            ledger.balance(payer)<
                totalCost
        ) {
            blockers +=
                SummonerSendBlocker
                    .INSUFFICIENT_COINS
        }

        return SummonerSendAvailability(
            canSend=blockers.isEmpty(),
            totalUnits=totalUnits,
            totalCost=totalCost,
            blockers=blockers
        )
    }
}
