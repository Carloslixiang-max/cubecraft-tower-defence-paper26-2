package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.match.*
import dev.cubecrafttd.paper.PaperBlockWorldAdapter
import dev.cubecrafttd.progression.TroopProgressionState
import dev.cubecrafttd.tower.TowerInstanceId
import dev.cubecrafttd.tower.lifecycle.*
import dev.cubecrafttd.tower.visual.*
import dev.cubecrafttd.ui.*
import java.util.UUID

private class PlacementFlowFakeWorld : PaperBlockWorldAdapter {
    private val blocks=linkedMapOf<BlockKey,BlockSnapshot>()
    override fun snapshot(block: BlockKey)=
        blocks[block] ?: BlockSnapshot("minecraft:air")
    override fun apply(block: BlockKey,snapshot: BlockSnapshot,applyPhysics: Boolean) {
        blocks[block]=snapshot
    }
    override fun currentBlockData(block: BlockKey)=snapshot(block).blockData
}

object TowerPlacementAndRangefinderFixture {
    fun run():List<FixtureResult> {
        val player=UUID.fromString("00000000-0000-0000-0000-000000034001")
        val context=ArenaContext(
            ArenaId("placement-flow"),
            UUID.fromString("00000000-0000-0000-0000-000000034100"),
            TestingMapFactory.minimal()
        ).also {
            it.redTeam.players += player
            it.state=ArenaState.RUNNING
        }
        val ledger=EconomyLedger()
        RecommendedMaturePricing.seedPlayer(
            ledger,TeamId.RED,player,
            MatchStartingBalance(50_000,0),1,"seed"
        )
        val playerState=PlayerMatchSessionState(
            player,TroopProgressionState()
        )
        val session=MatchSessionState(
            MatchRulePreset(
                MatchMode.NORMAL,PricingMode.NORMAL,
                GoldmineIncomeMode.NORMAL,
                dev.cubecrafttd.progression.ProgressionMode.ALL_UNLOCKED,
                MatchStartingBalance(50_000,0),1,1
            ),
            linkedMapOf(player to playerState)
        )
        val lifecycle=TowerLifecycleService(
            context,ledger,
            TowerBodyMutationService(
                PlacementFlowFakeWorld(),
                context.towerBodyLedger,
                RotationCache { data,_ -> data }
            )
        )
        val world=MatchTowerWorldActionService(
            context,session,lifecycle,
            EngineeringPlaceholderTowerBodyProvider,
            EngineeringFixedR0RotationPolicy
        )
        val flow=TowerPlacementInteractionService(session,world)

        val begin=flow.beginRegular(player,BlockPos(0,0,2))
        val choose=flow.chooseTower(player,"archer")
        val placed=flow.choosePathAndPlace(player,TowerPath.TOP)

        val ids=(1L..12L).associate {
            TowerInstanceId(it) to it.toDouble()
        }
        val visible=TowerRangefinderRuntime.visibleTowerIds(
            TowerRangefinderQuery(
                hoveredTower=null,
                sneaking=true,
                distanceSquaredByTower=ids,
                pinnedTowerIds=setOf(TowerInstanceId(12))
            )
        )

        return listOf(
            FixtureResult(
                "placement-regular-opens-combined-builder",
                begin.menu.title=="Tower builder" &&
                    begin.menu.size==45
            ),
            FixtureResult(
                "placement-tower-choice-requires-path-step",
                choose.towerId=="archer" &&
                    choose.menu.title=="Select an upgrade path"
            ),
            FixtureResult(
                "placement-path-commit-builds-at-pending-click",
                placed.result.selection.towerId=="archer" &&
                    placed.result.selection.path==TowerPath.TOP
            ),
            FixtureResult(
                "rangefinder-shift-nearest-ten-plus-pinned",
                visible.containsAll((1L..10L).map { TowerInstanceId(it) }) &&
                    TowerInstanceId(12) in visible &&
                    visible.size==11
            ),
            FixtureResult(
                "hotbar-universal-default-no-longer-claimed-original",
                HotbarLayout.ENGINEERING_RUNTIME_DEFAULT.evidence==
                    HotbarLayoutEvidence.ENGINEERING_RUNTIME_DEFAULT &&
                    HotbarLayout.OFFICIAL_2021_SCREENSHOT_EXAMPLE.evidence==
                    HotbarLayoutEvidence.OFFICIAL_2021_SCREENSHOT_EXAMPLE
            )
        )
    }
}
