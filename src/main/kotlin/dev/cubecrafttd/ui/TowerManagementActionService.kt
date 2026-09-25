package dev.cubecrafttd.ui

import dev.cubecrafttd.arena.ArenaContext
import dev.cubecrafttd.tower.TowerInstanceId
import dev.cubecrafttd.tower.lifecycle.MatchTowerWorldActionService
import dev.cubecrafttd.tower.lifecycle.TowerSellReceipt
import dev.cubecrafttd.tower.lifecycle.TowerUpgradeReceipt
import dev.cubecrafttd.tower.visual.TowerPath
import dev.cubecrafttd.match.MatchSessionState
import java.util.UUID

data class TowerManagementStats(
    val towerInstanceId: TowerInstanceId,
    val towerId: String,
    val level: Int,
    val path: TowerPath?,
    val ownerUuid: UUID,
    val currentGameCoinsInvested: Long
)

sealed interface TowerManagementActionResult {
    data class Stats(
        val stats: TowerManagementStats
    ) : TowerManagementActionResult

    data class Upgraded(
        val receipt: TowerUpgradeReceipt
    ) : TowerManagementActionResult

    data class Sold(
        val receipt: TowerSellReceipt
    ) : TowerManagementActionResult

    data class RangefinderToggled(
        val towerInstanceId: TowerInstanceId,
        val pinned: Boolean
    ) : TowerManagementActionResult
}

class TowerManagementActionService(
    private val context: ArenaContext,
    private val worldActions:
        MatchTowerWorldActionService,
    private val session: MatchSessionState
) {
    fun handle(
        playerUuid: UUID,
        actionId: String
    ): TowerManagementActionResult {
        val parts=actionId.split(':')
        check(
            parts.size==3 &&
                parts[0]=="tower-manage"
        ) {
            "Expected tower-manage:<instanceId>:<stats|upgrade|sell>"
        }

        val towerId=
            TowerInstanceId(
                parts[1].toLong()
            )
        val tower=
            context.entityIndex
                .towersByInstanceId[
                    towerId
                ] ?: error(
                    "Unknown tower ${towerId.value}"
                )

        return when(parts[2]) {
            "stats" ->
                TowerManagementActionResult
                    .Stats(
                        TowerManagementStats(
                            towerInstanceId=
                                tower.identity
                                    .instanceId,
                            towerId=
                                tower.identity
                                    .towerId,
                            level=
                                tower.upgrade.level,
                            path=
                                tower.upgrade.path,
                            ownerUuid=
                                tower.identity
                                    .ownerUuid,
                            currentGameCoinsInvested=
                                tower.investment
                                    .currentGameCoinsInvested
                        )
                    )

            "upgrade" ->
                TowerManagementActionResult
                    .Upgraded(
                        worldActions
                            .quickUpgrade(
                                playerUuid,
                                towerId
                            )
                    )

            "sell" ->
                TowerManagementActionResult
                    .Sold(
                        worldActions.sell(
                            playerUuid,
                            towerId
                        )
                    )

            "rangefinder" -> {
                val player=session.players[playerUuid]
                    ?: error("Player not in match session")
                val pins=player.interaction
                    .pinnedRangefinderTowers
                val pinned=if(towerId.value in pins) {
                    pins.remove(towerId.value)
                    false
                } else {
                    pins += towerId.value
                    true
                }
                TowerManagementActionResult
                    .RangefinderToggled(
                        towerId,pinned
                    )
            }

            else ->
                error(
                    "Unknown tower management action ${parts[2]}"
                )
        }
    }
}
