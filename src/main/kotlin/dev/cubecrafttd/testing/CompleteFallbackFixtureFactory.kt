package dev.cubecrafttd.testing

import dev.cubecrafttd.castle.GuardTargetPriorityPolicy
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.truth.*

/**
 * One canonical fully-populated engineering config for tests that need a
 * playable Normal ruleset. Values are fixture-only and are never original
 * CubeCraft truth.
 */
object CompleteFallbackFixtureFactory {
    fun create():
        RuntimeFallbackConfigV1 {
        val mobKeys =
            RecommendedMatureMobDefinitions
                .all()
                .values
                .flatMap { d ->
                    d.levels.map {
                        RuntimeFallbackKeyConventions
                            .mobLevel(
                                d.mobId,
                                it.level
                            )
                    }
                }

        val towerKeys =
            RecommendedMatureTowerDefinitions
                .all()
                .values
                .flatMap { d ->
                    d.levels
                        .filter {
                            it.stats
                                .attackIntervalSeconds ==
                                null
                        }
                        .map {
                            RuntimeFallbackKeyConventions
                                .towerStage(
                                    d.towerId,
                                    it.option,
                                    it.level
                                )
                        }
                }

        return RuntimeFallbackConfigV1(
            normalStartingCoins=800,
            normalStartingExp=0,
            goldmineFirstIncomeDelayTicks=20,
            troopSpawnCadenceTicks=5,
            mobSpeedBlocksPerTick=
                mobKeys.associateWith { 0.1 },
            castleFirstHitDelayTicks=20,
            castleOrdinaryAttackIntervalTicks=40,
            guardDamagePerArrow=5.0,
            guardFireIntervalTicks=20,
            guardRangeBlocks=12.0,
            guardTargetPriority=
                GuardTargetPriorityPolicy.FIRST,
            witchHealAmount=0.10,
            witchHealMode=
                WitchHealMode
                    .MAX_HEALTH_FRACTION,
            witchHealTargetCap=8,
            witchHealIntervalTicks=40,
            witchHealRadiusBlocks=8.0,
            creeperRegenHealthPerSecond=5.0,
            giantRegenHealthPerSecond=5.0,
            giantRunSpeedMultiplier=1.2,
            slimeMaxShrinkPhaseIndex=2,
            leachMaxCharge=100.0,
            leachChargePerTargetTick=1.0,
            towerAttackIntervalSeconds=
                towerKeys.associateWith {
                    1.0
                },
            towerSummonActiveCounts=
                mapOf(
                    "sorcerer.none.l1" to 1,
                    "sorcerer.none.l2" to 1,
                    "sorcerer.bottom.l3" to 2
                ),
            towerChainTargetPolicy=
                dev.cubecrafttd.tower
                    .TowerChainTargetPolicy
                    .ROUTE_PROGRESS_FORWARD_ENGINEERING,
            towerDefaultTargetPriority=
                dev.cubecrafttd.tower
                    .TargetPriorityPolicy.FIRST,
            towerRequiresLineOfSight=
                RecommendedMatureTowerDefinitions
                    .all()
                    .keys
                    .associateWith { true },
            mobKillCoins=
                mobKeys.associateWith {
                    1L
                }
        )
    }
}
