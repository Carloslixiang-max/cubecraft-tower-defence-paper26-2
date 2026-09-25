package dev.cubecrafttd.arena

/**
 * Builds the verified phase ordering for Normal runtime.
 * Optional phases remain optional because Paper/live adapters and unresolved
 * exact values are injected by the composition root.
 */
object NormalArenaPhaseOrder {
    const val GOLDMINE = 20
    const val TROOP_SPAWN = 30
    const val MOB_MOVEMENT = 40
    const val MOB_SUPPORT = 50
    const val LEACH_CHARGE = 55
    const val SUMMON_MAINTENANCE = 57
    const val TOWER_COMBAT = 60
    const val CASTLE_GUARDS = 70
    const val CASTLE_ATTACKS = 80
    const val MOB_CLEANUP = 90
}

class NormalArenaRuntimeEngine(
    phases: Collection<ArenaTickPhase>
) {
    private val engine=
        ArenaTickEngine(phases)

    fun tick(
        context: ArenaContext
    ) = engine.tick(context)

    fun tick(
        context: ArenaContext,
        ticks: Int
    ) = engine.tick(context,ticks)

    fun phaseIds(): List<String> =
        engine.phaseIds()
}
