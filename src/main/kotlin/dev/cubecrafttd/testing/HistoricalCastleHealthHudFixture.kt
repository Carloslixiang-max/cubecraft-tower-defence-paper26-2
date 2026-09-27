package dev.cubecrafttd.testing

import dev.cubecrafttd.ui.HistoricalCastleHealthHudProjector

object HistoricalCastleHealthHudFixture {
    fun run(): List<FixtureResult> {
        val half=
            HistoricalCastleHealthHudProjector
                .project(
                    10.0,20.0
                )
        val clampedLow=
            HistoricalCastleHealthHudProjector
                .project(
                    -5.0,20.0
                )
        val clampedHigh=
            HistoricalCastleHealthHudProjector
                .project(
                    30.0,20.0
                )

        return listOf(
            FixtureResult(
                "historical-castle-health-hud-title-and-ratio",
                half.title==
                    "Castle Health" &&
                    half.progress==0.5
            ),
            FixtureResult(
                "historical-castle-health-hud-progress-is-bossbar-safe",
                clampedLow.progress==0.0 &&
                    clampedHigh.progress==1.0
            )
        )
    }
}
