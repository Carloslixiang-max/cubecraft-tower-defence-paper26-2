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

private class FakeBlockWorld(
    initial: Map<BlockKey, BlockSnapshot> = emptyMap()
) : PaperBlockWorldAdapter {
    val blocks = linkedMapOf<BlockKey, BlockSnapshot>().apply {
        putAll(initial)
    }
    var failOnApplyNumber: Int? = null
    private var applyCounter = 0

    override fun snapshot(block: BlockKey): BlockSnapshot =
        blocks[block] ?: BlockSnapshot("minecraft:air")

    override fun apply(
        block: BlockKey,
        snapshot: BlockSnapshot,
        applyPhysics: Boolean
    ) {
        applyCounter++
        if (failOnApplyNumber == applyCounter) {
            error("synthetic block write failure")
        }
        blocks[block] = snapshot
    }

    override fun currentBlockData(block: BlockKey): String =
        snapshot(block).blockData

    fun resetApplyCounter() {
        applyCounter = 0
    }
}

object TowerLifecycleTransactionFixture {
    private val owner =
        UUID.fromString("00000000-0000-0000-0000-000000005555")

    private fun context(): ArenaContext {
        val map = MapRuntimeCompiler.compile(
            FarmFullRuntimeFixture.candidate()
        )
        return ArenaContext(
            ArenaId("tower-lifecycle-fixture"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000000999"
            ),
            map
        ).also {
            it.state = ArenaState.RUNNING
        }
    }

    private fun body(
        towerId: String,
        level: Int,
        path: TowerPath?,
        revision: String,
        material: String,
        includeSecondBlock: Boolean = false
    ) = TowerBodyDefinition(
        towerId = towerId,
        level = level,
        path = path,
        bodyRevision = revision,
        blocks = buildList {
            add(RelativeBodyBlock(BlockPos(0,0,0),material))
            if (includeSecondBlock) {
                add(
                    RelativeBodyBlock(
                        BlockPos(0,1,0),
                        "$material[level2]"
                    )
                )
            }
        },
        evidenceStatus =
            BodyEvidenceStatus.ENGINEERING_PLACEHOLDER,
        evidenceRefs = listOf("fixture")
    )

    private fun seededLedger(
        context: ArenaContext
    ): Pair<EconomyLedger, EconomyAccount> {
        val ledger = EconomyLedger()
        val account = EconomyAccount(
            TeamId.BLUE,
            owner,
            EconomyCurrency.MATCH_COINS
        )
        ledger.apply(
            EconomyTransaction(
                1,0,account,10000,
                EconomyReason.MATCH_INITIALIZATION,
                "tower-fixture-seed"
            )
        )
        return ledger to account
    }

    fun run(): List<FixtureResult> {
        // Successful place -> upgrade -> sell.
        val context = context()
        val (ledger, account) = seededLedger(context)
        val world = FakeBlockWorld()
        val rotation = RotationCache { block, _ -> block }
        val bodyService = TowerBodyMutationService(
            world, context.towerBodyLedger, rotation
        )
        val service = TowerLifecycleService(
            context, ledger, bodyService
        )

        val explicitSpot = context.mapRuntime.explicitTowerSpots
            .first {
                it.team == TeamId.BLUE &&
                    it.footprint == FootprintSize.THREE_BY_THREE
            }

        val place = service.place(
            TowerPlacementCommand(
                TowerInstanceId(100),
                "mage",
                owner,
                TeamId.BLUE,
                explicitSpot.center,
                autoCentre = true,
                path = TowerPath.TOP,
                bodyDefinition = body(
                    "mage",1,null,"mage-1","minecraft:stone"
                ),
                rotation = QuarterTurn.R0,
                transactionId = 2,
                correlationId = "tower-place-100"
            )
        )
        val balanceAfterPlace = ledger.balance(account)
        val placedTower =
            context.entityIndex.towersByInstanceId.getValue(
                TowerInstanceId(100)
            )

        val upgrade = service.upgrade(
            TowerUpgradeCommand(
                TowerInstanceId(100),
                owner,
                newLevel = 2,
                newPath = TowerPath.TOP,
                newBodyDefinition = body(
                    "mage",2,TowerPath.TOP,"mage-2",
                    "minecraft:cobblestone",
                    includeSecondBlock = true
                ),
                rotation = QuarterTurn.R0,
                transactionId = 3,
                correlationId = "tower-upgrade-100-2"
            )
        )
        val balanceAfterUpgrade = ledger.balance(account)

        val sell = service.sell(
            TowerSellCommand(
                TowerInstanceId(100),
                owner,
                owner,
                transactionId = 4,
                correlationId = "tower-sell-100"
            )
        )

        // Placement rollback after synthetic body write failure.
        val rollbackContext = context()
        val (rollbackLedger, rollbackAccount) =
            seededLedger(rollbackContext)
        val rollbackWorld = FakeBlockWorld()
        rollbackWorld.failOnApplyNumber = 2
        val rollbackBodyService = TowerBodyMutationService(
            rollbackWorld,
            rollbackContext.towerBodyLedger,
            RotationCache { b,_ -> b }
        )
        val rollbackService = TowerLifecycleService(
            rollbackContext,
            rollbackLedger,
            rollbackBodyService
        )
        val rollbackSpot =
            rollbackContext.mapRuntime.explicitTowerSpots.first {
                it.team == TeamId.BLUE &&
                    it.footprint == FootprintSize.THREE_BY_THREE
            }

        val placementFailed = try {
            rollbackService.place(
                TowerPlacementCommand(
                    TowerInstanceId(200),
                    "mage",
                    owner,
                    TeamId.BLUE,
                    rollbackSpot.center,
                    true,
                    TowerPath.TOP,
                    body(
                        "mage",1,null,"mage-fail",
                        "minecraft:stone",
                        includeSecondBlock = true
                    ),
                    QuarterTurn.R0,
                    20,
                    "tower-place-rollback"
                )
            )
            false
        } catch (_: Throwable) {
            true
        }

        // Leach team limit preflight.
        val leachContext = context()
        val (leachLedger, _) = seededLedger(leachContext)
        val leachWorld = FakeBlockWorld()
        val leachService = TowerLifecycleService(
            leachContext,
            leachLedger,
            TowerBodyMutationService(
                leachWorld,
                leachContext.towerBodyLedger,
                RotationCache { b,_ -> b }
            )
        )
        val bigSpots = leachContext.mapRuntime.explicitTowerSpots
            .filter {
                it.team == TeamId.BLUE &&
                    it.footprint == FootprintSize.FIVE_BY_FIVE
            }
        val leach1 = leachService.place(
            TowerPlacementCommand(
                TowerInstanceId(300),
                "leach",owner,TeamId.BLUE,
                bigSpots[0].center,true,TowerPath.TOP,
                body("leach",1,null,"leach-1","minecraft:gold_block"),
                QuarterTurn.R0,30,"leach-place-1"
            )
        )
        val secondLeachBlocked = try {
            leachService.place(
                TowerPlacementCommand(
                    TowerInstanceId(301),
                    "leach",owner,TeamId.BLUE,
                    bigSpots[1].center,true,TowerPath.TOP,
                    body("leach",1,null,"leach-1b","minecraft:gold_block"),
                    QuarterTurn.R0,31,"leach-place-2"
                )
            )
            false
        } catch (_: IllegalStateException) {
            true
        }

        return listOf(
            FixtureResult(
                "tower-place-debits-and-registers",
                place.cost == 350L &&
                    balanceAfterPlace == 9650L &&
                    placedTower.investment.currentGameCoinsInvested == 420L /* after later upgrade */
            ),
            FixtureResult(
                "tower-upgrade-debits-and-invests",
                upgrade.upgradeCost == 70L &&
                    upgrade.totalInvested == 420L &&
                    balanceAfterUpgrade == 9580L
            ),
            FixtureResult(
                "tower-sell-60-percent-total-investment",
                sell.refund == 252L &&
                    ledger.balance(account) == 9832L &&
                    context.entityIndex.towersByInstanceId.isEmpty() &&
                    context.towerBodyLedger.isEmpty()
            ),
            FixtureResult(
                "tower-body-restores-pre-tower-world",
                world.blocks.values.all {
                    it.blockData == "minecraft:air"
                }
            ),
            FixtureResult(
                "tower-placement-failure-compensates-and-rolls-back",
                placementFailed &&
                    rollbackLedger.balance(rollbackAccount) == 10000L &&
                    rollbackContext.entityIndex.towersByInstanceId.isEmpty() &&
                    rollbackContext.towerBodyLedger.isEmpty() &&
                    rollbackWorld.blocks.values.all {
                        it.blockData == "minecraft:air"
                    }
            ),
            FixtureResult(
                "leach-team-limit-one",
                leach1.cost == 3000L && secondLeachBlocked
            )
        )
    }
}
