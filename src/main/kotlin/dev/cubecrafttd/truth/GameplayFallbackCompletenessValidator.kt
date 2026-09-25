package dev.cubecrafttd.truth

import dev.cubecrafttd.mob.RecommendedMatureMobDefinitions
import dev.cubecrafttd.tower.RecommendedMatureTowerDefinitions

object RuntimeFallbackKeyConventions {
    fun mobLevel(
        mobId: String,
        level: Int
    ): String = "$mobId.l$level"

    fun towerStage(
        towerId: String,
        option: dev.cubecrafttd.tower.TowerPathOption,
        level: Int
    ): String =
        "$towerId.${option.name.lowercase()}.l$level"
}

object GameplayFallbackCompletenessValidator {
    fun validate(
        config: RuntimeFallbackConfigV1
    ): List<MissingFallback> {
        val missing =
            RuntimeFallbackValidator
                .validateForNormal(config)
                .toMutableList()

        RecommendedMatureMobDefinitions
            .all()
            .values
            .sortedBy { it.mobId }
            .forEach { definition ->
                definition.levels.forEach { level ->
                    val key =
                        RuntimeFallbackKeyConventions
                            .mobLevel(
                                definition.mobId,
                                level.level
                            )

                    if(
                        key !in
                            config.mobSpeedBlocksPerTick
                    ) {
                        missing += MissingFallback(
                            key =
                                "mob.speedBlocksPerTick.$key",
                            level =
                                FallbackRequirementLevel
                                    .BLOCKS_MECHANIC,
                            reason =
                                "World movement conversion is unresolved for " +
                                    "${definition.mobId} L${level.level}"
                        )
                    }

                    if(
                        key !in
                            config.mobKillCoins
                    ) {
                        missing += MissingFallback(
                            key =
                                "mob.killCoins.$key",
                            level =
                                FallbackRequirementLevel
                                    .BLOCKS_MECHANIC,
                            reason =
                                "Mature kill Coin reward is unresolved for " +
                                    "${definition.mobId} L${level.level}"
                        )
                    }
                }
            }

        RecommendedMatureTowerDefinitions
            .all()
            .values
            .sortedBy { it.towerId }
            .forEach { definition ->
                definition.levels
                    .filter {
                        it.stats
                            .attackIntervalSeconds ==
                            null
                    }
                    .forEach { stage ->
                        val key =
                            RuntimeFallbackKeyConventions
                                .towerStage(
                                    definition.towerId,
                                    stage.option,
                                    stage.level
                                )
                        if(
                            key !in
                                config
                                    .towerAttackIntervalSeconds
                        ) {
                            missing += MissingFallback(
                                key =
                                    "tower.attackIntervalSeconds.$key",
                                level =
                                    FallbackRequirementLevel
                                        .BLOCKS_MECHANIC,
                                reason =
                                    "Attack cadence is unresolved for " +
                                        "${definition.towerId} " +
                                        "${stage.option} L${stage.level}"
                            )
                        }
                    }
            }


        listOf(
            "sorcerer.none.l1",
            "sorcerer.none.l2",
            "sorcerer.bottom.l3"
        ).forEach { key ->
            if(
                key !in
                    config.towerSummonActiveCounts
            ) {
                missing += MissingFallback(
                    key=
                        "tower.summonActiveCounts.$key",
                    level=
                        FallbackRequirementLevel
                            .BLOCKS_MECHANIC,
                    reason=
                        "Persistent Sorcerer summon count is unresolved for $key"
                )
            }
        }


        RecommendedMatureTowerDefinitions
            .all()
            .keys
            .sorted()
            .forEach { towerId ->
                if(
                    towerId !in
                        config.towerRequiresLineOfSight
                ) {
                    missing += MissingFallback(
                        key=
                            "tower.requiresLineOfSight.$towerId",
                        level=
                            FallbackRequirementLevel
                                .BLOCKS_MECHANIC,
                        reason=
                            "Per-tower line-of-sight behavior is not fully recovered for Mature runtime"
                    )
                }
            }

        return missing.distinctBy {
            it.key
        }
    }
}
