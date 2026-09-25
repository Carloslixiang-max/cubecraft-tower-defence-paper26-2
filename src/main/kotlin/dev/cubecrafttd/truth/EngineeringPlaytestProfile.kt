package dev.cubecrafttd.truth

import dev.cubecrafttd.castle.GuardTargetPriorityPolicy
import dev.cubecrafttd.match.WitherTowerTargetPolicy
import dev.cubecrafttd.mob.RecommendedMatureMobDefinitions
import dev.cubecrafttd.mob.WitchHealMode
import dev.cubecrafttd.tower.RecommendedMatureTowerDefinitions
import dev.cubecrafttd.tower.TargetPriorityPolicy
import dev.cubecrafttd.tower.TowerChainTargetPolicy

/**
 * Explicit, non-authentic values whose only purpose is to make the current
 * Paper runtime playable while remaining original-truth-safe.
 */
object EngineeringPlaytestProfile {
    const val ID = "ENGINEERING_PLAYTEST_V1"

    fun create(): RuntimeFallbackConfigV1 {
        val mobKeys =
            RecommendedMatureMobDefinitions
                .all()
                .values
                .flatMap { definition ->
                    definition.levels.map { level ->
                        RuntimeFallbackKeyConventions
                            .mobLevel(
                                definition.mobId,
                                level.level
                            )
                    }
                }

        val unresolvedTowerCadenceKeys =
            RecommendedMatureTowerDefinitions
                .all()
                .values
                .flatMap { definition ->
                    definition.levels
                        .filter {
                            it.stats
                                .attackIntervalSeconds ==
                                null
                        }
                        .map { stage ->
                            RuntimeFallbackKeyConventions
                                .towerStage(
                                    definition.towerId,
                                    stage.option,
                                    stage.level
                                )
                        }
                }

        return RuntimeFallbackConfigV1(
            normalStartingCoins=800,
            normalStartingExp=0,
            goldmineFirstIncomeDelayTicks=20,
            troopSpawnCadenceTicks=5,
            mobSpeedBlocksPerTick=
                mobKeys.associateWith { 0.10 },
            castleFirstHitDelayTicks=20,
            castleOrdinaryAttackIntervalTicks=40,
            guardDamagePerArrow=5.0,
            guardFireIntervalTicks=20,
            guardRangeBlocks=12.0,
            guardTargetPriority=
                GuardTargetPriorityPolicy.FIRST,
            witchHealAmount=0.10,
            witchHealMode=
                WitchHealMode.MAX_HEALTH_FRACTION,
            witchHealTargetCap=8,
            witchHealIntervalTicks=40,
            witchHealRadiusBlocks=8.0,
            creeperRegenHealthPerSecond=5.0,
            giantRegenHealthPerSecond=5.0,
            giantRunSpeedMultiplier=1.20,
            iceSlowMovementMultiplier=0.50,
            slimeMaxShrinkPhaseIndex=2,
            leachMaxCharge=100.0,
            leachChargePerTargetTick=1.0,
            towerAttackIntervalSeconds=
                unresolvedTowerCadenceKeys
                    .associateWith { 1.0 },
            towerSummonActiveCounts=
                mapOf(
                    "sorcerer.none.l1" to 1,
                    "sorcerer.none.l2" to 1,
                    "sorcerer.bottom.l3" to 2
                ),
            towerChainTargetPolicy=
                TowerChainTargetPolicy
                    .ROUTE_PROGRESS_FORWARD_ENGINEERING,
            towerDefaultTargetPriority=
                TargetPriorityPolicy.FIRST,
            towerRequiresLineOfSight=
                RecommendedMatureTowerDefinitions
                    .all()
                    .keys
                    .associateWith { true },
            mobKillCoins=
                mobKeys.associateWith { 1L },
            lightningFirstStrikeDelayTicks=100,
            lightningStrikeIntervalTicks=200,
            lightningTowersPerTeamPerStrike=1,
            witherSpeedBlocksPerTick=0.025,
            witherRegenerationHealthPerSecond=5.0,
            witherFirstTowerSkullDelayTicks=100,
            witherTowerSkullIntervalTicks=80,
            witherTowerSkullRangeBlocks=8.0,
            witherTowerTargetPolicy=
                WitherTowerTargetPolicy
                    .NEAREST_IN_RANGE_ENGINEERING,
            witherFirstTrailSkeletonDelayTicks=200,
            witherTrailSkeletonIntervalTicks=200,
            witherTrailSkeletonsPerEvent=1,
            witherTrailSkeletonLevel=4,
            hordeFirstWaveDelayTicks=100,
            hordeWaveIntervalTicks=400,
            hordeWavePlan=
                listOf(
                    ArmageddonHordeFallbackWave(
                        "giant",3,1
                    ),
                    ArmageddonHordeFallbackWave(
                        "blaze",4,2
                    ),
                    ArmageddonHordeFallbackWave(
                        "skeleton",5,3
                    )
                )
        )
    }
}
