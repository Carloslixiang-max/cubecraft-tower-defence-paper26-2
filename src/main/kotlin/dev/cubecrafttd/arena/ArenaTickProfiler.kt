package dev.cubecrafttd.arena

fun interface ArenaNanoTimeSource {
    fun nowNanos(): Long
}

object SystemArenaNanoTimeSource :
    ArenaNanoTimeSource {
    override fun nowNanos(): Long =
        System.nanoTime()
}

data class ArenaPhaseProfileSnapshot(
    val calls: Long,
    val totalNanos: Long,
    val lastNanos: Long,
    val maxNanos: Long
) {
    val averageNanos: Long
        get() =
            if(calls==0L) 0L
            else totalNanos / calls
}

data class ArenaTickProfileSnapshot(
    val ticks: Long,
    val totalNanos: Long,
    val lastNanos: Long,
    val maxNanos: Long,
    val slowTicksOver50ms: Long,
    val phases:
        Map<String,ArenaPhaseProfileSnapshot>
) {
    val averageNanos: Long
        get() =
            if(ticks==0L) 0L
            else totalNanos / ticks
}

private data class MutableArenaPhaseProfile(
    var calls: Long = 0L,
    var totalNanos: Long = 0L,
    var lastNanos: Long = 0L,
    var maxNanos: Long = 0L
)

class ArenaTickProfiler {
    private var ticks=0L
    private var totalNanos=0L
    private var lastNanos=0L
    private var maxNanos=0L
    private var slowTicks=0L
    private val phases=
        linkedMapOf<
            String,
            MutableArenaPhaseProfile
        >()

    fun recordPhase(
        id: String,
        elapsedNanos: Long
    ) {
        val elapsed=
            elapsedNanos.coerceAtLeast(0L)
        val state=
            phases.getOrPut(id) {
                MutableArenaPhaseProfile()
            }
        state.calls++
        state.totalNanos += elapsed
        state.lastNanos=elapsed
        if(elapsed>state.maxNanos)
            state.maxNanos=elapsed
    }

    fun recordTick(
        elapsedNanos: Long
    ) {
        val elapsed=
            elapsedNanos.coerceAtLeast(0L)
        ticks++
        totalNanos += elapsed
        lastNanos=elapsed
        if(elapsed>maxNanos)
            maxNanos=elapsed
        if(elapsed>=50_000_000L)
            slowTicks++
    }

    fun snapshot():
        ArenaTickProfileSnapshot =
        ArenaTickProfileSnapshot(
            ticks=ticks,
            totalNanos=totalNanos,
            lastNanos=lastNanos,
            maxNanos=maxNanos,
            slowTicksOver50ms=
                slowTicks,
            phases=
                phases.mapValues {
                    (_,v) ->
                    ArenaPhaseProfileSnapshot(
                        calls=v.calls,
                        totalNanos=
                            v.totalNanos,
                        lastNanos=
                            v.lastNanos,
                        maxNanos=
                            v.maxNanos
                    )
                }
        )

    fun reset() {
        ticks=0L
        totalNanos=0L
        lastNanos=0L
        maxNanos=0L
        slowTicks=0L
        phases.clear()
    }
}
