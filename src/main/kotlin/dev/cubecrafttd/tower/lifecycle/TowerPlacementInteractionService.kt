package dev.cubecrafttd.tower.lifecycle

import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.match.MatchSessionState
import dev.cubecrafttd.tower.visual.TowerPath
import dev.cubecrafttd.ui.*
import java.util.UUID

sealed interface TowerPlacementInteractionResult {
    data class OpenBuilder(
        val menu: MenuDefinition
    ) : TowerPlacementInteractionResult

    data class OpenPathSelector(
        val menu: MenuDefinition,
        val towerId: String
    ) : TowerPlacementInteractionResult

    data class Placed(
        val result: TowerWorldPlaceResult
    ) : TowerPlacementInteractionResult
}

class TowerPlacementInteractionService(
    private val session: MatchSessionState,
    private val worldActions: MatchTowerWorldActionService
) {
    fun beginRegular(
        playerUuid: UUID,
        clickedBlock: BlockPos
    ): TowerPlacementInteractionResult.OpenBuilder {
        val player=player(playerUuid)
        player.interaction.towerPlacement.begin(clickedBlock)
        return TowerPlacementInteractionResult.OpenBuilder(
            TowerBuilderMenus.combinedMatureEngineering
        )
    }

    fun chooseTower(
        playerUuid: UUID,
        towerId: String
    ): TowerPlacementInteractionResult.OpenPathSelector {
        val player=player(playerUuid)
        player.interaction.towerPlacement.chooseTower(towerId)
        return TowerPlacementInteractionResult.OpenPathSelector(
            TowerBuilderMenus.pathSelectorEngineeringSlots,
            towerId
        )
    }

    /**
     * 2021 Mature flow: the tower item click itself chooses the path and
     * commits placement. The older engineering path-selector inventory is not
     * part of the ordinary live placement path anymore.
     */
    fun chooseTowerAndPlace(
        playerUuid: UUID,
        towerId: String,
        path: TowerPath
    ): TowerPlacementInteractionResult.Placed {
        player(playerUuid)
            .interaction
            .towerPlacement
            .chooseTower(towerId)
        return choosePathAndPlace(
            playerUuid,
            path
        )
    }

    fun choosePathAndPlace(
        playerUuid: UUID,
        path: TowerPath
    ): TowerPlacementInteractionResult.Placed {
        val player=player(playerUuid)
        val (clickedBlock,selection)=
            player.interaction.towerPlacement.choosePath(path)
        player.interaction.builder.select(
            selection.towerId,
            selection.path
        )
        return TowerPlacementInteractionResult.Placed(
            worldActions.place(
                playerUuid,
                clickedBlock,
                quickPlace=false
            )
        )
    }

    fun quickPlace(
        playerUuid: UUID,
        clickedBlock: BlockPos
    ): TowerPlacementInteractionResult.Placed =
        TowerPlacementInteractionResult.Placed(
            worldActions.place(
                playerUuid,
                clickedBlock,
                quickPlace=true
            )
        )

    fun cancel(playerUuid: UUID) {
        player(playerUuid)
            .interaction
            .towerPlacement
            .cancel()
    }

    private fun player(
        playerUuid: UUID
    ) = session.players[playerUuid]
        ?: error("Player not in match session")
}
