package dev.cubecrafttd.truth

import dev.cubecrafttd.economy.MobKillRewardResolver
import dev.cubecrafttd.mob.MobMovementRateResolver
import dev.cubecrafttd.tower.TowerPathDefinition

object RuntimeFallbackBindings {
    fun mobMovementRate(
        config: RuntimeFallbackConfigV1
    ): MobMovementRateResolver =
        MobMovementRateResolver {
            mobId, level, _ ->
            val key =
                RuntimeFallbackKeyConventions
                    .mobLevel(mobId,level)
            val value =
                config.mobSpeedBlocksPerTick[key]
                    ?: error(
                        "Missing mob speed fallback for $key"
                    )
            ResolvedTruth(
                value,
                ResolutionSource
                    .ENGINEERING_FALLBACK
            )
        }

    fun mobKillReward(
        config: RuntimeFallbackConfigV1
    ): MobKillRewardResolver =
        MobKillRewardResolver {
            mobId, level ->
            val key =
                RuntimeFallbackKeyConventions
                    .mobLevel(mobId,level)
            val value =
                config.mobKillCoins[key]
                    ?: error(
                        "Missing mob kill Coin fallback for $key"
                    )
            ResolvedTruth(
                value,
                ResolutionSource
                    .ENGINEERING_FALLBACK
            )
        }

    fun towerAttackInterval(
        config: RuntimeFallbackConfigV1,
        towerId: String,
        stage: TowerPathDefinition
    ): ResolvedTruth<Double>? {
        if(
            stage.stats.attackIntervalSeconds !=
                null
        ) return null

        val key =
            RuntimeFallbackKeyConventions
                .towerStage(
                    towerId,
                    stage.option,
                    stage.level
                )
        val value =
            config.towerAttackIntervalSeconds[
                key
            ] ?: error(
                "Missing tower attack interval fallback for $key"
            )

        return ResolvedTruth(
            value,
            ResolutionSource
                .ENGINEERING_FALLBACK
        )
    }
    fun slimeLethal(
        config: RuntimeFallbackConfigV1
    ): dev.cubecrafttd.mob.SlimeLethalConfig =
        dev.cubecrafttd.mob.SlimeLethalConfig(
            ResolvedTruth(
                config.slimeMaxShrinkPhaseIndex
                    ?: error(
                        "slime.maxShrinkPhaseIndex unresolved"
                    ),
                ResolutionSource
                    .ENGINEERING_FALLBACK
            )
        )

    fun giantRunMultiplier(
        config: RuntimeFallbackConfigV1
    ): ResolvedTruth<Double> =
        ResolvedTruth(
            config.giantRunSpeedMultiplier
                ?: error(
                    "giant.runSpeedMultiplier unresolved"
                ),
            ResolutionSource
                .ENGINEERING_FALLBACK
        )

    fun iceSlowMovementMultiplier(
        config: RuntimeFallbackConfigV1
    ): ResolvedTruth<Double> =
        ResolvedTruth(
            config.iceSlowMovementMultiplier
                ?: error(
                    "status.iceSlowMovementMultiplier unresolved"
                ),
            ResolutionSource
                .ENGINEERING_FALLBACK
        )

    fun mobStatusDamage(
        config: RuntimeFallbackConfigV1
    ): dev.cubecrafttd.mob.MobStatusDamageResolvedConfig =
        dev.cubecrafttd.mob.MobStatusDamageResolvedConfig(
            burnIntervalTicks=
                ResolvedTruth(
                    config.burnIntervalTicks
                        ?: error(
                            "status.burnIntervalTicks unresolved"
                        ),
                    ResolutionSource
                        .ENGINEERING_FALLBACK
                )
        )

    fun witchHeal(
        config: RuntimeFallbackConfigV1
    ): dev.cubecrafttd.mob.WitchHealConfig =
        dev.cubecrafttd.mob.WitchHealConfig(
            healValue=ResolvedTruth(
                config.witchHealAmount
                    ?: error(
                        "witch.healAmount unresolved"
                    ),
                ResolutionSource
                    .ENGINEERING_FALLBACK
            ),
            healIntervalTicks=ResolvedTruth(
                config.witchHealIntervalTicks
                    ?: error(
                        "witch.healIntervalTicks unresolved"
                    ),
                ResolutionSource
                    .ENGINEERING_FALLBACK
            ),
            healRadiusBlocks=ResolvedTruth(
                config.witchHealRadiusBlocks
                    ?: error(
                        "witch.healRadiusBlocks unresolved"
                    ),
                ResolutionSource
                    .ENGINEERING_FALLBACK
            ),
            targetCap=ResolvedTruth(
                config.witchHealTargetCap
                    ?: error(
                        "witch.healTargetCap unresolved"
                    ),
                ResolutionSource
                    .ENGINEERING_FALLBACK
            ),
            mode=ResolvedTruth(
                config.witchHealMode
                    ?: error(
                        "witch.healMode unresolved"
                    ),
                ResolutionSource
                    .ENGINEERING_FALLBACK
            )
        )

    fun creeperRegen(
        config: RuntimeFallbackConfigV1
    ): dev.cubecrafttd.mob.PassiveRegenConfig =
        dev.cubecrafttd.mob.PassiveRegenConfig(
            ResolvedTruth(
                config.creeperRegenHealthPerSecond
                    ?: error(
                        "creeper.regenHealthPerSecond unresolved"
                    ),
                ResolutionSource
                    .ENGINEERING_FALLBACK
            )
        )

    fun giantRegen(
        config: RuntimeFallbackConfigV1
    ): dev.cubecrafttd.mob.GiantRegenConfig =
        dev.cubecrafttd.mob.GiantRegenConfig(
            ResolvedTruth(
                config.giantRegenHealthPerSecond
                    ?: error(
                        "giant.regenHealthPerSecond unresolved"
                    ),
                ResolutionSource
                    .ENGINEERING_FALLBACK
            )
        )


    fun leachCharge(
        config: RuntimeFallbackConfigV1
    ): dev.cubecrafttd.tower.LeachChargeConfig =
        dev.cubecrafttd.tower.LeachChargeConfig(
            maxCharge=ResolvedTruth(
                config.leachMaxCharge
                    ?: error(
                        "leach.maxCharge unresolved"
                    ),
                ResolutionSource
                    .ENGINEERING_FALLBACK
            ),
            chargeGainPerEligibleTargetTick=
                ResolvedTruth(
                    config.leachChargePerTargetTick
                        ?: error(
                            "leach.chargePerTargetTick unresolved"
                        ),
                    ResolutionSource
                        .ENGINEERING_FALLBACK
                )
        )


    fun towerSummonCounts(
        config: RuntimeFallbackConfigV1
    ): dev.cubecrafttd.tower.TowerSummonCountResolver =
        dev.cubecrafttd.tower
            .RecommendedTowerSummonCountResolver
            .withExplicitSorcererCounts(
                config.towerSummonActiveCounts
            )


    fun towerChainTargetPolicy(
        config: RuntimeFallbackConfigV1
    ): ResolvedTruth<dev.cubecrafttd.tower.TowerChainTargetPolicy> =
        ResolvedTruth(
            config.towerChainTargetPolicy
                ?: error(
                    "tower.chainTargetPolicy unresolved"
                ),
            ResolutionSource
                .ENGINEERING_FALLBACK
        )


    fun towerDefaultTargetPriority(
        config: RuntimeFallbackConfigV1
    ): ResolvedTruth<dev.cubecrafttd.tower.TargetPriorityPolicy> =
        ResolvedTruth(
            config.towerDefaultTargetPriority
                ?: error(
                    "tower.defaultTargetPriority unresolved"
                ),
            ResolutionSource
                .ENGINEERING_FALLBACK
        )


    fun towerRequiresLineOfSight(
        config: RuntimeFallbackConfigV1,
        towerId: String
    ): ResolvedTruth<Boolean> =
        ResolvedTruth(
            config.towerRequiresLineOfSight[
                towerId
            ] ?: error(
                "tower.requiresLineOfSight.$towerId unresolved"
            ),
            ResolutionSource
                .ENGINEERING_FALLBACK
        )


    private fun aoeNumber(
        config: RuntimeFallbackConfigV1,
        key: String
    ): Number =
        config.aoeNumericFallbacks[key]
            ?: error(
                "aoe." + key + " unresolved"
            )

    fun aoePotionConfig(
        config: RuntimeFallbackConfigV1,
        potionId: String
    ): dev.cubecrafttd.player.AoEPotionRuntimeConfig =
        when(potionId) {
            "freeze" ->
                dev.cubecrafttd.player
                    .AoEPotionRuntimeConfig.Freeze(
                        dev.cubecrafttd.player
                            .FreezePotionResolvedConfig(
                                slowMagnitude=
                                    ResolvedTruth(
                                        aoeNumber(
                                            config,
                                            "freeze.slowMagnitude"
                                        ).toDouble(),
                                        ResolutionSource
                                            .ENGINEERING_FALLBACK
                                    ),
                                durationTicks=
                                    ResolvedTruth(
                                        aoeNumber(
                                            config,
                                            "freeze.durationTicks"
                                        ).toLong(),
                                        ResolutionSource
                                            .ENGINEERING_FALLBACK
                                    )
                            )
                    )
            "inferno" ->
                dev.cubecrafttd.player
                    .AoEPotionRuntimeConfig.Inferno(
                        dev.cubecrafttd.player
                            .InfernoPotionResolvedConfig(
                                damagePerSecond=
                                    ResolvedTruth(
                                        aoeNumber(
                                            config,
                                            "inferno.damagePerSecond"
                                        ).toDouble(),
                                        ResolutionSource
                                            .ENGINEERING_FALLBACK
                                    ),
                                pulseIntervalTicks=
                                    ResolvedTruth(
                                        aoeNumber(
                                            config,
                                            "inferno.pulseIntervalTicks"
                                        ).toLong(),
                                        ResolutionSource
                                            .ENGINEERING_FALLBACK
                                    )
                            )
                    )
            "meteor" ->
                dev.cubecrafttd.player
                    .AoEPotionRuntimeConfig.Meteor(
                        dev.cubecrafttd.player
                            .MeteorPotionResolvedConfig(
                                ResolvedTruth(
                                    aoeNumber(
                                        config,
                                        "meteor.pulseIntervalTicks"
                                    ).toLong(),
                                    ResolutionSource
                                        .ENGINEERING_FALLBACK
                                )
                            )
                    )
            "zeus" ->
                dev.cubecrafttd.player
                    .AoEPotionRuntimeConfig.Zeus(
                        dev.cubecrafttd.player
                            .ZeusPotionResolvedConfig(
                                damagePerBolt=
                                    ResolvedTruth(
                                        aoeNumber(
                                            config,
                                            "zeus.damagePerBolt"
                                        ).toDouble(),
                                        ResolutionSource
                                            .ENGINEERING_FALLBACK
                                    ),
                                boltCount=
                                    ResolvedTruth(
                                        aoeNumber(
                                            config,
                                            "zeus.boltCount"
                                        ).toInt(),
                                        ResolutionSource
                                            .ENGINEERING_FALLBACK
                                    )
                            )
                    )
            "speed" ->
                dev.cubecrafttd.player
                    .AoEPotionRuntimeConfig.Speed(
                        dev.cubecrafttd.player
                            .SpeedPotionResolvedConfig(
                                speedMultiplier=
                                    ResolvedTruth(
                                        aoeNumber(
                                            config,
                                            "speed.speedMultiplier"
                                        ).toDouble(),
                                        ResolutionSource
                                            .ENGINEERING_FALLBACK
                                    ),
                                durationTicks=
                                    ResolvedTruth(
                                        aoeNumber(
                                            config,
                                            "speed.durationTicks"
                                        ).toLong(),
                                        ResolutionSource
                                            .ENGINEERING_FALLBACK
                                    )
                            )
                    )
            "heal" ->
                dev.cubecrafttd.player
                    .AoEPotionRuntimeConfig.Heal(
                        dev.cubecrafttd.player
                            .HealPotionResolvedConfig(
                                ResolvedTruth(
                                    aoeNumber(
                                        config,
                                        "heal.healAmount"
                                    ).toDouble(),
                                    ResolutionSource
                                        .ENGINEERING_FALLBACK
                                )
                            )
                    )
            else ->
                error(
                    "Unknown AoE potion " + potionId
                )
        }

    fun aoeEffectLengthBlocks(
        config: RuntimeFallbackConfigV1,
        definition:
            dev.cubecrafttd.ui.AoEPotionDefinition
    ): ResolvedTruth<Double> {
        val observed=
            definition.effectLengthBlocks
        return if(observed!=null) {
            ResolvedTruth(
                observed.toDouble(),
                ResolutionSource
                    .OBSERVED_ORIGINAL
            )
        } else {
            ResolvedTruth(
                aoeNumber(
                    config,
                    definition.potionId +
                        ".effectLengthBlocks"
                ).toDouble(),
                ResolutionSource
                    .ENGINEERING_FALLBACK
            )
        }
    }

    fun aoeKillAwardsCoins(
        config: RuntimeFallbackConfigV1
    ): ResolvedTruth<Boolean> =
        ResolvedTruth(
            config.aoeKillAwardsCoins
                ?: error(
                    "aoe.killAwardsCoins unresolved"
                ),
            ResolutionSource
                .ENGINEERING_FALLBACK
        )


}
