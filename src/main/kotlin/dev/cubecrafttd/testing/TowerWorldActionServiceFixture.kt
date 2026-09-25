package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.map.*
import dev.cubecrafttd.match.*
import dev.cubecrafttd.progression.*
import dev.cubecrafttd.paper.PaperBlockWorldAdapter
import dev.cubecrafttd.tower.lifecycle.*
import dev.cubecrafttd.tower.visual.*
import dev.cubecrafttd.ui.*
import java.util.UUID


private class TowerWorldActionFakeBlockWorld :
    PaperBlockWorldAdapter {
    private val blocks=
        linkedMapOf<
            BlockKey,
            BlockSnapshot
        >()

    override fun snapshot(
        block: BlockKey
    ): BlockSnapshot =
        blocks[block]
            ?: BlockSnapshot(
                "minecraft:air"
            )

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

object TowerWorldActionServiceFixture {
    fun run():List<FixtureResult> {
        val player=UUID.fromString(
            "00000000-0000-0000-0000-000000030001"
        )
        val context=ArenaContext(
            ArenaId("tower-world-action"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000030100"
            ),
            TestingMapFactory.minimal()
        ).also {
            it.redTeam.players += player
            it.state=ArenaState.RUNNING
        }
        val ledger=EconomyLedger()
        RecommendedMaturePricing.seedPlayer(
            ledger,TeamId.RED,player,
            MatchStartingBalance(
                10_000,0
            ),
            1,"seed"
        )
        val session=MatchSessionState(
            MatchRulePreset(
                MatchMode.NORMAL,
                PricingMode.NORMAL,
                GoldmineIncomeMode.NORMAL,
                ProgressionMode
                    .CLASSIC_PROGRESSION,
                MatchStartingBalance(
                    10_000,0
                ),
                1,1
            ),
            linkedMapOf(
                player to
                    PlayerMatchSessionState(
                        player,
                        TroopProgressionState()
                    )
            )
        )

        val world=TowerWorldActionFakeBlockWorld()
        val mutation=
            TowerBodyMutationService(
                world,
                context.towerBodyLedger,
                RotationCache {
                    blockData,_ ->
                    blockData
                }
            )
        val lifecycle=
            TowerLifecycleService(
                context,ledger,mutation
            )
        val actions=
            MatchTowerWorldActionService(
                context,session,lifecycle,
                EngineeringPlaceholderTowerBodyProvider,
                EngineeringFixedR0RotationPolicy,
                initialSequence=100
            )

        session.players.getValue(player)
            .interaction.builder
            .select(
                "archer",
                TowerPath.TOP
            )

        val beforeQuick=
            session.players.getValue(player)
                .interaction.builder
                .quickPlaceSelection()

        val placed=actions.place(
            player,
            BlockPos(0,0,2),
            quickPlace=false
        )
        val afterQuick=
            session.players.getValue(player)
                .interaction.builder
                .quickPlaceSelection()

        return listOf(
            FixtureResult(
                "tower-builder-selection-not-yet-last-placed",
                beforeQuick==null
            ),
            FixtureResult(
                "tower-world-place-confirms-quick-place-memory",
                afterQuick?.towerId==
                    "archer" &&
                    afterQuick.path==
                        TowerPath.TOP
            ),
            FixtureResult(
                "tower-placeholder-explicit-evidence",
                placed.bodyEvidence==
                    BodyEvidenceStatus
                        .ENGINEERING_PLACEHOLDER
            ),
            FixtureResult(
                "tower-world-place-registers-runtime",
                context.entityIndex
                    .towersByInstanceId
                    .containsKey(
                        placed.receipt
                            .towerInstanceId
                    )
            )
        )
    }
}
