package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.tower.*
import org.bukkit.Server
import java.util.UUID
import kotlin.math.sqrt

class BukkitMobSupportGeometryProvider(
    private val server: Server
) : MobSupportGeometryProvider {
    override fun distanceBlocks(
        fromEntityUuid: UUID,
        toEntityUuid: UUID
    ): Double? {
        val a=server.getEntity(fromEntityUuid)
            ?: return null
        val b=server.getEntity(toEntityUuid)
            ?: return null
        if(a.world.uid!=b.world.uid) return null
        return a.location.distance(
            b.location
        )
    }
}

class BukkitAreaTowerTargetResolver(
    private val server: Server
) : AreaTowerTargetResolver {
    override fun affectedTargets(
        context: ArenaContext,
        tower: TowerRuntimeState,
        primaryTargetUuid: UUID,
        radiusBlocks: Double
    ): List<UUID> {
        require(radiusBlocks>=0.0)
        val primary=server.getEntity(
            primaryTargetUuid
        ) ?: return emptyList()

        return context.entityIndex
            .mobsByDefendingTeam
            .getValue(tower.identity.team)
            .asSequence()
            .mapNotNull { uuid ->
                val state=
                    context.entityIndex
                        .mobsByUuid[uuid]
                        ?: return@mapNotNull null
                if(
                    state.combat.lifecycle !=
                        MobLifecycleState.MOVING &&
                    state.combat.lifecycle !=
                        MobLifecycleState
                            .ATTACKING_CASTLE
                ) return@mapNotNull null

                val entity=
                    server.getEntity(uuid)
                        ?: return@mapNotNull null
                if(
                    entity.world.uid !=
                        primary.world.uid
                ) return@mapNotNull null
                val distance=
                    entity.location.distance(
                        primary.location
                    )
                if(distance<=radiusBlocks)
                    uuid to distance
                else null
            }
            .sortedWith(
                compareBy<Pair<UUID,Double>> {
                    it.second
                }.thenBy {
                    it.first.toString()
                }
            )
            .map { it.first }
            .toList()
    }
}

class BukkitPolicyChainTargetResolver(
    private val server: Server,
    private val policy:
        TowerChainTargetPolicy,
    private val towerGeometry:
        MobGeometryProvider,
    private val definitions:
        TowerDefinitionRepository =
        RecommendedMatureTowerDefinitions
) : ChainTowerTargetResolver {
    override fun orderedNextTargets(
        context: ArenaContext,
        tower: TowerRuntimeState,
        primaryTargetUuid: UUID,
        maxAdditionalTargets: Int
    ): List<UUID> {
        require(maxAdditionalTargets>=0)
        if(maxAdditionalTargets==0)
            return emptyList()

        val primaryState=
            context.entityIndex
                .mobsByUuid[
                    primaryTargetUuid
                ] ?: return emptyList()

        val path=tower.upgrade.path
            ?: error("Tower path unresolved")
        val stage=
            TowerStageResolver.resolve(
                definitions.get(
                    tower.identity.towerId
                ),
                path,
                tower.upgrade.level
            )
        val range=
            stage.stats.rangeBlocks
                ?: error(
                    "Tower range unresolved"
                )

        val eligible=
            context.entityIndex
                .mobsByDefendingTeam
                .getValue(
                    tower.identity.team
                )
                .asSequence()
                .filter {
                    it != primaryTargetUuid
                }
                .mapNotNull { uuid ->
                    val state=
                        context.entityIndex
                            .mobsByUuid[uuid]
                            ?: return@mapNotNull null
                    if(
                        state.combat.lifecycle !=
                            MobLifecycleState.MOVING &&
                        state.combat.lifecycle !=
                            MobLifecycleState
                                .ATTACKING_CASTLE
                    ) return@mapNotNull null

                    val g=towerGeometry
                        .geometry(
                            tower,uuid
                        ) ?: return@mapNotNull null
                    if(g.distanceBlocks>range)
                        null
                    else state
                }
                .toList()

        return when(policy) {
            TowerChainTargetPolicy
                .ROUTE_PROGRESS_FORWARD_ENGINEERING ->
                eligible
                    .filter {
                        it.route.routeProgress >
                            primaryState.route
                                .routeProgress
                    }
                    .sortedWith(
                        compareBy<
                            MobRuntimeState
                        > {
                            it.route.routeProgress -
                                primaryState.route
                                    .routeProgress
                        }.thenBy {
                            it.identity.entityUuid
                                .toString()
                        }
                    )
                    .take(maxAdditionalTargets)
                    .map {
                        it.identity.entityUuid
                    }

            TowerChainTargetPolicy
                .NEAREST_FROM_PREVIOUS_ENGINEERING ->
                nearestChain(
                    primaryTargetUuid,
                    eligible,
                    maxAdditionalTargets
                )
        }
    }

    private fun nearestChain(
        primaryTargetUuid: UUID,
        candidates:
            List<MobRuntimeState>,
        maxAdditionalTargets: Int
    ): List<UUID> {
        var previous=
            server.getEntity(
                primaryTargetUuid
            ) ?: return emptyList()
        val remaining=
            candidates.toMutableList()
        val out=mutableListOf<UUID>()

        repeat(maxAdditionalTargets) {
            val next=
                remaining
                    .mapNotNull { state ->
                        val entity=
                            server.getEntity(
                                state.identity
                                    .entityUuid
                            ) ?: return@mapNotNull null
                        if(
                            entity.world.uid !=
                                previous.world.uid
                        ) return@mapNotNull null
                        state to
                            entity.location
                                .distance(
                                    previous.location
                                )
                    }
                    .minWithOrNull(
                        compareBy<
                            Pair<
                                MobRuntimeState,
                                Double
                            >
                        > {
                            it.second
                        }.thenBy {
                            it.first.identity
                                .entityUuid
                                .toString()
                        }
                    )
                    ?: return@repeat

            remaining.remove(next.first)
            out +=
                next.first.identity
                    .entityUuid
            previous=
                server.getEntity(
                    next.first.identity
                        .entityUuid
                ) ?: return@repeat
        }

        return out
    }
}
