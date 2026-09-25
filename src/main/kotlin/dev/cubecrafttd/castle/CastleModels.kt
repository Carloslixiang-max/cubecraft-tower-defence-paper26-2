package dev.cubecrafttd.castle

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.truth.ResolvedTruth
import java.util.UUID

const val RECOMMENDED_MATURE_CASTLE_START_HP: Double = 1000.0
const val RECOMMENDED_MATURE_GUARD_COUNT_PER_CASTLE: Int = 2

enum class CastleState { ACTIVE, DESTROYED }

data class CastleRuntime(
    val team: TeamId,
    val maxHealth: Double = RECOMMENDED_MATURE_CASTLE_START_HP,
    var health: Double = maxHealth,
    var state: CastleState = CastleState.ACTIVE
) {
    init {
        require(maxHealth > 0)
        require(health >= 0.0 && health <= maxHealth)
    }

    fun damage(amount: Double): Double {
        require(amount >= 0.0)
        if (state == CastleState.DESTROYED) return health
        health = (health - amount).coerceAtLeast(0.0)
        if (health <= 0.0) state = CastleState.DESTROYED
        return health
    }
}

enum class GuardLifecycleState { ACTIVE, DISABLED_ARMAGEDDON, REMOVED }

data class GuardIdentity(
    val guardInstanceId: Long,
    val team: TeamId,
    val entityUuid: UUID
)

/**
 * Exact combat numbers stay outside this object until TruthGate resolves them.
 */
data class GuardRuntime(
    val identity: GuardIdentity,
    var lifecycle: GuardLifecycleState = GuardLifecycleState.ACTIVE,
    var nextAttackTick: Long = 0L
)

enum class GuardTargetPriorityPolicy {
    FIRST,
    LAST,
    RANDOM,
    EXPLICIT_ENGINEERING
}

data class GuardResolvedCombatConfig(
    val damagePerArrow: ResolvedTruth<Double>,
    val fireIntervalTicks: ResolvedTruth<Long>,
    val rangeBlocks: ResolvedTruth<Double>,
    val targetPriority:
        ResolvedTruth<GuardTargetPriorityPolicy>
) {
    init {
        require(damagePerArrow.value >= 0.0)
        require(fireIntervalTicks.value > 0L)
        require(rangeBlocks.value >= 0.0)
    }
}

data class CastleAttackResolvedConfig(
    val firstHitDelayTicks: ResolvedTruth<Long>,
    val ordinaryAttackIntervalTicks:
        ResolvedTruth<Long>
) {
    init {
        require(firstHitDelayTicks.value >= 0L)
        require(
            ordinaryAttackIntervalTicks.value > 0L
        )
    }
}
