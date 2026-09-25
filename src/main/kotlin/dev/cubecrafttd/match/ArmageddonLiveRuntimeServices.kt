package dev.cubecrafttd.match

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.tower.TowerInstanceId
import dev.cubecrafttd.troop.*
import dev.cubecrafttd.truth.ResolvedTruth
import java.util.UUID

fun interface ArmageddonTowerDestroyPort {
    fun destroy(
        towerId: TowerInstanceId
    ): Boolean
}

data class LightningRuntimeMetrics(
    var strikes: Int = 0,
    var towersSelected: Int = 0,
    var towersDestroyed: Int = 0,
    var destroyFailures: Int = 0
)

class LightningArmageddonTickRuntime(
    config: LightningArmageddonConfig,
    activationTick: Long,
    firstStrikeDelayTicks:
        ResolvedTruth<Long>,
    private val destroyPort:
        ArmageddonTowerDestroyPort
) {
    private val engine=
        LightningArmageddonEngine(
            config,
            Math.addExact(
                activationTick,
                firstStrikeDelayTicks.value
            )
        )
    private val metrics=
        LightningRuntimeMetrics()

    fun tick(
        context: ArenaContext
    ) {
        val selection=
            engine.tick(context)
                ?: return
        metrics.strikes++
        metrics.towersSelected +=
            selection.towerIds.size

        selection.towerIds.forEach {
            id ->
            val ok=runCatching {
                destroyPort.destroy(
                    TowerInstanceId(id)
                )
            }.getOrDefault(false)
            if(ok) metrics.towersDestroyed++
            else metrics.destroyFailures++
        }
    }

    fun metricsSnapshot():
        LightningRuntimeMetrics =
        metrics.copy()
}

val ARMAGEDDON_SYSTEM_SENDER_UUID:
    UUID =
    UUID.fromString(
        "00000000-0000-0000-0000-00000000a11d"
    )

data class HordeRuntimeMetrics(
    var wavesEmitted: Int = 0,
    var mobsSpawned: Int = 0
)

class HordeArmageddonTickRuntime(
    config: HordeArmageddonConfig,
    activationTick: Long,
    firstWaveDelayTicks:
        ResolvedTruth<Long>,
    private val routeAssignment:
        RouteAssignmentPolicy,
    private val entitySpawn:
        MobEntitySpawnPort,
    private val idAllocator:
        MobInstanceIdAllocator =
        MobInstanceIdAllocator(
            1_000_000L
        ),
    private val definitions:
        MobDefinitionRepository =
        RecommendedMatureMobDefinitions
) {
    private val engine=
        HordeArmageddonEngine(
            config,
            Math.addExact(
                activationTick,
                firstWaveDelayTicks.value
            )
        )
    private val metrics=
        HordeRuntimeMetrics()

    fun tick(
        context: ArenaContext
    ) {
        val requests=
            engine.tick(
                context.gameTick
            )
        if(requests.isEmpty()) return

        val waveCount=
            requests.map {
                it.waveIndex
            }.distinct().size
        metrics.wavesEmitted +=
            waveCount

        requests.forEach {
            request ->
            repeat(request.quantity) {
                spawnOne(
                    context,request
                )
                metrics.mobsSpawned++
            }
        }
    }

    private fun spawnOne(
        context: ArenaContext,
        request: HordeSpawnRequest
    ) {
        val instanceId=
            idAllocator.next()
        val routeId=
            routeAssignment.chooseRoute(
                context.mapRuntime,
                request.attackedTeam,
                instanceId
            )
        check(
            context.mapRuntime
                .routeOwnerById[routeId] ==
                request.attackedTeam
        )

        val route=
            context.mapRuntime
                .routesById[routeId]
                ?: error(
                    "Armageddon route missing: $routeId"
                )
        val level=
            definitions
                .get(request.mobId)
                .level(request.level)

        val entityUuid=
            entitySpawn.spawn(
                MobEntitySpawnRequest(
                    instanceId=instanceId,
                    senderPlayerUuid=
                        ARMAGEDDON_SYSTEM_SENDER_UUID,
                    attackedTeam=
                        request.attackedTeam,
                    mobId=request.mobId,
                    level=request.level,
                    routeId=routeId,
                    spawnPosition=
                        route.nodes.first()
                )
            )

        context.entityIndex
            .registerMob(
                MobRuntimeState(
                    identity=
                        MobIdentity(
                            instanceId,
                            entityUuid,
                            request.mobId,
                            ARMAGEDDON_SYSTEM_SENDER_UUID,
                            request.attackedTeam,
                            request.level
                        ),
                    route=
                        MobRouteState(
                            routeId,0,0.0,0.0
                        ),
                    combat=
                        MobCombatState(
                            level.health,
                            level.health,
                            MobLifecycleState
                                .MOVING
                        )
                )
            )
    }

    fun metricsSnapshot():
        HordeRuntimeMetrics =
        metrics.copy()

    fun finished(): Boolean =
        engine.finished()
}

data class WitherTowerSelection(
    val towerId: TowerInstanceId,
    val distanceBlocks: Double
)

object WitherTowerTargetSelector {
    fun select(
        context: ArenaContext,
        attackedTeam: TeamId,
        bossPosition: dev.cubecrafttd.map.Vec3,
        rangeBlocks: Double,
        policy: WitherTowerTargetPolicy
    ): WitherTowerSelection? {
        require(rangeBlocks>=0.0)

        val eligible=
            context.entityIndex
                .towersByTeam
                .getValue(attackedTeam)
                .mapNotNull { id ->
                    val tower=
                        context.entityIndex
                            .towersByInstanceId[id]
                            ?: return@mapNotNull null
                    if(
                        tower.lifecycle !=
                            dev.cubecrafttd.tower
                                .TowerLifecycleState.ACTIVE
                    ) {
                        return@mapNotNull null
                    }

                    val distance=
                        tower.geometry.baseOrigin
                            .distanceTo(
                                bossPosition
                            )
                    if(distance<=rangeBlocks)
                        WitherTowerSelection(
                            id,distance
                        )
                    else null
                }

        if(eligible.isEmpty())
            return null

        return when(policy) {
            WitherTowerTargetPolicy
                .NEAREST_IN_RANGE_ENGINEERING ->
                eligible.minWithOrNull(
                    compareBy<
                        WitherTowerSelection
                    > {
                        it.distanceBlocks
                    }.thenBy {
                        it.towerId.value
                    }
                )

            WitherTowerTargetPolicy
                .RANDOM_IN_RANGE_ENGINEERING ->
                eligible[
                    context.rng.nextInt(
                        eligible.size
                    )
                ]
        }
    }
}

data class WitherBossRuntimeHandle(
    val attackedTeam: TeamId,
    val mobInstanceId: MobInstanceId,
    val entityUuid: UUID,
    val routeId: String,
    val state: WitherArmageddonState
)

data class WitherRuntimeMetrics(
    var bossesSpawned: Int = 0,
    var bossesDefeated: Int = 0,
    var towerSkullEvents: Int = 0,
    var towersDestroyed: Int = 0,
    var towerSkullsWithoutTarget: Int = 0,
    var trailEvents: Int = 0,
    var trailSkeletonsSpawned: Int = 0,
    var castlesReached: Int = 0
)

/**
 * Executable Armageddon boss runtime.
 *
 * The boss is registered as an arena mob with lifecycle ARMAGEDDON_BOSS so
 * tower/player damage, Witch healing, AoE immunity and cleanup share the same
 * combat state. Movement remains boss-owned and never enters the ordinary mob
 * movement phase.
 */
class WitherArmageddonTickRuntime(
    private val config: WitherArmageddonConfig,
    private val activationTick: Long,
    private val routeAssignment:
        RouteAssignmentPolicy,
    private val entitySpawn:
        MobEntitySpawnPort,
    private val livePosition:
        MobPositionUpdatePort,
    private val destroyPort:
        ArmageddonTowerDestroyPort,
    private val bossIdAllocator:
        MobInstanceIdAllocator =
        MobInstanceIdAllocator(
            2_000_000L
        ),
    private val trailIdAllocator:
        MobInstanceIdAllocator =
        MobInstanceIdAllocator(
            3_000_000L
        ),
    private val definitions:
        MobDefinitionRepository =
        RecommendedMatureMobDefinitions
) {
    private val engine=
        WitherArmageddonEngine(config)
    private val bosses=
        linkedMapOf<
            TeamId,
            WitherBossRuntimeHandle
        >()
    private val defeatedTeams=
        linkedSetOf<TeamId>()
    private val metrics=
        WitherRuntimeMetrics()

    init {
        require(activationTick>=0L)
    }

    fun start(
        context: ArenaContext
    ) {
        check(bosses.isEmpty()) {
            "Wither runtime already started"
        }

        TeamId.entries.forEach {
            attackedTeam ->
            val instanceId=
                bossIdAllocator.next()
            val routeId=
                routeAssignment.chooseRoute(
                    context.mapRuntime,
                    attackedTeam,
                    instanceId
                )
            check(
                context.mapRuntime
                    .routeOwnerById[
                        routeId
                    ]==attackedTeam
            ) {
                "Wither route $routeId belongs to wrong team"
            }

            val route=
                context.mapRuntime
                    .routesById[routeId]
                    ?: error(
                        "Wither route missing: $routeId"
                    )
            val spawnPosition=
                route.nodes.first()
            val entityUuid=
                entitySpawn.spawn(
                    MobEntitySpawnRequest(
                        instanceId=
                            instanceId,
                        senderPlayerUuid=
                            ARMAGEDDON_SYSTEM_SENDER_UUID,
                        attackedTeam=
                            attackedTeam,
                        mobId="wither",
                        level=1,
                        routeId=routeId,
                        spawnPosition=
                            spawnPosition
                    )
                )

            val mob=
                MobRuntimeState(
                    identity=
                        MobIdentity(
                            instanceId,
                            entityUuid,
                            "wither",
                            ARMAGEDDON_SYSTEM_SENDER_UUID,
                            attackedTeam,
                            1
                        ),
                    route=
                        MobRouteState(
                            routeId,
                            0,0.0,0.0
                        ),
                    combat=
                        MobCombatState(
                            RECOMMENDED_WITHER_MAX_HEALTH,
                            RECOMMENDED_WITHER_MAX_HEALTH,
                            MobLifecycleState
                                .ARMAGEDDON_BOSS
                        ),
                    form=
                        MobFormState(
                            "wither",
                            transitionReason=
                                MobFormTransitionReason
                                    .ARMAGEDDON
                        )
                )
            context.entityIndex
                .registerMob(mob)

            bosses[attackedTeam]=
                WitherBossRuntimeHandle(
                    attackedTeam,
                    instanceId,
                    entityUuid,
                    routeId,
                    WitherArmageddonState(
                        attackedTeam=
                            attackedTeam,
                        health=
                            RECOMMENDED_WITHER_MAX_HEALTH,
                        routeProgress=0.0,
                        nextTowerSkullTick=
                            Math.addExact(
                                activationTick,
                                config
                                    .firstTowerSkullDelayTicks
                                    .value
                            ),
                        nextTrailSpawnTick=
                            Math.addExact(
                                activationTick,
                                config
                                    .firstTrailWitherSkeletonDelayTicks
                                    .value
                            )
                    )
                )
            metrics.bossesSpawned++
        }
    }

    fun tick(
        context: ArenaContext
    ) {
        bosses.values
            .sortedBy {
                it.attackedTeam.name
            }
            .forEach {
                handle ->
                tickBoss(
                    context,handle
                )
            }
    }

    private fun tickBoss(
        context: ArenaContext,
        handle:
            WitherBossRuntimeHandle
    ) {
        if(
            handle.attackedTeam in
                defeatedTeams
        ) return

        val mob=
            context.entityIndex
                .mobsByUuid[
                    handle.entityUuid
                ]

        if(
            mob==null ||
            mob.combat.lifecycle==
                MobLifecycleState.DEAD ||
            mob.combat.lifecycle==
                MobLifecycleState.REMOVED
        ) {
            defeatedTeams +=
                handle.attackedTeam
            metrics.bossesDefeated++
            return
        }

        check(
            mob.combat.lifecycle==
                MobLifecycleState
                    .ARMAGEDDON_BOSS
        )

        // Tower/player/Witch damage/heal is authoritative in MobCombatState.
        handle.state.health=
            mob.combat.health

        val route=
            context.mapRuntime
                .routesById[
                    handle.routeId
                ] ?: error(
                    "Wither route vanished: ${handle.routeId}"
                )

        val events=
            engine.tick(
                handle.state,
                context.gameTick,
                route.totalLength,
                context.castles.getValue(
                    handle.attackedTeam
                )
            )

        mob.combat.health=
            handle.state.health

        val sample=
            RouteProgressSampler.sample(
                route,
                handle.state.routeProgress
            )
        mob.route=
            sample.routeState
        livePosition.move(
            handle.entityUuid,
            sample.position
        )

        if(events.fireTowerSkull) {
            metrics.towerSkullEvents++
            val selected=
                WitherTowerTargetSelector
                    .select(
                        context,
                        handle.attackedTeam,
                        sample.position,
                        config
                            .towerSkullRangeBlocks
                            .value,
                        config.towerTargetPolicy
                            .value
                    )
            if(selected==null) {
                metrics
                    .towerSkullsWithoutTarget++
            } else if(
                destroyPort.destroy(
                    selected.towerId
                )
            ) {
                metrics.towersDestroyed++
            }
        }

        if(
            events
                .spawnWitherSkeletonTrail
        ) {
            metrics.trailEvents++
            repeat(
                config
                    .trailWitherSkeletonsPerEvent
                    .value
            ) {
                spawnTrailSkeleton(
                    context,
                    handle,
                    sample
                )
                metrics
                    .trailSkeletonsSpawned++
            }
        }

        if(
            events.reachedCastleThisTick
        ) {
            metrics.castlesReached++
        }
    }

    private fun spawnTrailSkeleton(
        context: ArenaContext,
        boss: WitherBossRuntimeHandle,
        sample:
            RouteProgressSample
    ) {
        val level=
            config
                .trailWitherSkeletonLevel
                .value
        val definition=
            definitions
                .get("skeleton")
                .level(level)
        val instanceId=
            trailIdAllocator.next()

        val entityUuid=
            entitySpawn.spawn(
                MobEntitySpawnRequest(
                    instanceId=
                        instanceId,
                    senderPlayerUuid=
                        ARMAGEDDON_SYSTEM_SENDER_UUID,
                    attackedTeam=
                        boss.attackedTeam,
                    mobId="skeleton",
                    level=level,
                    routeId=
                        boss.routeId,
                    spawnPosition=
                        sample.position
                )
            )

        context.entityIndex
            .registerMob(
                MobRuntimeState(
                    identity=
                        MobIdentity(
                            instanceId,
                            entityUuid,
                            "skeleton",
                            ARMAGEDDON_SYSTEM_SENDER_UUID,
                            boss.attackedTeam,
                            level
                        ),
                    route=
                        sample.routeState,
                    combat=
                        MobCombatState(
                            definition.health,
                            definition.health,
                            MobLifecycleState
                                .MOVING
                        )
                )
            )
    }

    fun activeBosses():
        List<WitherBossRuntimeHandle> =
        bosses.values
            .filter {
                it.attackedTeam !in
                    defeatedTeams
            }
            .sortedBy {
                it.attackedTeam.name
            }

    fun metricsSnapshot():
        WitherRuntimeMetrics =
        metrics.copy()
}

