package dev.cubecrafttd.testing

import dev.cubecrafttd.match.*
import dev.cubecrafttd.truth.*

object ArmageddonFallbackFixture {
    fun run():List<FixtureResult> {
        val empty=
            RuntimeFallbackConfigV1()

        val lightningMissing=
            ArmageddonFallbackValidator
                .validate(
                    ArmageddonType.LIGHTNING,
                    empty
                )
        val witherMissing=
            ArmageddonFallbackValidator
                .validate(
                    ArmageddonType.WITHER,
                    empty
                )
        val hordeMissing=
            ArmageddonFallbackValidator
                .validate(
                    ArmageddonType.HORDE,
                    empty
                )

        val configured=
            RuntimeFallbackConfigV1(
                lightningFirstStrikeDelayTicks=20,
                lightningStrikeIntervalTicks=100,
                lightningTowersPerTeamPerStrike=1,
                witherSpeedBlocksPerTick=0.1,
                witherRegenerationHealthPerSecond=10.0,
                witherFirstTowerSkullDelayTicks=20,
                witherTowerSkullIntervalTicks=100,
                witherTowerSkullRangeBlocks=12.0,
                witherTowerTargetPolicy=
                    WitherTowerTargetPolicy
                        .NEAREST_IN_RANGE_ENGINEERING,
                witherFirstTrailSkeletonDelayTicks=40,
                witherTrailSkeletonIntervalTicks=200,
                witherTrailSkeletonsPerEvent=2,
                witherTrailSkeletonLevel=4,
                hordeFirstWaveDelayTicks=20,
                hordeWaveIntervalTicks=100,
                hordeWavePlan=listOf(
                    ArmageddonHordeFallbackWave(
                        "giant",3,2
                    )
                )
            )

        val lightning=
            ArmageddonFallbackBindings
                .lightning(configured)
        val wither=
            ArmageddonFallbackBindings
                .wither(configured)
        val horde=
            ArmageddonFallbackBindings
                .horde(configured)

        return listOf(
            FixtureResult(
                "armageddon-fallback-selection-specific",
                lightningMissing.size==3 &&
                    witherMissing.size==10 &&
                    hordeMissing.size==3
            ),
            FixtureResult(
                "armageddon-lightning-binding",
                lightning
                    .strikeIntervalTicks.value==
                    100L &&
                    lightning
                        .towersPerTeamPerStrike
                        .value==1
            ),
            FixtureResult(
                "armageddon-wither-binding",
                wither.speedBlocksPerTick
                    .value==0.1 &&
                    wither
                        .regenerationHealthPerSecond
                        .value==10.0 &&
                    wither
                        .towerSkullRangeBlocks
                        .value==12.0 &&
                    wither
                        .trailWitherSkeletonsPerEvent
                        .value==2 &&
                    wither
                        .trailWitherSkeletonLevel
                        .value==4
            ),
            FixtureResult(
                "armageddon-horde-binding",
                horde.waves.single()
                    .mobId=="giant" &&
                    horde.waves.single()
                        .quantityPerTeam==2
            )
        )
    }
}
