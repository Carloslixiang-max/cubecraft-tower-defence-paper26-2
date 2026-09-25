package dev.cubecrafttd.status

import java.util.UUID

enum class StatusEffectType {
    POISON,
    BURN,
    ICE_SLOW,
    STUN
}

enum class StatusStackPolicy {
    REPLACE_STRONGER,
    REFRESH_DURATION,
    MAX_DURATION,
    NON_STACKING
}

data class StatusEffectInstance(
    val type: StatusEffectType,
    val sourceId: String,
    val magnitude: Double,
    val appliedTick: Long,
    val expireTick: Long,
    val tickIntervalTicks: Long? = null,
    val sourcePlayerUuid: UUID? = null,
    val sourceTowerInstanceId: Long? = null
) {
    init {
        require(expireTick >= appliedTick)
        tickIntervalTicks?.let {
            require(it > 0L)
        }
    }
}

class StatusEffectSet(
    private val policies: Map<StatusEffectType, StatusStackPolicy>
) {
    private val active = linkedMapOf<StatusEffectType, StatusEffectInstance>()

    fun apply(effect: StatusEffectInstance): StatusEffectInstance {
        val current = active[effect.type]
        val policy = policies[effect.type] ?: StatusStackPolicy.NON_STACKING

        val resolved = when {
            current == null -> effect
            policy == StatusStackPolicy.REPLACE_STRONGER -> {
                when {
                    effect.magnitude > current.magnitude -> effect
                    effect.magnitude < current.magnitude -> current
                    else -> if (effect.expireTick > current.expireTick) effect else current
                }
            }
            policy == StatusStackPolicy.REFRESH_DURATION -> current.copy(
                sourceId = effect.sourceId,
                magnitude = maxOf(current.magnitude, effect.magnitude),
                appliedTick = effect.appliedTick,
                expireTick = maxOf(current.expireTick, effect.expireTick)
            )
            policy == StatusStackPolicy.MAX_DURATION -> {
                if (effect.expireTick > current.expireTick) effect else current
            }
            else -> current
        }

        active[effect.type] = resolved
        return resolved
    }

    fun removeExpired(gameTick: Long) {
        active.entries.removeIf { it.value.expireTick <= gameTick }
    }

    fun get(type: StatusEffectType): StatusEffectInstance? = active[type]

    fun all(): Map<StatusEffectType, StatusEffectInstance> = active.toMap()
}

object RecommendedMatureStatusPolicies {
    /**
     * These are implementation semantics for deterministic runtime composition,
     * not claims about hidden CubeCraft internal code.
     */
    val policies: Map<StatusEffectType, StatusStackPolicy> = mapOf(
        StatusEffectType.POISON to StatusStackPolicy.NON_STACKING,
        StatusEffectType.BURN to StatusStackPolicy.NON_STACKING,
        StatusEffectType.ICE_SLOW to StatusStackPolicy.NON_STACKING,
        StatusEffectType.STUN to StatusStackPolicy.NON_STACKING
    )
}
