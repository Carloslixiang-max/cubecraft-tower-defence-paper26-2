package dev.cubecrafttd.ui

import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.tower.visual.TowerPath

data class PendingTowerPlacement(
    val clickedBlock: BlockPos,
    val towerId: String? = null
)

class TowerPlacementFlowState {
    var pending: PendingTowerPlacement? = null
        private set

    fun begin(clickedBlock: BlockPos) {
        pending=PendingTowerPlacement(clickedBlock)
    }

    fun chooseTower(towerId: String) {
        val current=pending
            ?: error("No pending tower placement")
        pending=current.copy(towerId=towerId)
    }

    fun choosePath(
        path: TowerPath
    ): Pair<BlockPos,TowerBuilderSelection> {
        val current=pending
            ?: error("No pending tower placement")
        val towerId=current.towerId
            ?: error("No tower selected for pending placement")
        pending=null
        return current.clickedBlock to
            TowerBuilderSelection(towerId,path)
    }

    fun cancel() {
        pending=null
    }
}
