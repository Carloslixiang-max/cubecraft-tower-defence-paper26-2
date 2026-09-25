package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.match.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.progression.*
import dev.cubecrafttd.truth.*
import java.util.UUID

object MatchModePresetFixture {
    private fun <T> observed(
        value: T
    )=ResolvedTruth(
        value,
        ResolutionSource
            .OBSERVED_ORIGINAL
    )

    fun run(): List<FixtureResult> {
        val normal=
            MatchRulePresetFactory
                .recommendedMature(
                    MatchMode.NORMAL,
                    observed(500L),
                    observed(0L)
                )
        val doubleIncome=
            MatchRulePresetFactory
                .recommendedMature(
                    MatchMode.DOUBLE_INCOME,
                    observed(500L),
                    observed(0L)
                )
        val quick=
            MatchRulePresetFactory
                .recommendedMature(
                    MatchMode.QUICK_START,
                    observed(500L),
                    observed(0L)
                )

        val classic=
            TroopProgressionState()
        val all=
            TroopProgressionState()
        val progression=
            TroopProgressionService(
                EconomyLedger()
            )
        progression.initialize(
            classic,
            ProgressionMode
                .CLASSIC_PROGRESSION
        )
        progression.initialize(
            all,
            ProgressionMode.ALL_UNLOCKED
        )

        val player=
            UUID.fromString(
                "00000000-0000-0000-0000-000000009100"
            )
        val ledger=EconomyLedger()
        val exp=EconomyAccount(
            TeamId.RED,player,
            EconomyCurrency.MATCH_EXP
        )
        ledger.apply(
            EconomyTransaction(
                1,0,exp,1000,
                EconomyReason.MATCH_INITIALIZATION,
                "mode-exp"
            )
        )
        val mine=GoldmineRuntime(
            player,TeamId.RED,1,
            GoldmineIncomeMode.NORMAL,
            nextIncomeTick=20
        )
        val upgraded=
            GoldmineUpgradeService(ledger)
                .upgrade(
                    mine,10,2,
                    "mine-l2"
                )

        val killLedger=EconomyLedger()
        val mob=MobRuntimeState(
            MobIdentity(
                MobInstanceId(99),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000009199"
                ),
                "zombie",player,
                TeamId.RED,1
            ),
            MobRouteState(
                "r",0,0.0,0.0
            ),
            MobCombatState(
                0.0,40.0,
                MobLifecycleState.DEAD
            )
        )
        val attribution=
            dev.cubecrafttd.combat
                .KillAttributionService
                .matureFinalBlow(
                    DamageSourceIdentity
                        .PlayerSword(player)
                )
        val reward=
            MobKillRewardService(
                killLedger,
                MobKillRewardResolver {
                    _,_ -> observed(15L)
                },
                PricingMode.DOUBLE_INCOME
            ).award(
                mob,attribution,50,3
            )

        return listOf(
            FixtureResult(
                "mode-normal-balance-and-income",
                normal.startingBalance.coins==
                    500L &&
                    normal.goldmineIncomeMode==
                        GoldmineIncomeMode.NORMAL &&
                    normal.mobKillCoinMultiplier==
                        1L
            ),
            FixtureResult(
                "mode-double-income-multipliers",
                doubleIncome
                    .goldmineIncomeMode==
                    GoldmineIncomeMode
                        .DOUBLE_INCOME &&
                    doubleIncome
                        .mobKillCoinMultiplier==
                        2L &&
                    doubleIncome
                        .sentMobExpMultiplier==
                        2L
            ),
            FixtureResult(
                "mode-quick-start-balance",
                quick.startingBalance.coins==
                    1500L &&
                    quick.startingBalance.exp==
                        100L
            ),
            FixtureResult(
                "progression-classic-vs-all-unlocked",
                classic.level("zombie")==1 &&
                    classic.level("giant")==0 &&
                    all.unlockedLevelByMob
                        .values.all{it==5}
            ),
            FixtureResult(
                "goldmine-upgrade-exp-debit",
                upgraded.level==2 &&
                    mine.level==2 &&
                    ledger.balance(exp)==875L
            ),
            FixtureResult(
                "double-income-kill-coins-x2",
                reward==30L
            )
        )
    }
}
