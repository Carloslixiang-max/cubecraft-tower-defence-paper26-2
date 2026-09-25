package dev.cubecrafttd.testing

import dev.cubecrafttd.mob.*
import dev.cubecrafttd.status.*
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.truth.*

object MobPhaseSupportAndTowerFixture {
    fun run(): List<FixtureResult> {
        val spiderL3 = RecommendedMatureMobFormResolver.initialForm("spider", 3)
        val spiderL4 = RecommendedMatureMobFormResolver.initialForm("spider", 4)
        val skeletonL4 = RecommendedMatureMobFormResolver.initialForm("skeleton", 4)
        val silverfishL4 = RecommendedMatureMobFormResolver.initialForm("silverfish", 4)
        val slimeL4 = RecommendedMatureMobFormResolver.initialForm("slime", 4)

        val shrink = SlimeShrinkPhaseService.shrink(
            current = slimeL4,
            currentMaxHealth = 1000.0,
            currentHealth = 500.0,
            maxPhaseIndexInclusive = 2
        )

        val giantAbove = GiantPhaseService.runState(300.0, 1000.0)
        val giantAt = GiantPhaseService.runState(200.0, 1000.0)

        val gate = TruthGate(
            EngineeringFallbackConfig(
                mapOf(
                    "witch.healAmount" to 0.10,
                    "witch.healIntervalTicks" to 40L,
                    "witch.healRadiusBlocks" to 8.0,
                    "witch.healTargetCap" to 8,
                    "witch.healMode" to WitchHealMode.MAX_HEALTH_FRACTION,
                    "giant.regenHealthPerSecond" to 5.0
                )
            )
        )
        fun <T> unresolved(key: String): TruthField<T> = TruthField(
            key, null, EvidenceStatus.VIDEO_REQUIRED
        )

        val witchConfig = WitchHealConfig(
            gate.resolveWithExplicitFallback(unresolved("witch.healAmount")),
            gate.resolveWithExplicitFallback(unresolved("witch.healIntervalTicks")),
            gate.resolveWithExplicitFallback(unresolved("witch.healRadiusBlocks")),
            gate.resolveWithExplicitFallback(unresolved("witch.healTargetCap")),
            gate.resolveWithExplicitFallback(unresolved("witch.healMode"))
        )
        val witchClock = WitchHealClock(witchConfig)
        witchClock.arm(100)

        val giantConfig = GiantRegenConfig(
            gate.resolveWithExplicitFallback(unresolved("giant.regenHealthPerSecond"))
        )
        val regen = GiantRegenerationService.regenerate(
            currentHealth = 100.0,
            maxHealth = 200.0,
            config = giantConfig,
            ticksElapsed = 20
        )

        val effects = StatusEffectSet(RecommendedMatureStatusPolicies.policies)
        effects.apply(StatusEffectInstance(
            StatusEffectType.ICE_SLOW, "ice-a", 0.2, 0, 100
        ))
        effects.apply(StatusEffectInstance(
            StatusEffectType.ICE_SLOW, "ice-b", 0.5, 10, 50
        ))
        effects.apply(StatusEffectInstance(
            StatusEffectType.STUN, "quake-a", 1.0, 0, 40
        ))
        effects.apply(StatusEffectInstance(
            StatusEffectType.STUN, "quake-b", 1.0, 10, 80
        ))

        val towers = RecommendedMatureTowerDefinitions.all()

        return listOf(
            FixtureResult(
                "mob-form-level-variants",
                spiderL3.formId == "spider" &&
                    spiderL4.formId == "cave_spider" &&
                    skeletonL4.formId == "wither_skeleton" &&
                    silverfishL4.formId == "endermite" &&
                    slimeL4.formId == "magma_cube"
            ),
            FixtureResult(
                "slime-shrink-25-percent-max-health",
                shrink.nextMaxHealth == 750.0 &&
                    shrink.nextHealth == 375.0 &&
                    shrink.nextForm.phaseIndex == 1
            ),
            FixtureResult(
                "giant-run-threshold-20-percent",
                !giantAbove.running && giantAt.running
            ),
            FixtureResult(
                "witch-truth-gated-clock",
                !witchClock.due(139) &&
                    witchClock.due(140) &&
                    witchClock.consume(140)
            ),
            FixtureResult(
                "giant-regen-truth-gated",
                regen == 105.0 &&
                    giantConfig.healthPerSecond.source ==
                        ResolutionSource.ENGINEERING_FALLBACK
            ),
            FixtureResult(
                "status-effect-slow-non-stacking-keeps-existing",
                effects.get(StatusEffectType.ICE_SLOW)?.magnitude == 0.2 &&
                    effects.get(StatusEffectType.ICE_SLOW)?.expireTick == 100L
            ),
            FixtureResult(
                "status-effect-stun-non-stacking-keeps-existing",
                effects.get(StatusEffectType.STUN)?.expireTick == 40L
            ),
            FixtureResult(
                "tower-repository-eleven-families",
                towers.size == 11
            ),
            FixtureResult(
                "tower-footprint-locks",
                towers.getValue("necromancer").footprint == TowerFootprint.FIVE_BY_FIVE &&
                    towers.getValue("turret").footprint == TowerFootprint.FIVE_BY_FIVE &&
                    towers.getValue("leach").footprint == TowerFootprint.FIVE_BY_FIVE &&
                    towers.getValue("archer").footprint == TowerFootprint.THREE_BY_THREE
            ),
            FixtureResult(
                "tower-leach-one-per-team",
                towers.getValue("leach").teamLimit == 1
            )
        )
    }
}
