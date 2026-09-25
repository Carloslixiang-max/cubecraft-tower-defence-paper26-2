package dev.cubecrafttd.tower.lifecycle

import dev.cubecrafttd.arena.ArenaContext
import dev.cubecrafttd.tower.*

data class TowerExternalDestructionReceipt(
    val towerInstanceId: TowerInstanceId,
    val conflictReport:
        dev.cubecrafttd.tower.visual
            .TowerBodyConflictReport
)

/**
 * Used by environment/Armageddon destruction.
 * No player interaction permission and no economy refund are involved.
 */
class TowerExternalDestructionService(
    private val context: ArenaContext,
    private val bodyMutation:
        TowerBodyMutationService
) {
    fun destroy(
        towerId: TowerInstanceId
    ): TowerExternalDestructionReceipt {
        val tower=
            context.entityIndex
                .towersByInstanceId[
                    towerId
                ] ?: error(
                    "Unknown tower ${towerId.value}"
                )

        check(
            tower.lifecycle ==
                TowerLifecycleState.ACTIVE
        ) {
            "Tower ${towerId.value} is already mutating"
        }

        tower.lifecycle=
            TowerLifecycleState.REMOVING

        val removal=try {
            bodyMutation.remove(
                towerId.value
            )
        } catch(t:Throwable) {
            tower.lifecycle=
                TowerLifecycleState.ACTIVE
            throw t
        }

        context.entityIndex
            .unregisterTower(towerId)
            ?: error(
                "Tower vanished during external destruction"
            )

        return TowerExternalDestructionReceipt(
            towerId,
            removal.conflictReport
        )
    }
}
