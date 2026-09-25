package dev.cubecrafttd.tower.lifecycle

import dev.cubecrafttd.arena.ArenaContext
import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.tower.TowerRuntimeState
import dev.cubecrafttd.tower.visual.BlockKey

class TowerBodyInteractionLookup(
    private val context: ArenaContext
) {
    fun towerAt(
        clickedBlock: BlockPos
    ): TowerRuntimeState? {
        val record=
            context.towerBodyLedger.get(
                BlockKey(
                    clickedBlock.x,
                    clickedBlock.y,
                    clickedBlock.z
                )
            ) ?: return null

        return context.entityIndex
            .towersByInstanceId
            .values
            .firstOrNull {
                it.identity.instanceId.value ==
                    record.towerInstanceId
            }
    }
}
