package dev.cubecrafttd.arena

interface ArenaTickPhase {
    val order: Int
    val id: String
    fun tick(context: ArenaContext)
}

class ArenaTickEngine(
    phases: Collection<ArenaTickPhase>
) {
    private val phases = phases.sortedWith(
        compareBy<ArenaTickPhase> { it.order }.thenBy { it.id }
    )

    fun tick(context: ArenaContext) {
        check(context.state == ArenaState.RUNNING) {
            "Arena tick requires RUNNING state; current=${context.state}"
        }
        context.advanceSyntheticTick(1)
        phases.forEach { it.tick(context) }
    }

    fun tick(context: ArenaContext, ticks: Int) {
        require(ticks >= 0)
        repeat(ticks) { tick(context) }
    }

    fun phaseIds(): List<String> = phases.map { it.id }
}
