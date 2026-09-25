package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.castle.GuardTargetPriorityPolicy
import dev.cubecrafttd.match.NormalGameplayConfigResolver
import dev.cubecrafttd.mob.WitchHealMode
import dev.cubecrafttd.tower.RecommendedMatureTowerDefinitions
import dev.cubecrafttd.truth.*

object ResolvedSupportCompositionFixture {
    private fun completeConfig() =
        CompleteFallbackFixtureFactory.create()


    fun run(): List<FixtureResult> {
        val resolved =
            NormalGameplayConfigResolver
                .resolve(completeConfig())

        val phaseOrders =
            RecommendedNormalArenaPhasePlan
                .entries.map { it.order }

        return listOf(
            FixtureResult(
                "normal-support-phase-order",
                phaseOrders ==
                    listOf(20,30,40,50,55,57,60,70,80,90)
            ),
            FixtureResult(
                "normal-support-config-resolved",
                resolved.witchHeal
                    .mode.value ==
                    WitchHealMode
                        .MAX_HEALTH_FRACTION &&
                    resolved.witchHeal
                        .healValue.value == 0.10 &&
                    resolved.creeperRegen
                        .healthPerSecond.value == 5.0 &&
                    resolved.giantRegen
                        .healthPerSecond.value == 5.0 &&
                    resolved.giantRunSpeedMultiplier
                        .value == 1.2 &&
                    resolved.slimeLethal
                        .maxShrinkPhaseIndex.value == 2
            ),
            FixtureResult(
                "normal-resolver-uses-full-completeness-gate",
                runCatching {
                    NormalGameplayConfigResolver
                        .resolve(
                            RuntimeFallbackConfigV1(
                                normalStartingCoins=800,
                                normalStartingExp=0,
                                goldmineFirstIncomeDelayTicks=20,
                                troopSpawnCadenceTicks=5
                            )
                        )
                }.isFailure
            )
        )
    }
}
