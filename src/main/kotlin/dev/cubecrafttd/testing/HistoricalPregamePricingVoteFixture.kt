package dev.cubecrafttd.testing

import dev.cubecrafttd.economy.PricingMode
import dev.cubecrafttd.match.*
import java.util.UUID

object HistoricalPregamePricingVoteFixture {
    fun run(): List<FixtureResult> {
        val red=UUID.fromString(
            "00000000-0000-0000-0000-000000056001"
        )
        val blue=UUID.fromString(
            "00000000-0000-0000-0000-000000056002"
        )
        val outsider=UUID.fromString(
            "00000000-0000-0000-0000-000000056003"
        )

        fun runtime()=
            HistoricalPregamePricingVoteRuntime(
                setOf(red,blue)
            )

        val noVotes=
            runtime().resolve()

        val doubleRuntime=runtime()
        doubleRuntime.cast(
            red,
            HistoricalPregamePricingVoteOption
                .DOUBLE_INCOME
        )
        doubleRuntime.cast(
            blue,
            HistoricalPregamePricingVoteOption
                .DOUBLE_INCOME
        )
        val double=
            doubleRuntime.resolve()

        val quickRuntime=runtime()
        quickRuntime.cast(
            red,
            HistoricalPregamePricingVoteOption
                .QUICK_START
        )
        val quick=
            quickRuntime.resolve()

        val tieRuntime=runtime()
        tieRuntime.cast(
            red,
            HistoricalPregamePricingVoteOption
                .DOUBLE_INCOME
        )
        tieRuntime.cast(
            blue,
            HistoricalPregamePricingVoteOption
                .QUICK_START
        )
        val tie=
            tieRuntime.resolve()

        val outsiderRejected=
            runCatching {
                runtime().cast(
                    outsider,
                    HistoricalPregamePricingVoteOption
                        .NORMAL
                )
            }.isFailure

        return listOf(
            FixtureResult(
                "historical-pregame-pricing-no-votes-normal",
                noVotes.pricingMode==
                    PricingMode.NORMAL &&
                    noVotes.resolution==
                        HistoricalPregamePricingResolution
                            .NO_VOTES_NORMAL
            ),
            FixtureResult(
                "historical-pregame-pricing-double-income-vote",
                double.pricingMode==
                    PricingMode.DOUBLE_INCOME &&
                    double.resolution==
                        HistoricalPregamePricingResolution
                            .UNIQUE_HIGHEST
            ),
            FixtureResult(
                "historical-pregame-pricing-quick-start-vote",
                quick.pricingMode==
                    PricingMode.QUICK_START &&
                    quick.winningOption==
                        HistoricalPregamePricingVoteOption
                            .QUICK_START
            ),
            FixtureResult(
                "historical-pregame-pricing-tie-engineering-normal-fallback",
                tie.pricingMode==
                    PricingMode.NORMAL &&
                    tie.resolution==
                        HistoricalPregamePricingResolution
                            .ENGINEERING_TIE_NORMAL_FALLBACK
            ),
            FixtureResult(
                "historical-pregame-pricing-rejects-outsider",
                outsiderRejected
            )
        )
    }
}
