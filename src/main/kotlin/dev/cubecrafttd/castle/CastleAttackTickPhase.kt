package dev.cubecrafttd.castle

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.mob.*

data class CastleAttackTickMetrics(
    var mobsObserved: Int = 0,
    var clocksStarted: Int = 0,
    var hitsApplied: Int = 0,
    var clocksCancelled: Int = 0
)

class CastleAttackTickPhase(
    config: CastleAttackResolvedConfig,
    private val definitions:
        MobDefinitionRepository =
        RecommendedMatureMobDefinitions
) : ArenaTickPhase {
    override val order: Int = 80
    override val id: String = "castle-attacks"

    private val clock =
        CastleAttackClock(
            CastleAttackClockConfig(
                config.firstHitDelayTicks,
                config.ordinaryAttackIntervalTicks
            )
        )

    private val metrics =
        CastleAttackTickMetrics()

    override fun tick(
        context: ArenaContext
    ) {
        context.entityIndex.mobsByUuid
            .values
            .toList()
            .sortedBy {
                it.identity.instanceId.value
            }
            .forEach { mob ->
                when (mob.combat.lifecycle) {
                    MobLifecycleState
                        .ATTACKING_CASTLE -> {
                        metrics.mobsObserved++
                        if (
                            !clock.has(
                                mob.identity.instanceId
                            )
                        ) {
                            clock.begin(
                                mob.identity.instanceId,
                                context.gameTick
                            )
                            metrics.clocksStarted++
                        }
                        if (
                            clock.consumeHit(
                                mob.identity.instanceId,
                                context.gameTick
                            )
                        ) {
                            val damage =
                                definitions
                                    .get(
                                        mob.identity
                                            .mobId
                                    )
                                    .level(
                                        mob.identity
                                            .level
                                    )
                                    .castleDamage
                            context.castles
                                .getValue(
                                    mob.identity
                                        .attackedTeam
                                )
                                .damage(damage)
                            metrics.hitsApplied++
                        }
                    }

                    MobLifecycleState.DEAD,
                    MobLifecycleState.REMOVED -> {
                        if (
                            clock.has(
                                mob.identity.instanceId
                            )
                        ) {
                            clock.cancel(
                                mob.identity.instanceId
                            )
                            metrics.clocksCancelled++
                        }
                    }

                    else -> Unit
                }
            }
    }

    fun metricsSnapshot():
        CastleAttackTickMetrics =
        metrics.copy()
}
