package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.match.*
import dev.cubecrafttd.progression.*
import dev.cubecrafttd.ui.*
import java.util.UUID

object MatchMenuActionRouterFixture {
    fun run():List<FixtureResult> {
        val red=UUID.fromString(
            "00000000-0000-0000-0000-000000029001"
        )
        val blue=UUID.fromString(
            "00000000-0000-0000-0000-000000029002"
        )
        val context=ArenaContext(
            ArenaId("menu-router"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000029100"
            ),
            TestingMapFactory.minimal()
        ).also {
            it.redTeam.players += red
            it.blueTeam.players += blue
            it.state=ArenaState.RUNNING
            it.gameTick=100
        }

        val ledger=EconomyLedger()
        val runtime=
            NormalArenaRuntimeState
                .withCadence(
                    dev.cubecrafttd.troop
                        .TroopSpawnCadence(
                            5,"fixture"
                        )
                )
        runtime.goldmines[red]=
            GoldmineRuntime(
                red,TeamId.RED,1,
                GoldmineIncomeMode.NORMAL,
                120
            )
        val progressionService=
            TroopProgressionService(
                ledger
            )
        val progression=
            TroopProgressionState()
        progressionService.initialize(
            progression
        )
        val session=MatchSessionState(
            MatchRulePreset(
                MatchMode.NORMAL,
                PricingMode.NORMAL,
                GoldmineIncomeMode.NORMAL,
                ProgressionMode
                    .CLASSIC_PROGRESSION,
                MatchStartingBalance(
                    5000,2000
                ),
                1,1
            ),
            linkedMapOf(
                red to
                    PlayerMatchSessionState(
                        red,progression
                    )
            )
        )

        RecommendedMaturePricing
            .seedPlayer(
                ledger,TeamId.RED,red,
                MatchStartingBalance(
                    5000,2000
                ),
                1,"seed-red"
            )

        val router=
            MatchMenuActionRouter(
                context,runtime,session,
                ledger,progressionService,
                initialSequence=1000
            )

        val tower=
            router.handle(
                MenuActionInvocation(
                    red,
                    "tower:archer",
                    ClickKind.RIGHT
                )
            ) as
                MatchMenuActionResult
                    .TowerSelected

        // Zombie L1 is initially unlocked.
        router.handle(
            MenuActionInvocation(
                red,
                "summoner:mob:zombie:l1",
                ClickKind.LEFT
            )
        )
        router.handle(
            MenuActionInvocation(
                red,
                "summoner:mob:zombie:l1",
                ClickKind.SHIFT_LEFT
            )
        )
        val sent=
            router.handle(
                MenuActionInvocation(
                    red,
                    "summoner:send",
                    ClickKind.LEFT
                )
            ) as
                MatchMenuActionResult
                    .SummonerBatchSent

        val progressionResult=
            router.handle(
                MenuActionInvocation(
                    red,
                    "progression:upgrade:spider",
                    ClickKind.LEFT
                )
            ) as
                MatchMenuActionResult
                    .ProgressionChanged

        val mine=
            router.handle(
                MenuActionInvocation(
                    red,
                    "bazaar:goldmine:upgrade",
                    ClickKind.LEFT
                )
            ) as
                MatchMenuActionResult
                    .GoldmineUpgraded

        return listOf(
            FixtureResult(
                "menu-router-right-click-selects-bottom-path",
                tower.selection.path==
                    dev.cubecrafttd.tower
                        .visual.TowerPath.BOTTOM
            ),
            FixtureResult(
                "menu-router-summoner-commit",
                sent.receipt.totalUnits==
                    12 &&
                    runtime.queues
                        .getValue(
                            TeamId.BLUE
                        ).usedUnits()==12
            ),
            FixtureResult(
                "menu-router-progression-upgrade",
                progressionResult.mobId==
                    "spider" &&
                    progressionResult.level==1
            ),
            FixtureResult(
                "menu-router-goldmine-upgrade",
                mine.level==2 &&
                    runtime.goldmines
                        .getValue(red)
                        .level==2
            )
        )
    }
}
