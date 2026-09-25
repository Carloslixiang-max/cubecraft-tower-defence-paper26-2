package dev.cubecrafttd.match

import dev.cubecrafttd.economy.*
import dev.cubecrafttd.progression.ProgressionMode
import dev.cubecrafttd.truth.ResolvedTruth

enum class MatchMode {
    NORMAL,
    DOUBLE_INCOME,
    QUICK_START
}

data class MatchRulePreset(
    val mode: MatchMode,
    val pricingMode: PricingMode,
    val goldmineIncomeMode:
        GoldmineIncomeMode,
    val progressionMode:
        ProgressionMode,
    val startingBalance:
        MatchStartingBalance,
    val mobKillCoinMultiplier: Long,
    val sentMobExpMultiplier: Long
) {
    init {
        require(mobKillCoinMultiplier > 0L)
        require(sentMobExpMultiplier > 0L)
    }
}

object MatchRulePresetFactory {
    fun recommendedMature(
        mode: MatchMode,
        normalStartCoins:
            ResolvedTruth<Long>,
        normalStartExp:
            ResolvedTruth<Long>,
        progressionMode:
            ProgressionMode =
            ProgressionMode.CLASSIC_PROGRESSION
    ): MatchRulePreset {
        val pricing=when(mode) {
            MatchMode.NORMAL ->
                PricingMode.NORMAL
            MatchMode.DOUBLE_INCOME ->
                PricingMode.DOUBLE_INCOME
            MatchMode.QUICK_START ->
                PricingMode.QUICK_START
        }

        return MatchRulePreset(
            mode=mode,
            pricingMode=pricing,
            goldmineIncomeMode=
                if(mode==
                    MatchMode.DOUBLE_INCOME)
                    GoldmineIncomeMode
                        .DOUBLE_INCOME
                else
                    GoldmineIncomeMode.NORMAL,
            progressionMode=progressionMode,
            startingBalance=
                RecommendedMaturePricing
                    .startingBalance(
                        pricing,
                        normalStartCoins,
                        normalStartExp
                    ),
            mobKillCoinMultiplier=
                if(mode==
                    MatchMode.DOUBLE_INCOME)
                    2L else 1L,
            sentMobExpMultiplier=
                if(mode==
                    MatchMode.DOUBLE_INCOME)
                    2L else 1L
        )
    }
}
