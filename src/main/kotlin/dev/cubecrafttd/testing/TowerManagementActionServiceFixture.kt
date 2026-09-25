package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.match.*
import dev.cubecrafttd.paper.PaperBlockWorldAdapter
import dev.cubecrafttd.progression.*
import dev.cubecrafttd.tower.lifecycle.*
import dev.cubecrafttd.tower.visual.*
import dev.cubecrafttd.ui.*
import java.util.UUID

private class TowerManageFakeWorld :
    PaperBlockWorldAdapter {
    private val blocks=
        linkedMapOf<BlockKey,BlockSnapshot>()

    override fun snapshot(
        block: BlockKey
    ): BlockSnapshot =
        blocks[block]
            ?: BlockSnapshot("minecraft:air")

    override fun apply(
        block: BlockKey,
        snapshot: BlockSnapshot,
        applyPhysics: Boolean
    ) {
        blocks[block]=snapshot
    }

    override fun currentBlockData(
        block: BlockKey
    ): String =
        snapshot(block).blockData
}

object TowerManagementActionServiceFixture {
    fun run():List<FixtureResult> {
        val player=UUID.fromString(
            "00000000-0000-0000-0000-000000031001"
        )
        val context=ArenaContext(
            ArenaId("tower-manage-actions"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000031100"
            ),
            TestingMapFactory.minimal()
        ).also {
            it.redTeam.players += player
            it.state=ArenaState.RUNNING
        }

        val ledger=EconomyLedger()
        RecommendedMaturePricing.seedPlayer(
            ledger,
            TeamId.RED,
            player,
            MatchStartingBalance(
                50_000L,0L
            ),
            1L,
            "seed"
        )

        val session=MatchSessionState(
            MatchRulePreset(
                MatchMode.NORMAL,
                PricingMode.NORMAL,
                GoldmineIncomeMode.NORMAL,
                ProgressionMode
                    .ALL_UNLOCKED,
                MatchStartingBalance(
                    50_000L,0L
                ),
                1L,1L
            ),
            linkedMapOf(
                player to
                    PlayerMatchSessionState(
                        player,
                        TroopProgressionState()
                    )
            )
        )
        session.players
            .getValue(player)
            .interaction.builder
            .select(
                "archer",
                TowerPath.TOP
            )

        val mutation=
            TowerBodyMutationService(
                TowerManageFakeWorld(),
                context.towerBodyLedger,
                RotationCache {
                    blockData,_ -> blockData
                }
            )
        val lifecycle=
            TowerLifecycleService(
                context,ledger,mutation
            )
        val worldActions=
            MatchTowerWorldActionService(
                context,
                session,
                lifecycle,
                EngineeringPlaceholderTowerBodyProvider,
                EngineeringFixedR0RotationPolicy
            )
        val placed=
            worldActions.place(
                player,
                BlockPos(0,0,2),
                quickPlace=false
            )
        val id=
            placed.receipt.towerInstanceId

        val lookup=
            TowerBodyInteractionLookup(
                context
            )
        val found=
            lookup.towerAt(
                placed.receipt.baseCenter
            )

        val service=
            TowerManagementActionService(
                context,worldActions,session
            )
        val stats=
            service.handle(
                player,
                "tower-manage:${id.value}:stats"
            ) as
                TowerManagementActionResult.Stats

        val upgraded=
            service.handle(
                player,
                "tower-manage:${id.value}:upgrade"
            ) as
                TowerManagementActionResult.Upgraded

        val rangeOn=
            service.handle(
                player,
                "tower-manage:${id.value}:rangefinder"
            ) as
                TowerManagementActionResult.RangefinderToggled
        val rangeOff=
            service.handle(
                player,
                "tower-manage:${id.value}:rangefinder"
            ) as
                TowerManagementActionResult.RangefinderToggled

        val sold=
            service.handle(
                player,
                "tower-manage:${id.value}:sell"
            ) as
                TowerManagementActionResult.Sold

        return listOf(
            FixtureResult(
                "tower-body-ledger-resolves-runtime-tower",
                found?.identity?.instanceId==
                    id
            ),
            FixtureResult(
                "tower-management-stats-no-scan-id",
                stats.stats.towerInstanceId==
                    id &&
                    stats.stats.towerId==
                        "archer" &&
                    stats.stats.level==1
            ),
            FixtureResult(
                "tower-management-upgrade-routes-lifecycle",
                upgraded.receipt
                    .towerInstanceId==id &&
                    upgraded.receipt
                        .upgradeCost>0L
            ),
            FixtureResult(
                "tower-management-rangefinder-pin-toggle",
                rangeOn.pinned &&
                    !rangeOff.pinned &&
                    id.value !in
                        session.players
                            .getValue(player)
                            .interaction
                            .pinnedRangefinderTowers
            ),
            FixtureResult(
                "tower-management-sell-routes-lifecycle",
                sold.receipt
                    .towerInstanceId==id &&
                    id !in
                        context.entityIndex
                            .towersByInstanceId
            )
        )
    }
}
