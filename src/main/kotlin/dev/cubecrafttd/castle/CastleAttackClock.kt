package dev.cubecrafttd.castle

import dev.cubecrafttd.mob.MobInstanceId
import dev.cubecrafttd.truth.ResolvedTruth

data class CastleAttackClockConfig(
    val firstHitDelayTicks: ResolvedTruth<Long>,
    val repeatIntervalTicks: ResolvedTruth<Long>
) {
    init {
        require(firstHitDelayTicks.value >= 0L)
        require(repeatIntervalTicks.value > 0L)
    }
}

class CastleAttackClock(
    private val config: CastleAttackClockConfig
) {
    private val nextHitTick = linkedMapOf<MobInstanceId, Long>()

    fun begin(mobId: MobInstanceId, contactTick: Long) {
        check(mobId !in nextHitTick) { "Castle attack clock already exists for ${mobId.value}" }
        nextHitTick[mobId] = contactTick + config.firstHitDelayTicks.value
    }

    fun cancel(mobId: MobInstanceId) {
        nextHitTick.remove(mobId)
    }

    fun has(mobId: MobInstanceId): Boolean =
        mobId in nextHitTick

    fun size(): Int = nextHitTick.size

    fun due(mobId: MobInstanceId, gameTick: Long): Boolean =
        nextHitTick[mobId]?.let { gameTick >= it } ?: false

    fun consumeHit(mobId: MobInstanceId, gameTick: Long): Boolean {
        val dueAt = nextHitTick[mobId] ?: return false
        if (gameTick < dueAt) return false

        // Preserve cadence from the prior due time rather than wall clock.
        var next = dueAt + config.repeatIntervalTicks.value
        while (next <= gameTick) next += config.repeatIntervalTicks.value
        nextHitTick[mobId] = next
        return true
    }

    fun nextHitTick(mobId: MobInstanceId): Long? = nextHitTick[mobId]
}
