package dev.cubecrafttd.mob

import dev.cubecrafttd.truth.ResolvedTruth

enum class WitchHealMode {
    MAX_HEALTH_FRACTION,
    ABSOLUTE_HP
}

data class WitchHealConfig(
    val healValue: ResolvedTruth<Double>,
    val healIntervalTicks: ResolvedTruth<Long>,
    val healRadiusBlocks: ResolvedTruth<Double>,
    val targetCap: ResolvedTruth<Int>,
    val mode: ResolvedTruth<WitchHealMode>
) {
    init {
        require(healValue.value >= 0.0)
        require(healIntervalTicks.value > 0L)
        require(healRadiusBlocks.value >= 0.0)
        require(targetCap.value > 0)
        if(mode.value == WitchHealMode.MAX_HEALTH_FRACTION) {
            require(healValue.value in 0.0..1.0)
        }
    }

    fun healAmountFor(targetMaxHealth: Double): Double =
        when(mode.value) {
            WitchHealMode.MAX_HEALTH_FRACTION ->
                targetMaxHealth * healValue.value
            WitchHealMode.ABSOLUTE_HP ->
                healValue.value
        }
}

data class PassiveRegenConfig(
    val healthPerSecond: ResolvedTruth<Double>
) {
    init {
        require(healthPerSecond.value >= 0.0)
    }
    val healthPerTick: Double
        get() = healthPerSecond.value / 20.0
}

typealias GiantRegenConfig = PassiveRegenConfig

class WitchHealClock(
    private val config: WitchHealConfig
) {
    private var nextHealTick: Long? = null

    fun arm(gameTick: Long) {
        if(nextHealTick == null) {
            nextHealTick =
                gameTick +
                    config.healIntervalTicks.value
        }
    }

    fun due(gameTick: Long): Boolean =
        nextHealTick?.let {
            gameTick >= it
        } ?: false

    fun consume(gameTick: Long): Boolean {
        val dueAt=nextHealTick
            ?: return false
        if(gameTick < dueAt) return false

        var next =
            dueAt +
                config.healIntervalTicks.value
        while(next <= gameTick) {
            next +=
                config.healIntervalTicks.value
        }
        nextHealTick=next
        return true
    }

    fun nextTick(): Long? = nextHealTick
}

object PassiveRegenerationService {
    fun regenerate(
        currentHealth: Double,
        maxHealth: Double,
        config: PassiveRegenConfig,
        ticksElapsed: Long
    ): Double {
        require(ticksElapsed >= 0L)
        return (
            currentHealth +
                config.healthPerTick *
                    ticksElapsed
        ).coerceAtMost(maxHealth)
    }
}

object GiantRegenerationService {
    fun regenerate(
        currentHealth: Double,
        maxHealth: Double,
        config: GiantRegenConfig,
        ticksElapsed: Long
    ): Double =
        PassiveRegenerationService
            .regenerate(
                currentHealth,
                maxHealth,
                config,
                ticksElapsed
            )
}
