package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.map.*
import dev.cubecrafttd.map.importer.MapRuntimeCompiler
import dev.cubecrafttd.paper.PaperBlockWorldAdapter
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.tower.lifecycle.*
import dev.cubecrafttd.tower.visual.*
import java.util.UUID

private class BoundaryFakeWorld : PaperBlockWorldAdapter {
    val blocks = linkedMapOf<BlockKey, BlockSnapshot>()
    var failNextApply = false

    override fun snapshot(block: BlockKey): BlockSnapshot =
        blocks[block] ?: BlockSnapshot("minecraft:air")

    override fun apply(
        block: BlockKey,
        snapshot: BlockSnapshot,
        applyPhysics: Boolean
    ) {
        if (failNextApply) {
            failNextApply = false
            error("synthetic restore failure")
        }
        blocks[block] = snapshot
    }

    override fun currentBlockData(block: BlockKey): String =
        snapshot(block).blockData
}

object TowerLifecycleBoundaryFixture {
    private val owner =
        UUID.fromString("00000000-0000-0000-0000-000000006666")

    private fun body(
        id: String,
        level: Int,
        path: TowerPath?,
        revision: String,
        material: String
    ) = TowerBodyDefinition(
        id,level,path,revision,
        listOf(
            RelativeBodyBlock(BlockPos(0,0,0),material)
        ),
        BodyEvidenceStatus.ENGINEERING_PLACEHOLDER,
        listOf("boundary-fixture")
    )

    private fun setup(): Triple<
        ArenaContext,
        EconomyLedger,
        Pair<BoundaryFakeWorld,TowerLifecycleService>
    > {
        val map = MapRuntimeCompiler.compile(
            FarmFullRuntimeFixture.candidate()
        )
        val context = ArenaContext(
            ArenaId("boundary"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000001111"
            ),
            map
        ).also { it.state = ArenaState.RUNNING }

        val ledger = EconomyLedger()
        ledger.apply(
            EconomyTransaction(
                1,0,
                EconomyAccount(
                    TeamId.BLUE,owner,
                    EconomyCurrency.MATCH_COINS
                ),
                10000,
                EconomyReason.MATCH_INITIALIZATION,
                "boundary-seed"
            )
        )
        val world = BoundaryFakeWorld()
        val service = TowerLifecycleService(
            context,ledger,
            TowerBodyMutationService(
                world,
                context.towerBodyLedger,
                RotationCache { b,_ -> b }
            )
        )
        return Triple(context,ledger,world to service)
    }

    fun run(): List<FixtureResult> {
        val (context,ledger,pair) = setup()
        val (world,service) = pair
        val spot = context.mapRuntime.explicitTowerSpots.first {
            it.team == TeamId.BLUE &&
                it.footprint == FootprintSize.THREE_BY_THREE
        }
        val account = EconomyAccount(
            TeamId.BLUE,owner,EconomyCurrency.MATCH_COINS
        )

        service.place(
            TowerPlacementCommand(
                TowerInstanceId(700),"mage",owner,TeamId.BLUE,
                spot.center,true,TowerPath.TOP,
                body("mage",1,null,"m1","minecraft:stone"),
                QuarterTurn.R0,2,"place-700"
            )
        )

        val duplicateCorrelationBlockedBeforeWorldMutation = try {
            service.place(
                TowerPlacementCommand(
                    TowerInstanceId(701),"mage",owner,TeamId.BLUE,
                    spot.center,true,TowerPath.TOP,
                    body("mage",1,null,"m1b","minecraft:stone"),
                    QuarterTurn.R0,3,"place-700"
                )
            )
            false
        } catch (_: IllegalStateException) {
            true
        }

        val pathSwitchBlocked = try {
            service.upgrade(
                TowerUpgradeCommand(
                    TowerInstanceId(700),owner,2,
                    TowerPath.BOTTOM,
                    body(
                        "mage",2,TowerPath.BOTTOM,
                        "m2b","minecraft:cobblestone"
                    ),
                    QuarterTurn.R0,4,"upgrade-path-switch"
                )
            )
            false
        } catch (_: IllegalStateException) {
            true
        }

        val bodyKey = context.towerBodyLedger
            .ownedBy(700L).keys.single()
        world.blocks[bodyKey] =
            BlockSnapshot("minecraft:diamond_block")

        val balanceBeforeConflictUpgrade =
            ledger.balance(account)
        val externalEditUpgradeBlocked = try {
            service.upgrade(
                TowerUpgradeCommand(
                    TowerInstanceId(700),owner,2,
                    TowerPath.TOP,
                    body(
                        "mage",2,TowerPath.TOP,
                        "m2","minecraft:cobblestone"
                    ),
                    QuarterTurn.R0,5,"upgrade-external-conflict"
                )
            )
            false
        } catch (_: IllegalStateException) {
            true
        }

        val noDebitOnPreflightConflict =
            ledger.balance(account) ==
                balanceBeforeConflictUpgrade

        val sell = service.sell(
            TowerSellCommand(
                TowerInstanceId(700),owner,owner,
                6,"sell-external-conflict"
            )
        )

        // A separate tower proves failed restore is transactionally rolled back.
        val (ctx2,ledger2,pair2) = setup()
        val (world2,service2) = pair2
        val spot2 = ctx2.mapRuntime.explicitTowerSpots.first {
            it.team == TeamId.BLUE &&
                it.footprint == FootprintSize.THREE_BY_THREE
        }
        service2.place(
            TowerPlacementCommand(
                TowerInstanceId(800),"mage",owner,TeamId.BLUE,
                spot2.center,true,TowerPath.TOP,
                body("mage",1,null,"r1","minecraft:stone"),
                QuarterTurn.R0,10,"place-800"
            )
        )
        val ledgerSizeBeforeSell =
            ctx2.towerBodyLedger.size()
        val blockBeforeSell =
            world2.blocks.toMap()
        world2.failNextApply = true
        val restoreFailureRolledBack = try {
            service2.sell(
                TowerSellCommand(
                    TowerInstanceId(800),owner,owner,
                    11,"sell-restore-failure"
                )
            )
            false
        } catch (_: Throwable) {
            ctx2.entityIndex.towersByInstanceId
                .containsKey(TowerInstanceId(800)) &&
                ctx2.entityIndex.towersByInstanceId
                    .getValue(TowerInstanceId(800))
                    .lifecycle == TowerLifecycleState.ACTIVE &&
                ctx2.towerBodyLedger.size() ==
                    ledgerSizeBeforeSell &&
                world2.blocks == blockBeforeSell
        }

        return listOf(
            FixtureResult(
                "tower-duplicate-correlation-preflight",
                duplicateCorrelationBlockedBeforeWorldMutation
            ),
            FixtureResult(
                "tower-path-switch-blocked",
                pathSwitchBlocked
            ),
            FixtureResult(
                "tower-upgrade-external-edit-preflight",
                externalEditUpgradeBlocked &&
                    noDebitOnPreflightConflict
            ),
            FixtureResult(
                "tower-sell-external-edit-leaves-block-and-reports",
                sell.conflictReport.hasConflicts &&
                    world.blocks[bodyKey]?.blockData ==
                        "minecraft:diamond_block" &&
                    context.towerBodyLedger.isEmpty()
            ),
            FixtureResult(
                "tower-sell-restore-failure-rolls-back",
                restoreFailureRolledBack
            )
        )
    }
}
