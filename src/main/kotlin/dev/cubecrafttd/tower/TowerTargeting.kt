package dev.cubecrafttd.tower

import dev.cubecrafttd.mob.*
import dev.cubecrafttd.arena.ArenaDeterministicRng
import java.util.UUID

enum class TargetPriorityPolicy {
    FIRST,
    LAST,
    RANDOM,
    EXPLICIT_ENGINEERING
}

data class TowerTargetCandidate(
    val entityUuid: UUID,
    val routeProgress: Double,
    val combatProfile: MobCombatProfile,
    val distanceBlocks: Double,
    val lineOfSight: Boolean
)

data class TowerTargetQuery(
    val capability: TowerAttackCapability,
    val rangeBlocks: Double,
    val requiresLineOfSight: Boolean,
    val priority: TargetPriorityPolicy
)

object TowerTargetSelector {
    /**
     * Candidate collection is already arena-local. This selector never scans Bukkit worlds/entities.
     */
    fun select(
        candidates: Collection<TowerTargetCandidate>,
        query: TowerTargetQuery,
        rng: ArenaDeterministicRng? = null
    ): TowerTargetCandidate? {
        val eligible = candidates.asSequence()
            .filter { it.distanceBlocks <= query.rangeBlocks }
            .filter { !query.requiresLineOfSight || it.lineOfSight }
            .filter {
                MobCombatEligibility.directTargetable(
                    it.combatProfile,
                    query.capability
                )
            }
            .toList()

        return when (query.priority) {
            TargetPriorityPolicy.FIRST ->
                eligible.maxWithOrNull(
                    compareBy<TowerTargetCandidate> { it.routeProgress }
                        .thenByDescending { it.entityUuid.toString() }
                )
            TargetPriorityPolicy.LAST ->
                eligible.minWithOrNull(
                    compareBy<TowerTargetCandidate> { it.routeProgress }
                        .thenBy { it.entityUuid.toString() }
                )
            TargetPriorityPolicy.RANDOM -> {
                if (eligible.isEmpty()) null
                else {
                    val arenaRng = rng
                        ?: error("RANDOM target policy requires ArenaDeterministicRng")
                    eligible[arenaRng.nextInt(eligible.size)]
                }
            }
            TargetPriorityPolicy.EXPLICIT_ENGINEERING ->
                eligible.firstOrNull()
        }
    }
}

data class TowerAttackSchedule(
    val intervalTicks: Long,
    var nextAttackTick: Long
)

object TowerAttackScheduleFactory {
    fun fromStage(
        stage: TowerPathDefinition,
        gameTick: Long
    ): TowerAttackSchedule {
        val seconds = stage.stats.attackIntervalSeconds
            ?: error(
                "Attack interval unresolved for stage ${stage.option} L${stage.level}; " +
                    "Truth/engineering fallback must resolve it before activation"
            )
        val ticks = (seconds * 20.0).toLong()
        require(ticks > 0L)
        return TowerAttackSchedule(ticks, gameTick)
    }
}
