package dev.cubecrafttd.truth

import dev.cubecrafttd.match.*

data class ArmageddonHordeFallbackWave(
    val mobId: String,
    val level: Int,
    val quantityPerTeam: Int
) {
    init {
        require(mobId.isNotBlank())
        require(level > 0)
        require(quantityPerTeam > 0)
    }

    fun toRuntime():
        HordeSpawnOrder =
        HordeSpawnOrder(
            mobId,level,quantityPerTeam
        )
}

object ArmageddonFallbackValidator {
    fun validate(
        type: ArmageddonType,
        config: RuntimeFallbackConfigV1
    ): List<MissingFallback> {
        val out=
            mutableListOf<MissingFallback>()

        fun missing(
            condition:Boolean,
            key:String,
            reason:String
        ) {
            if(!condition) {
                out += MissingFallback(
                    key,
                    FallbackRequirementLevel
                        .BLOCKS_MECHANIC,
                    reason
                )
            }
        }

        when(type) {
            ArmageddonType.LIGHTNING -> {
                missing(
                    config.lightningFirstStrikeDelayTicks!=
                        null,
                    "armageddon.lightning.firstStrikeDelayTicks",
                    "Delay before the first Lightning strike is unresolved"
                )
                missing(
                    config.lightningStrikeIntervalTicks!=
                        null,
                    "armageddon.lightning.strikeIntervalTicks",
                    "Lightning destroys towers over time, but exact cadence is unresolved"
                )
                missing(
                    config.lightningTowersPerTeamPerStrike!=
                        null,
                    "armageddon.lightning.towersPerTeamPerStrike",
                    "Exact number of towers destroyed per Lightning strike is unresolved"
                )
            }

            ArmageddonType.WITHER -> {
                missing(
                    config.witherSpeedBlocksPerTick!=
                        null,
                    "armageddon.wither.speedBlocksPerTick",
                    "Wither movement speed is unresolved"
                )
                missing(
                    config.witherRegenerationHealthPerSecond!=
                        null,
                    "armageddon.wither.regenerationHealthPerSecond",
                    "Wither has regeneration, but exact rate is unresolved"
                )

                missing(
                    config.witherFirstTowerSkullDelayTicks!=
                        null,
                    "armageddon.wither.firstTowerSkullDelayTicks",
                    "Delay before the first tower-destroying skull is unresolved"
                )
                missing(
                    config.witherTowerSkullIntervalTicks!=
                        null,
                    "armageddon.wither.towerSkullIntervalTicks",
                    "Wither destroys towers, but exact skull cadence is unresolved"
                )
                missing(
                    config.witherTowerSkullRangeBlocks!=
                        null,
                    "armageddon.wither.towerSkullRangeBlocks",
                    "The Wither attacks a tower when near it, but exact proximity range is unresolved"
                )
                missing(
                    config.witherTowerTargetPolicy!=
                        null,
                    "armageddon.wither.towerTargetPolicy",
                    "Exact tower selection rule for a Wither skull is unresolved"
                )

                missing(
                    config.witherFirstTrailSkeletonDelayTicks!=
                        null,
                    "armageddon.wither.firstTrailSkeletonDelayTicks",
                    "Delay before the first Wither Skeleton trail spawn is unresolved"
                )
                missing(
                    config.witherTrailSkeletonIntervalTicks!=
                        null,
                    "armageddon.wither.trailSkeletonIntervalTicks",
                    "Wither Skeleton trail cadence is unresolved"
                )
                missing(
                    config.witherTrailSkeletonsPerEvent!=
                        null,
                    "armageddon.wither.trailSkeletonsPerEvent",
                    "Number of Wither Skeletons per trail event is unresolved"
                )
                missing(
                    config.witherTrailSkeletonLevel!=
                        null,
                    "armageddon.wither.trailSkeletonLevel",
                    "Armageddon trail Skeleton exact stats are unresolved; explicit L4/L5 engineering mapping is required"
                )
            }

            ArmageddonType.HORDE -> {
                missing(
                    config.hordeFirstWaveDelayTicks!=
                        null,
                    "armageddon.horde.firstWaveDelayTicks",
                    "Delay before the first Horde wave is unresolved"
                )
                missing(
                    config.hordeWaveIntervalTicks!=
                        null,
                    "armageddon.horde.waveIntervalTicks",
                    "Horde spawns strong troops over time, but exact wave cadence is unresolved"
                )
                missing(
                    config.hordeWavePlan.isNotEmpty(),
                    "armageddon.horde.wavePlan",
                    "Complete original Horde composition is not recovered"
                )
            }
        }

        return out
    }
}

object ArmageddonFallbackBindings {
    fun lightning(
        config: RuntimeFallbackConfigV1
    ): LightningArmageddonConfig {
        check(
            ArmageddonFallbackValidator
                .validate(
                    ArmageddonType.LIGHTNING,
                    config
                ).isEmpty()
        )
        return LightningArmageddonConfig(
            strikeIntervalTicks=
                ResolvedTruth(
                    config.lightningStrikeIntervalTicks!!,
                    ResolutionSource
                        .ENGINEERING_FALLBACK
                ),
            towersPerTeamPerStrike=
                ResolvedTruth(
                    config.lightningTowersPerTeamPerStrike!!,
                    ResolutionSource
                        .ENGINEERING_FALLBACK
                )
        )
    }

    fun wither(
        config: RuntimeFallbackConfigV1
    ): WitherArmageddonConfig {
        check(
            ArmageddonFallbackValidator
                .validate(
                    ArmageddonType.WITHER,
                    config
                ).isEmpty()
        )
        fun <T> resolved(
            value:T
        )=ResolvedTruth(
            value,
            ResolutionSource
                .ENGINEERING_FALLBACK
        )

        return WitherArmageddonConfig(
            speedBlocksPerTick=
                resolved(
                    config.witherSpeedBlocksPerTick!!
                ),
            regenerationHealthPerSecond=
                resolved(
                    config.witherRegenerationHealthPerSecond!!
                ),

            firstTowerSkullDelayTicks=
                resolved(
                    config.witherFirstTowerSkullDelayTicks!!
                ),
            towerSkullIntervalTicks=
                resolved(
                    config.witherTowerSkullIntervalTicks!!
                ),
            towerSkullRangeBlocks=
                resolved(
                    config.witherTowerSkullRangeBlocks!!
                ),
            towerTargetPolicy=
                resolved(
                    config.witherTowerTargetPolicy!!
                ),

            firstTrailWitherSkeletonDelayTicks=
                resolved(
                    config.witherFirstTrailSkeletonDelayTicks!!
                ),
            trailWitherSkeletonIntervalTicks=
                resolved(
                    config.witherTrailSkeletonIntervalTicks!!
                ),
            trailWitherSkeletonsPerEvent=
                resolved(
                    config.witherTrailSkeletonsPerEvent!!
                ),
            trailWitherSkeletonLevel=
                resolved(
                    config.witherTrailSkeletonLevel!!
                )
        )
    }

    fun horde(
        config: RuntimeFallbackConfigV1
    ): HordeArmageddonConfig {
        check(
            ArmageddonFallbackValidator
                .validate(
                    ArmageddonType.HORDE,
                    config
                ).isEmpty()
        )
        return HordeArmageddonConfig(
            waveIntervalTicks=
                ResolvedTruth(
                    config.hordeWaveIntervalTicks!!,
                    ResolutionSource
                        .ENGINEERING_FALLBACK
                ),
            waves=
                config.hordeWavePlan
                    .map {
                        it.toRuntime()
                    }
        )
    }
    fun lightningFirstDelay(
        config: RuntimeFallbackConfigV1
    ): ResolvedTruth<Long> =
        ResolvedTruth(
            config.lightningFirstStrikeDelayTicks
                ?: error(
                    "armageddon.lightning.firstStrikeDelayTicks unresolved"
                ),
            ResolutionSource
                .ENGINEERING_FALLBACK
        )

    fun hordeFirstDelay(
        config: RuntimeFallbackConfigV1
    ): ResolvedTruth<Long> =
        ResolvedTruth(
            config.hordeFirstWaveDelayTicks
                ?: error(
                    "armageddon.horde.firstWaveDelayTicks unresolved"
                ),
            ResolutionSource
                .ENGINEERING_FALLBACK
        )


}
