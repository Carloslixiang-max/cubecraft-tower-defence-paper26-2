package dev.cubecrafttd.economy

import dev.cubecrafttd.arena.ArenaContext
import dev.cubecrafttd.combat.KillAttributionService
import dev.cubecrafttd.mob.MobDeathFinalizationPort
import dev.cubecrafttd.mob.MobRuntimeState
import dev.cubecrafttd.mob.MobDefinitionRepository
import dev.cubecrafttd.mob.RecommendedMatureMobDefinitions
import dev.cubecrafttd.stats.MatchStatsRecorder
import java.util.concurrent.atomic.AtomicLong

/**
 * Final live death settlement.
 *
 * Direct weapon hits may already have applied the same mob-kill correlation;
 * MobKillRewardService is idempotent, so this phase safely fills gaps for
 * tower DoT and AoE final blows without double-paying direct kills.
 */
class MatchMobDeathFinalizer(
    private val sentExp:
        PlayerSentMobDeathFinalizer,
    private val killRewards:
        MobKillRewardService,
    private val stats:
        MatchStatsRecorder,
    private val nextTransactionId:
        AtomicLong,
    private val definitions:
        MobDefinitionRepository =
        RecommendedMatureMobDefinitions
) : MobDeathFinalizationPort {
    private val statsFinalizedMobIds=
        linkedSetOf<Long>()

    override fun finalize(
        context: ArenaContext,
        mob: MobRuntimeState
    ) {
        sentExp.finalize(
            context,
            mob
        )

        if(mob.identity.mobId=="wither") {
            return
        }

        val attribution=
            KillAttributionService
                .matureFinalBlow(
                    mob.combat
                        .lastEligibleDamageSource
                )
        val player=
            attribution
                .creditedPlayerUuid
        val awarded=
            killRewards.award(
                mob,
                attribution,
                context.gameTick,
                nextTransactionId
                    .getAndIncrement()
            )

        if(
            player!=null &&
            statsFinalizedMobIds.add(
                mob.identity.instanceId.value
            )
        ) {
            val killValue=
                definitions
                    .get(
                        mob.identity.mobId
                    )
                    .level(
                        mob.identity.level
                    )
                    .sendCoins
            stats.recordTroopKill(
                player,
                killValue
            )
        }

        if(
            player!=null &&
            awarded>0L
        ) {
            stats.recordCoinsEarned(
                player,
                awarded
            )
        }
    }
}
