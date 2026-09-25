package dev.cubecrafttd.troop

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.mob.*
import java.util.UUID

class MobInstanceIdAllocator(
    startInclusive: Long = 1L
) {
    private var next = startInclusive

    fun next(): MobInstanceId =
        MobInstanceId(next++)
}

data class MobEntitySpawnRequest(
    val instanceId: MobInstanceId,
    val senderPlayerUuid: UUID,
    val attackedTeam: TeamId,
    val mobId: String,
    val level: Int,
    val routeId: String,
    val spawnPosition: Vec3
)

fun interface MobEntitySpawnPort {
    /**
     * Creates/binds the live entity and returns its UUID.
     * Core creates MobRuntimeState only after this succeeds.
     */
    fun spawn(
        request: MobEntitySpawnRequest
    ): UUID
}

data class TroopSpawnTickMetrics(
    var spawnAttempts: Int = 0,
    var successfulSpawns: Int = 0,
    var emptyQueues: Int = 0
)

class TroopSpawnTickPhase(
    private val runtime:
        NormalArenaRuntimeState,
    private val routeAssignment:
        RouteAssignmentPolicy,
    private val entitySpawn:
        MobEntitySpawnPort,
    private val idAllocator:
        MobInstanceIdAllocator =
        MobInstanceIdAllocator(),
    private val definitions:
        MobDefinitionRepository =
        RecommendedMatureMobDefinitions
) : ArenaTickPhase {
    override val order: Int = 30
    override val id: String = "troop-spawn"

    private val metrics =
        TroopSpawnTickMetrics()

    override fun tick(
        context: ArenaContext
    ) {
        TeamId.entries.forEach { attackedTeam ->
            val queue=
                runtime.queues
                    .getValue(attackedTeam)
            val clock=
                runtime.spawnClocks
                    .getValue(attackedTeam)
            val head=queue.peek()
            if(head==null) {
                metrics.emptyQueues++
                clock.disarm()
                return@forEach
            }

            clock.arm(context.gameTick)
            if(!clock.isDue(context.gameTick)) {
                return@forEach
            }

            metrics.spawnAttempts++
            val instanceId=idAllocator.next()
            val routeId=
                routeAssignment.chooseRoute(
                    context.mapRuntime,
                    attackedTeam,
                    instanceId
                )
            val route=
                context.mapRuntime
                    .routesById[routeId]
                    ?: error(
                        "Assigned route missing: $routeId"
                    )
            check(
                context.mapRuntime
                    .routeOwnerById[routeId] ==
                    attackedTeam
            ) {
                "Assigned route belongs to wrong team"
            }

            val definition=
                definitions.get(head.mobId)
                    .level(head.level)

            val entityUuid=
                entitySpawn.spawn(
                    MobEntitySpawnRequest(
                        instanceId,
                        head.senderPlayerUuid,
                        attackedTeam,
                        head.mobId,
                        head.level,
                        routeId,
                        route.nodes.first()
                    )
                )

            val mob=MobRuntimeState(
                identity=MobIdentity(
                    instanceId,
                    entityUuid,
                    head.mobId,
                    head.senderPlayerUuid,
                    attackedTeam,
                    head.level
                ),
                route=MobRouteState(
                    routeId,0,0.0,0.0
                ),
                combat=MobCombatState(
                    definition.health,
                    definition.health,
                    MobLifecycleState.MOVING
                )
            )

            context.entityIndex.registerMob(mob)
            queue.recordOneSpawn(head.entryId)
            check(clock.consume(context.gameTick))
            metrics.successfulSpawns++
        }
    }

    fun metricsSnapshot():
        TroopSpawnTickMetrics =
        metrics.copy()
}
