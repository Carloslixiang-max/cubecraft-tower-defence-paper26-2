package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*

object ArenaPerformanceGateFixture {
    private fun profile(
        ticks: Long,
        averageNanos: Long,
        maxNanos: Long,
        slowTicks: Long
    ): ArenaTickProfileSnapshot =
        ArenaTickProfileSnapshot(
            ticks=ticks,
            totalNanos=
                Math.multiplyExact(
                    ticks,
                    averageNanos
                ),
            lastNanos=
                averageNanos,
            maxNanos=
                maxNanos,
            slowTicksOver50ms=
                slowTicks,
            phases=emptyMap()
        )

    fun run(): List<FixtureResult> {
        val healthy=
            profile(
                ticks=1_200L,
                averageNanos=5_000_000L,
                maxNanos=20_000_000L,
                slowTicks=0L
            )

        val pass=
            EngineeringArenaPerformanceGate
                .evaluate(
                    60,
                    healthy
                )
        val towerNotReady=
            EngineeringArenaPerformanceGate
                .evaluate(
                    59,
                    healthy
                )
        val tickNotReady=
            EngineeringArenaPerformanceGate
                .evaluate(
                    60,
                    profile(
                        ticks=1_199L,
                        averageNanos=
                            5_000_000L,
                        maxNanos=
                            20_000_000L,
                        slowTicks=0L
                    )
                )
        val overloaded=
            EngineeringArenaPerformanceGate
                .evaluate(
                    64,
                    profile(
                        ticks=1_200L,
                        averageNanos=
                            12_000_000L,
                        maxNanos=
                            60_000_000L,
                        slowTicks=2L
                    )
                )

        return listOf(
            FixtureResult(
                "performance-gate-passes-sustained-60-tower-sample",
                pass.passed &&
                    pass.reasons.isEmpty()
            ),
            FixtureResult(
                "performance-gate-requires-at-least-60-towers",
                towerNotReady.status==
                    ArenaPerformanceGateStatus
                        .NOT_READY
            ),
            FixtureResult(
                "performance-gate-requires-sustained-profile-window",
                tickNotReady.status==
                    ArenaPerformanceGateStatus
                        .NOT_READY
            ),
            FixtureResult(
                "performance-gate-rejects-live-tick-budget-regression",
                overloaded.status==
                    ArenaPerformanceGateStatus
                        .FAIL &&
                    overloaded.reasons.size==3
            )
        )
    }
}
