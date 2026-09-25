package dev.cubecrafttd.mob

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.castle.CastleCombatService
import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.truth.ResolvedTruth
import java.util.UUID

fun interface MobMovementRateResolver {
    /**
     * Exact blocks/tick value is deliberately external because the recovered
     * `speedStat` is not yet proven to map 1:1 to world movement.
     */
    fun blocksPerTick(
        mobId: String,
        level: Int,
        formId: String
    ): ResolvedTruth<Double>
}

fun interface MobPositionUpdatePort {
    fun move(
        entityUuid: UUID,
        position: Vec3
    )
}

data class MobMovementTickMetrics(
    var movingVisited: Int = 0,
    var moved: Int = 0,
    var reachedCastle: Int = 0,
    var failedLiveUpdates: Int = 0
)

class MobMovementTickPhase(
    private val movementRate:
        MobMovementRateResolver,
    private val livePosition:
        MobPositionUpdatePort,
    private val giantRunSpeedMultiplier:
        ResolvedTruth<Double>? = null
) : ArenaTickPhase {
    override val order: Int = 40
    override val id: String = "mob-movement"

    private val metrics =
        MobMovementTickMetrics()

    override fun tick(
        context: ArenaContext
    ) {
        val castleCombat=
            CastleCombatService(
                context.castles
            )

        context.entityIndex.mobsByUuid
            .values
            .toList()
            .sortedBy {
                it.identity.instanceId.value
            }
            .forEach { mob ->
                if(
                    mob.combat.lifecycle !=
                        MobLifecycleState.MOVING
                ) return@forEach

                metrics.movingVisited++
                val route=
                    context.mapRuntime
                        .routesById[
                            mob.route.routeId
                        ] ?: error(
                            "Mob route missing"
                        )
                var distance=
                    movementRate.blocksPerTick(
                        mob.identity.mobId,
                        mob.identity.level,
                        mob.form.formId
                    ).value
                require(distance>=0.0)

                if(
                    mob.identity.mobId=="giant" &&
                    GiantPhaseService
                        .runState(
                            mob.combat.health,
                            mob.combat.maxHealth
                        ).running
                ) {
                    val multiplier=
                        giantRunSpeedMultiplier
                            ?: error(
                                "Giant run speed multiplier unresolved"
                            )
                    require(
                        multiplier.value>=1.0
                    )
                    distance *=
                        multiplier.value
                }

                val oldRoute=mob.route
                val result=
                    MobRouteMovementService
                        .advance(
                            mob,route,distance
                        )

                try {
                    livePosition.move(
                        mob.identity.entityUuid,
                        result.position
                    )
                } catch (t: Throwable) {
                    // Core and live entity must not diverge.
                    mob.route=oldRoute
                    metrics.failedLiveUpdates++
                    throw t
                }

                metrics.moved++
                if(result.reachedTerminal) {
                    castleCombat
                        .enterCastleAttackState(mob)
                    metrics.reachedCastle++
                }
            }
    }

    fun metricsSnapshot():
        MobMovementTickMetrics =
        metrics.copy()
}
