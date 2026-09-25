package dev.cubecrafttd.tower.lifecycle

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.match.MatchSessionState
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.tower.visual.*
import java.util.UUID

fun interface TowerActionRotationPolicy {
    fun rotation(
        playerUuid: UUID,
        clickedBlock: BlockPos
    ): QuarterTurn
}

object EngineeringFixedR0RotationPolicy :
    TowerActionRotationPolicy {
    override fun rotation(
        playerUuid: UUID,
        clickedBlock: BlockPos
    ): QuarterTurn =
        QuarterTurn.R0
}

data class TowerWorldPlaceResult(
    val receipt:
        TowerPlacementReceipt,
    val selection:
        dev.cubecrafttd.ui
            .TowerBuilderSelection,
    val bodyEvidence:
        BodyEvidenceStatus
)

class MatchTowerWorldActionService(
    private val context: ArenaContext,
    private val session: MatchSessionState,
    private val lifecycle:
        TowerLifecycleService,
    private val bodies:
        TowerBodyDefinitionProvider,
    private val rotation:
        TowerActionRotationPolicy,
    initialSequence: Long =
        5_000_000L
) {
    private var nextSequence=
        initialSequence

    fun place(
        playerUuid: UUID,
        clickedBlock: BlockPos,
        quickPlace: Boolean
    ): TowerWorldPlaceResult {
        val player=
            session.players[playerUuid]
                ?: error(
                    "Player not in match session"
                )
        val builder=
            player.interaction.builder
        val candidateSelection=
            if(quickPlace)
                builder.quickPlaceSelection()
            else
                builder.selected()
        val selection=
            candidateSelection
                ?: error(
                    if(quickPlace)
                        "No successfully placed tower is remembered for quick-place"
                    else
                        "No tower selected"
                )

        val team=teamOf(
            playerUuid
        )
        val body=bodies.definition(
            selection.towerId,
            1,
            selection.path
        )
        val seq=nextSequence++
        val receipt=
            lifecycle.place(
                TowerPlacementCommand(
                    towerInstanceId=
                        TowerInstanceId(seq),
                    towerId=
                        selection.towerId,
                    ownerUuid=
                        playerUuid,
                    team=team,
                    clickedBlock=
                        clickedBlock,
                    autoCentre=
                        player.interaction
                            .settings
                            .autoCentreTowers,
                    path=
                        selection.path,
                    bodyDefinition=body,
                    rotation=
                        rotation.rotation(
                            playerUuid,
                            clickedBlock
                        ),
                    transactionId=seq,
                    correlationId=
                        "tower-place:$playerUuid:$seq"
                )
            )

        builder.confirmPlaced(
            selection
        )

        return TowerWorldPlaceResult(
            receipt,
            selection,
            body.evidenceStatus
        )
    }

    fun quickUpgrade(
        playerUuid: UUID,
        towerId: TowerInstanceId
    ): TowerUpgradeReceipt {
        val tower=
            context.entityIndex
                .towersByInstanceId[
                    towerId
                ] ?: error(
                    "Unknown tower"
                )
        check(tower.upgrade.level<4) {
            "Tower is already max level"
        }
        val path=tower.upgrade.path
            ?: error(
                "Tower path missing"
            )
        val nextLevel=
            tower.upgrade.level+1
        val body=bodies.definition(
            tower.identity.towerId,
            nextLevel,
            path
        )
        val seq=nextSequence++

        return lifecycle.upgrade(
            TowerUpgradeCommand(
                towerInstanceId=towerId,
                actorUuid=playerUuid,
                newLevel=nextLevel,
                newPath=path,
                newBodyDefinition=body,
                rotation=
                    rotation.rotation(
                        playerUuid,
                        BlockPos(
                            tower.geometry
                                .baseOrigin.x
                                .toInt(),
                            tower.geometry
                                .baseOrigin.y
                                .toInt(),
                            tower.geometry
                                .baseOrigin.z
                                .toInt()
                        )
                    ),
                transactionId=seq,
                correlationId=
                    "tower-upgrade:$playerUuid:$seq"
            )
        )
    }

    fun sell(
        playerUuid: UUID,
        towerId: TowerInstanceId
    ): TowerSellReceipt {
        val seq=nextSequence++
        return lifecycle.sell(
            TowerSellCommand(
                towerInstanceId=towerId,
                actorUuid=playerUuid,
                refundRecipientUuid=
                    playerUuid,
                transactionId=seq,
                correlationId=
                    "tower-sell:$playerUuid:$seq"
            )
        )
    }

    private fun teamOf(
        playerUuid: UUID
    ): TeamId =
        when {
            context.redTeam.players
                .contains(playerUuid) ->
                TeamId.RED
            context.blueTeam.players
                .contains(playerUuid) ->
                TeamId.BLUE
            else ->
                error(
                    "Player not in arena"
                )
        }
}
