package dev.cubecrafttd.tower

import dev.cubecrafttd.truth.ResolvedTruth
import dev.cubecrafttd.truth.ResolutionSource
import kotlin.math.roundToLong

data class ResolvedTowerAttackTiming(
    val intervalTicks: Long,
    val source: ResolutionSource
) {
    init { require(intervalTicks > 0L) }
}

object TowerAttackTimingResolver {
    fun resolve(
        stage: TowerPathDefinition,
        explicitFallbackSeconds: ResolvedTruth<Double>? = null
    ): ResolvedTowerAttackTiming {
        val observed = stage.stats.attackIntervalSeconds
        if (observed != null) {
            val ticks = (observed * 20.0).roundToLong()
            require(ticks > 0L)
            return ResolvedTowerAttackTiming(
                ticks,
                ResolutionSource.OBSERVED_ORIGINAL
            )
        }

        val fallback = explicitFallbackSeconds
            ?: error(
                "Attack interval unresolved for ${stage.option} L${stage.level}; " +
                    "explicit engineering fallback required"
            )
        val ticks = (fallback.value * 20.0).roundToLong()
        require(ticks > 0L)
        return ResolvedTowerAttackTiming(ticks, fallback.source)
    }
}

class TowerAttackClock(
    private val timing: ResolvedTowerAttackTiming,
    firstReadyTick: Long
) {
    var nextAttackTick: Long = firstReadyTick
        private set

    fun isReady(gameTick: Long): Boolean =
        gameTick >= nextAttackTick

    fun consume(gameTick: Long): Boolean {
        if (!isReady(gameTick)) return false
        var next = nextAttackTick + timing.intervalTicks
        while (next <= gameTick) {
            next += timing.intervalTicks
        }
        nextAttackTick = next
        return true
    }
}
