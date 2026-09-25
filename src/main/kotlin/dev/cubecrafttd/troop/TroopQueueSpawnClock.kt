package dev.cubecrafttd.troop

data class TroopSpawnCadence(
    val intervalTicks: Long,
    val source: String
) {
    init { require(intervalTicks > 0L) }
}

class TroopQueueSpawnClock(
    private val cadence: TroopSpawnCadence
) {
    private var nextSpawnTick: Long? = null

    fun arm(gameTick: Long) {
        if (nextSpawnTick == null) nextSpawnTick = gameTick
    }

    fun disarm() {
        nextSpawnTick = null
    }

    fun isDue(gameTick: Long): Boolean =
        nextSpawnTick?.let { gameTick >= it } ?: false

    fun consume(gameTick: Long): Boolean {
        val due = nextSpawnTick ?: return false
        if (gameTick < due) return false
        var next = due + cadence.intervalTicks
        while (next <= gameTick) next += cadence.intervalTicks
        nextSpawnTick = next
        return true
    }

    fun nextTick(): Long? = nextSpawnTick
}
