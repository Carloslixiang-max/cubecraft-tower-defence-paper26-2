package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.ArenaContext
import dev.cubecrafttd.match.ResolvedNormalGameplayConfig
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.truth.*

class BukkitTowerRuntimeConfigProvider(
    private val fallback:
        RuntimeFallbackConfigV1,
    private val resolved:
        ResolvedNormalGameplayConfig,
    private val definitions:
        TowerDefinitionRepository =
        RecommendedMatureTowerDefinitions
) : TowerRuntimeConfigProvider {
    override fun config(
        context: ArenaContext,
        tower: TowerRuntimeState,
        engine: TowerControllerEngine
    ): TowerAttackRuntimeConfig {
        val path=tower.upgrade.path
            ?: error(
                "Tower path unresolved"
            )
        val stage=
            TowerStageResolver.resolve(
                definitions.get(
                    tower.identity.towerId
                ),
                path,
                tower.upgrade.level
            )

        return engine.resolveConfig(
            tower=tower,
            priority=
                resolved
                    .towerDefaultTargetPriority
                    .value,
            requiresLineOfSight=
                RuntimeFallbackBindings
                    .towerRequiresLineOfSight(
                        fallback,
                        tower.identity.towerId
                    ).value,
            explicitTimingFallbackSeconds=
                RuntimeFallbackBindings
                    .towerAttackInterval(
                        fallback,
                        tower.identity.towerId,
                        stage
                    )
        )
    }
}
