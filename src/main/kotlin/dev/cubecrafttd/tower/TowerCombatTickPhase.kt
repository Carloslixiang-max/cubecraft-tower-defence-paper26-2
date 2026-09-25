package dev.cubecrafttd.tower

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.combat.AttackBatchApplier
import dev.cubecrafttd.mob.RecommendedMatureMobCombatProfiles
import java.util.UUID

data class MobGeometryView(
    val distanceBlocks: Double,
    val lineOfSight: Boolean
)

fun interface MobGeometryProvider {
    fun geometry(
        tower: TowerRuntimeState,
        mobUuid: UUID
    ): MobGeometryView?
}

class ArenaLocalTowerCandidateProvider(
    private val geometryProvider: MobGeometryProvider
) {
    fun candidates(
        context: ArenaContext,
        tower: TowerRuntimeState
    ): List<TowerTargetCandidate> {
        val uuids = context.entityIndex
            .mobsByDefendingTeam
            .getValue(tower.identity.team)

        return uuids.mapNotNull { uuid ->
            val mob = context.entityIndex.mobsByUuid[uuid]
                ?: return@mapNotNull null
            val geometry = geometryProvider
                .geometry(tower,uuid)
                ?: return@mapNotNull null
            val profile =
                RecommendedMatureMobCombatProfiles
                    .get(mob.form.formId)

            TowerTargetCandidate(
                entityUuid = uuid,
                routeProgress = mob.route.routeProgress,
                combatProfile = profile,
                distanceBlocks = geometry.distanceBlocks,
                lineOfSight = geometry.lineOfSight
            )
        }
    }
}

fun interface TowerRuntimeConfigProvider {
    fun config(
        context: ArenaContext,
        tower: TowerRuntimeState,
        engine: TowerControllerEngine
    ): TowerAttackRuntimeConfig
}

fun interface TowerAttackPlannerProvider {
    fun planner(
        context: ArenaContext,
        tower: TowerRuntimeState
    ): TowerAttackPlanner
}

data class TowerCombatTickMetrics(
    var towersVisited: Int = 0,
    var candidatesBuilt: Int = 0,
    var attacksFired: Int = 0,
    var noTarget: Int = 0,
    var cooldown: Int = 0,
    var inactive: Int = 0
)

class TowerCombatTickPhase(
    private val geometryProvider: MobGeometryProvider,
    private val configProvider: TowerRuntimeConfigProvider,
    private val plannerProvider: TowerAttackPlannerProvider,
    private val lethalResolver:
        dev.cubecrafttd.mob.MobLethalHitResolver =
        dev.cubecrafttd.mob.MobLethalHitResolver(),
    private val metrics: TowerCombatTickMetrics =
        TowerCombatTickMetrics()
) : ArenaTickPhase {
    override val order: Int = 60
    override val id: String = "tower-combat"

    override fun tick(context: ArenaContext) {
        val candidateProvider =
            ArenaLocalTowerCandidateProvider(
                geometryProvider
            )
        val engine = TowerControllerEngine(
            context.rng,
            AttackBatchApplier(
                context.entityIndex,
                lethalResolver=lethalResolver
            )
        )

        // Snapshot IDs so sell/remove during another arena-owned phase cannot
        // invalidate iteration order.
        val towerIds = context.entityIndex
            .towersByInstanceId
            .keys
            .sortedBy { it.value }

        for (id in towerIds) {
            val tower = context.entityIndex
                .towersByInstanceId[id]
                ?: continue
            metrics.towersVisited++

            val candidates = candidateProvider
                .candidates(context,tower)
            metrics.candidatesBuilt += candidates.size

            val report = engine.tryAttack(
                tower = tower,
                gameTick = context.gameTick,
                candidates = candidates,
                config = configProvider.config(
                    context,tower,engine
                ),
                planner = plannerProvider.planner(
                    context,tower
                )
            )

            when (report.status) {
                TowerAttackCycleStatus.FIRED ->
                    metrics.attacksFired++
                TowerAttackCycleStatus.NO_TARGET ->
                    metrics.noTarget++
                TowerAttackCycleStatus.COOLDOWN ->
                    metrics.cooldown++
                TowerAttackCycleStatus.INACTIVE ->
                    metrics.inactive++
            }
        }
    }

    fun metricsSnapshot(): TowerCombatTickMetrics =
        metrics.copy()
}
