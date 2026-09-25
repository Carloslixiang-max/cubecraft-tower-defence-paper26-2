package dev.cubecrafttd.testing

import dev.cubecrafttd.mob.WitchHealMode
import dev.cubecrafttd.truth.*

object EconomyEntryTruthFixture {
    fun run(): List<FixtureResult> {
        val empty=
            RuntimeFallbackConfigV1()
        val missing=
            RuntimeFallbackValidator
                .validateForNormal(empty)

        val configured=
            RuntimeFallbackConfigV1(
                normalStartingCoins=800,
                normalStartingExp=0,
                goldmineFirstIncomeDelayTicks=20,
                troopSpawnCadenceTicks=20,
                castleFirstHitDelayTicks=20,
                castleOrdinaryAttackIntervalTicks=40,
                guardDamagePerArrow=5.0,
                guardFireIntervalTicks=20,
                guardRangeBlocks=10.0,
                guardTargetPriority=
                    dev.cubecrafttd.castle
                        .GuardTargetPriorityPolicy.FIRST,
                witchHealAmount=10.0,
                witchHealIntervalTicks=40,
                witchHealRadiusBlocks=8.0,
                witchHealMode=
                    WitchHealMode.MAX_HEALTH_FRACTION,
                witchHealTargetCap=8,
                creeperRegenHealthPerSecond=5.0,
                giantRegenHealthPerSecond=5.0,
                giantRunSpeedMultiplier=1.2,
                slimeMaxShrinkPhaseIndex=2,
                leachMaxCharge=100.0,
                leachChargePerTargetTick=1.0
            )
        val engineering=
            configured
                .toEngineeringFallbackConfig()
        val gate=TruthGate(engineering)

        val coins=
            gate.resolveWithExplicitFallback(
                TruthField<Long>(
                    "match.normalStartingCoins",
                    null,
                    EvidenceStatus.VIDEO_REQUIRED
                )
            )
        val delay=
            gate.resolveWithExplicitFallback(
                TruthField<Long>(
                    "goldmine.firstIncomeDelayTicks",
                    null,
                    EvidenceStatus.VIDEO_REQUIRED
                )
            )

        return listOf(
            FixtureResult(
                "economy-entry-truth-empty-config-blocks-normal",
                missing.any {
                    it.key==
                        "match.normalStartingCoins"
                } &&
                    missing.any {
                        it.key==
                            "match.normalStartingExp"
                    } &&
                    missing.any {
                        it.key==
                            "goldmine.firstIncomeDelayTicks"
                    }
            ),
            FixtureResult(
                "economy-entry-truth-explicit-fallback-adapts",
                coins.value==800L &&
                    delay.value==20L &&
                    coins.source==
                        ResolutionSource
                            .ENGINEERING_FALLBACK
            )
        )
    }
}
