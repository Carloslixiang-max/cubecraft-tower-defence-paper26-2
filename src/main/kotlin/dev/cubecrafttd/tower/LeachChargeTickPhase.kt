package dev.cubecrafttd.tower

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.combat.AttackBatchApplier
import dev.cubecrafttd.mob.MobLethalHitResolver

data class LeachChargeTickMetrics(
    var towersVisited: Int = 0,
    var eligibleTargetTicks: Int = 0,
    var beamsFired: Int = 0,
    var beamTargets: Int = 0
)

class LeachChargeTickPhase(
    private val geometry:
        MobGeometryProvider,
    config: LeachChargeConfig,
    private val lethalResolver:
        MobLethalHitResolver,
    private val definitions:
        TowerDefinitionRepository =
        RecommendedMatureTowerDefinitions
) : ArenaTickPhase {
    override val order: Int =
        NormalArenaPhaseOrder.LEACH_CHARGE
    override val id: String =
        "leach-charge"

    private val service=
        LeachChargeService(config)
    private val states=
        linkedMapOf<
            TowerInstanceId,
            LeachChargeState
        >()
    private val metrics=
        LeachChargeTickMetrics()

    override fun tick(
        context: ArenaContext
    ) {
        val activeLeachIds=
            context.entityIndex
                .towersByInstanceId
                .values
                .filter {
                    it.identity.towerId ==
                        "leach" &&
                    it.lifecycle ==
                        TowerLifecycleState.ACTIVE
                }
                .mapTo(linkedSetOf()) {
                    it.identity.instanceId
                }

        states.keys.removeIf {
            it !in activeLeachIds
        }

        activeLeachIds
            .sortedBy { it.value }
            .forEach { id ->
                val tower=
                    context.entityIndex
                        .towersByInstanceId
                        .getValue(id)
                metrics.towersVisited++

                val path=
                    tower.upgrade.path
                        ?: error(
                            "Leach path unresolved"
                        )
                val stage=
                    TowerStageResolver.resolve(
                        definitions.get("leach"),
                        path,
                        tower.upgrade.level
                    )
                val range=
                    stage.stats.rangeBlocks
                        ?: error(
                            "Leach range unresolved"
                        )

                val targets=
                    context.entityIndex
                        .mobsByDefendingTeam
                        .getValue(
                            tower.identity.team
                        )
                        .mapNotNull { uuid ->
                            val mob=
                                context.entityIndex
                                    .mobsByUuid[uuid]
                                    ?: return@mapNotNull null
                            if(
                                mob.combat.lifecycle !=
                                    dev.cubecrafttd.mob
                                        .MobLifecycleState.MOVING &&
                                mob.combat.lifecycle !=
                                    dev.cubecrafttd.mob
                                        .MobLifecycleState
                                        .ATTACKING_CASTLE
                            ) {
                                return@mapNotNull null
                            }
                            val g=
                                geometry.geometry(
                                    tower,uuid
                                ) ?: return@mapNotNull null
                            if(
                                g.distanceBlocks >
                                    range ||
                                !g.lineOfSight
                            ) {
                                return@mapNotNull null
                            }
                            mob
                        }
                        .sortedWith(
                            compareByDescending<
                                dev.cubecrafttd.mob
                                    .MobRuntimeState
                            > {
                                it.route.routeProgress
                            }.thenBy {
                                it.identity.entityUuid
                                    .toString()
                            }
                        )

                metrics.eligibleTargetTicks +=
                    targets.size

                val state=
                    states.getOrPut(id) {
                        LeachChargeState()
                    }
                service.addEligibleTargets(
                    state,
                    targets.size
                )

                if(
                    targets.isNotEmpty() &&
                    service.consume(state)
                ) {
                    val ctx=
                        TowerAttackContext(
                            tower.identity
                                .instanceId.value,
                            tower.identity
                                .ownerUuid,
                            context.gameTick
                        )
                    val batch=
                        SpecialTowerProcessors
                            .leachBeam(
                                stage,
                                targets.map {
                                    it.identity
                                        .entityUuid
                                },
                                ctx
                            )
                    AttackBatchApplier(
                        context.entityIndex,
                        lethalResolver=
                            lethalResolver
                    ).apply(batch)
                    metrics.beamsFired++
                    metrics.beamTargets +=
                        batch.impacts.size
                }
            }
    }

    fun charge(
        towerId: TowerInstanceId
    ): Double =
        states[towerId]?.charge ?: 0.0

    fun metricsSnapshot():
        LeachChargeTickMetrics =
        metrics.copy()
}
