package dev.cubecrafttd.arena

interface ArenaTickPhase {
    val order: Int
    val id: String
    fun tick(context: ArenaContext)
}

class ArenaTickEngine(
    phases: Collection<ArenaTickPhase>,
    private val nanoTimeSource:
        ArenaNanoTimeSource =
        SystemArenaNanoTimeSource,
    private val profiler:
        ArenaTickProfiler =
        ArenaTickProfiler()
) {
    private val phases = phases.sortedWith(
        compareBy<ArenaTickPhase> { it.order }.thenBy { it.id }
    )

    fun tick(context: ArenaContext) {
        val tickStart=
            nanoTimeSource.nowNanos()
        try {
            check(
                context.state ==
                    ArenaState.RUNNING
            ) {
                "Arena tick requires RUNNING state; current=${context.state}"
            }
            context.advanceSyntheticTick(1)
            phases.forEach { phase ->
                val phaseStart=
                    nanoTimeSource
                        .nowNanos()
                try {
                    phase.tick(context)
                } finally {
                    profiler.recordPhase(
                        phase.id,
                        nanoTimeSource
                            .nowNanos() -
                            phaseStart
                    )
                }
            }
        } finally {
            profiler.recordTick(
                nanoTimeSource
                    .nowNanos() -
                    tickStart
            )
        }
    }

    fun tick(context: ArenaContext, ticks: Int) {
        require(ticks >= 0)
        repeat(ticks) { tick(context) }
    }

    fun phaseIds(): List<String> =
        phases.map { it.id }

    fun profileSnapshot():
        ArenaTickProfileSnapshot =
        profiler.snapshot()

    fun resetProfile() {
        profiler.reset()
    }
}
