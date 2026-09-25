package dev.cubecrafttd.mob

import dev.cubecrafttd.arena.*

fun interface MobDeathFinalizationPort {
    /**
     * Called only after the live entity was successfully removed and before
     * the arena index unregisters the mob.
     */
    fun finalize(
        context: ArenaContext,
        mob: MobRuntimeState
    )
}

object NoOpMobDeathFinalizationPort :
    MobDeathFinalizationPort {
    override fun finalize(
        context: ArenaContext,
        mob: MobRuntimeState
    ) = Unit
}

data class MobCleanupTickMetrics(
    var deadVisited: Int = 0,
    var removed: Int = 0,
    var failedRemovals: Int = 0,
    var finalizations: Int = 0,
    var finalizationFailures: Int = 0
)

class MobLifecycleCleanupTickPhase(
    private val removal:
        TrackedEntityRemovalPort,
    private val finalization:
        MobDeathFinalizationPort =
        NoOpMobDeathFinalizationPort
) : ArenaTickPhase {
    override val order: Int =
        NormalArenaPhaseOrder.MOB_CLEANUP
    override val id: String =
        "mob-cleanup"

    private val metrics=
        MobCleanupTickMetrics()

    override fun tick(
        context: ArenaContext
    ) {
        val deadIds=
            context.entityIndex
                .mobsByUuid
                .values
                .filter {
                    it.combat.lifecycle ==
                        MobLifecycleState.DEAD
                }
                .map {
                    it.identity.entityUuid
                }
                .sortedBy {
                    it.toString()
                }

        deadIds.forEach { uuid ->
            metrics.deadVisited++

            val removedLive=
                runCatching {
                    removal.remove(uuid)
                }.getOrDefault(false)

            if(!removedLive) {
                metrics.failedRemovals++
                return@forEach
            }

            val state=
                context.entityIndex
                    .mobsByUuid[uuid]
                    ?: return@forEach

            val finalized=
                runCatching {
                    finalization.finalize(
                        context,state
                    )
                }.isSuccess

            if(!finalized) {
                metrics.finalizationFailures++
                return@forEach
            }

            metrics.finalizations++

            context.entityIndex
                .unregisterMob(uuid)
                ?: return@forEach

            state.combat.lifecycle=
                MobLifecycleState.REMOVED
            metrics.removed++
        }
    }

    fun metricsSnapshot():
        MobCleanupTickMetrics =
        metrics.copy()
}
