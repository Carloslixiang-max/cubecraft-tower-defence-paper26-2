package dev.cubecrafttd.arena

enum class ArenaPerformanceGateStatus {
    NOT_READY,
    PASS,
    FAIL
}

data class ArenaPerformanceGatePolicy(
    val minimumTowerCount: Int = 60,
    val minimumProfiledTicks: Long = 1_200L,
    val maximumAverageTickNanos: Long =
        10_000_000L,
    val hardTickBudgetNanos: Long =
        50_000_000L
) {
    init {
        require(minimumTowerCount>0)
        require(minimumProfiledTicks>0L)
        require(maximumAverageTickNanos>0L)
        require(hardTickBudgetNanos>0L)
        require(
            maximumAverageTickNanos <
                hardTickBudgetNanos
        )
    }
}

data class ArenaPerformanceGateResult(
    val status:
        ArenaPerformanceGateStatus,
    val reasons: List<String>,
    val towerCount: Int,
    val profiledTicks: Long,
    val averageTickNanos: Long,
    val maximumTickNanos: Long,
    val slowTicksOver50ms: Long
) {
    val passed: Boolean
        get() =
            status==
                ArenaPerformanceGateStatus
                    .PASS
}

/**
 * Engineering-only real-server stress acceptance gate.
 *
 * These thresholds are not asserted as CubeCraft gameplay truth. They reserve
 * most of Minecraft's 50 ms tick budget for the server/other plugins while
 * requiring a sustained >=60-tower live sample before performance evidence can
 * be recorded.
 */
object EngineeringArenaPerformanceGate {
    val DEFAULT_POLICY=
        ArenaPerformanceGatePolicy()

    fun evaluate(
        towerCount: Int,
        liveProfile:
            ArenaTickProfileSnapshot,
        policy:
            ArenaPerformanceGatePolicy =
            DEFAULT_POLICY
    ): ArenaPerformanceGateResult {
        val readiness=
            mutableListOf<String>()

        if(
            towerCount <
                policy.minimumTowerCount
        ) {
            readiness +=
                "requires >=${policy.minimumTowerCount} towers (have ${towerCount})"
        }
        if(
            liveProfile.ticks <
                policy.minimumProfiledTicks
        ) {
            readiness +=
                "requires >=${policy.minimumProfiledTicks} profiled ticks (have ${liveProfile.ticks})"
        }

        if(readiness.isNotEmpty()) {
            return ArenaPerformanceGateResult(
                status=
                    ArenaPerformanceGateStatus
                        .NOT_READY,
                reasons=readiness,
                towerCount=towerCount,
                profiledTicks=
                    liveProfile.ticks,
                averageTickNanos=
                    liveProfile
                        .averageNanos,
                maximumTickNanos=
                    liveProfile.maxNanos,
                slowTicksOver50ms=
                    liveProfile
                        .slowTicksOver50ms
            )
        }

        val failures=
            mutableListOf<String>()
        if(
            liveProfile.averageNanos >
                policy.maximumAverageTickNanos
        ) {
            failures +=
                "average live TD tick exceeds ${policy.maximumAverageTickNanos} ns"
        }
        if(
            liveProfile.maxNanos >=
                policy.hardTickBudgetNanos
        ) {
            failures +=
                "maximum live TD tick reached/exceeded ${policy.hardTickBudgetNanos} ns"
        }
        if(
            liveProfile.slowTicksOver50ms >
                0L
        ) {
            failures +=
                "observed ${liveProfile.slowTicksOver50ms} live TD tick(s) >=50 ms"
        }

        return ArenaPerformanceGateResult(
            status=
                if(failures.isEmpty())
                    ArenaPerformanceGateStatus
                        .PASS
                else
                    ArenaPerformanceGateStatus
                        .FAIL,
            reasons=failures,
            towerCount=towerCount,
            profiledTicks=
                liveProfile.ticks,
            averageTickNanos=
                liveProfile.averageNanos,
            maximumTickNanos=
                liveProfile.maxNanos,
            slowTicksOver50ms=
                liveProfile
                    .slowTicksOver50ms
        )
    }
}
