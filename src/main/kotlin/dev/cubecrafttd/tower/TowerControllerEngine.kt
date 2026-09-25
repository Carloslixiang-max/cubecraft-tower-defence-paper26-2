package dev.cubecrafttd.tower

import dev.cubecrafttd.arena.ArenaDeterministicRng
import dev.cubecrafttd.combat.*
import dev.cubecrafttd.mob.TowerAttackCapability
import dev.cubecrafttd.truth.ResolvedTruth
import java.util.UUID

enum class TowerAttackCycleStatus {
    FIRED,
    COOLDOWN,
    NO_TARGET,
    INACTIVE
}

data class TowerAttackRuntimeConfig(
    val priority: TargetPriorityPolicy,
    val requiresLineOfSight: Boolean,
    val timing: ResolvedTowerAttackTiming
)

data class TowerAttackCycleReport(
    val status: TowerAttackCycleStatus,
    val selectedTargetUuid: UUID? = null,
    val application: AttackApplicationReport? = null,
    val nextAttackTick: Long? = null
)

fun interface TowerAttackPlanner {
    fun plan(
        stage: TowerPathDefinition,
        selectedTarget: TowerTargetCandidate,
        context: TowerAttackContext
    ): AttackBatch
}

object DirectTowerAttackPlanners {
    fun archer(): TowerAttackPlanner =
        TowerAttackPlanner { stage,target,context ->
            BasicTowerProcessors.archer(
                stage,target.entityUuid,context
            )
        }

    fun mage(): TowerAttackPlanner =
        TowerAttackPlanner { stage,target,context ->
            BasicTowerProcessors.mage(
                stage,target.entityUuid,context
            )
        }

    fun poison(): TowerAttackPlanner =
        TowerAttackPlanner { stage,target,context ->
            BasicTowerProcessors.poison(
                stage,target.entityUuid,context
            )
        }

    fun leachDirect(): TowerAttackPlanner =
        TowerAttackPlanner { stage,target,context ->
            val damage =
                stage.stats.damage
                    ?: error(
                        "Leach direct damage unresolved"
                    )
            AttackBatch(
                target.entityUuid,
                listOf(
                    AttackImpact(
                        target.entityUuid,
                        AttackDeliveryKind.DIRECT,
                        listOf(
                            DamageComponent(
                                dev.cubecrafttd.mob
                                    .DamageKind.LIGHTNING,
                                damage
                            )
                        ),
                        sourceTowerInstanceId =
                            context.towerInstanceId,
                        sourceOwnerUuid =
                            context.ownerUuid
                    )
                ),
                mapOf("tower" to "leach")
            )
        }

    fun ice(
        rng: ArenaDeterministicRng
    ): TowerAttackPlanner =
        TowerAttackPlanner { stage,target,context ->
            BasicTowerProcessors.ice(
                stage,
                target.entityUuid,
                context,
                chanceRoll =
                    if (stage.stats.extras["damageChance2021"] != null)
                        rng.nextDouble()
                    else null
            )
        }
}

class TowerControllerEngine(
    private val rng: ArenaDeterministicRng,
    private val applier: AttackBatchApplier,
    private val definitions: TowerDefinitionRepository =
        RecommendedMatureTowerDefinitions
) {
    fun resolveConfig(
        tower: TowerRuntimeState,
        priority: TargetPriorityPolicy,
        requiresLineOfSight: Boolean,
        explicitTimingFallbackSeconds:
            ResolvedTruth<Double>? = null
    ): TowerAttackRuntimeConfig {
        val stage = currentStage(tower)
        return TowerAttackRuntimeConfig(
            priority,
            requiresLineOfSight,
            TowerAttackTimingResolver.resolve(
                stage,
                explicitTimingFallbackSeconds
            )
        )
    }

    fun arm(
        tower: TowerRuntimeState,
        firstReadyTick: Long
    ) {
        require(firstReadyTick >= 0L)
        tower.cooldown.nextAttackTick = firstReadyTick
    }

    fun tryAttack(
        tower: TowerRuntimeState,
        gameTick: Long,
        candidates: Collection<TowerTargetCandidate>,
        config: TowerAttackRuntimeConfig,
        planner: TowerAttackPlanner
    ): TowerAttackCycleReport {
        if (tower.lifecycle != TowerLifecycleState.ACTIVE) {
            return TowerAttackCycleReport(
                TowerAttackCycleStatus.INACTIVE,
                nextAttackTick =
                    tower.cooldown.nextAttackTick
            )
        }
        if (gameTick < tower.cooldown.nextAttackTick) {
            return TowerAttackCycleReport(
                TowerAttackCycleStatus.COOLDOWN,
                nextAttackTick =
                    tower.cooldown.nextAttackTick
            )
        }

        val definition = definitions.get(
            tower.identity.towerId
        )
        val stage = currentStage(tower)
        val range = stage.stats.rangeBlocks
            ?: error(
                "Tower range unresolved for " +
                    "${tower.identity.towerId} " +
                    "L${tower.upgrade.level}"
            )
        val capability = TowerAttackCapability(
            tower.identity.towerId,
            (
                stage.targetLayersOverride
                    ?: definition.targetLayers
            ) + dev.cubecrafttd.mob.TargetLayer.BOSS,
            definition.damageKinds,
            definition.effectKinds
        )
        val selected = TowerTargetSelector.select(
            candidates,
            TowerTargetQuery(
                capability,
                range,
                config.requiresLineOfSight,
                config.priority
            ),
            rng
        ) ?: return TowerAttackCycleReport(
            TowerAttackCycleStatus.NO_TARGET,
            nextAttackTick =
                tower.cooldown.nextAttackTick
        )

        val attackContext = TowerAttackContext(
            tower.identity.instanceId.value,
            tower.identity.ownerUuid,
            gameTick
        )
        val batch = planner.plan(
            stage,selected,attackContext
        )
        check(
            batch.impacts.all {
                it.sourceTowerInstanceId ==
                    tower.identity.instanceId.value
            }
        ) {
            "Attack planner returned mismatched tower source"
        }

        val application = applier.apply(batch)
        advanceCooldown(
            tower,
            gameTick,
            config.timing.intervalTicks
        )

        return TowerAttackCycleReport(
            TowerAttackCycleStatus.FIRED,
            selected.entityUuid,
            application,
            tower.cooldown.nextAttackTick
        )
    }

    private fun currentStage(
        tower: TowerRuntimeState
    ): TowerPathDefinition {
        val path = tower.upgrade.path
            ?: error("Tower path unresolved")
        return TowerStageResolver.resolve(
            definitions.get(tower.identity.towerId),
            path,
            tower.upgrade.level
        )
    }

    private fun advanceCooldown(
        tower: TowerRuntimeState,
        gameTick: Long,
        intervalTicks: Long
    ) {
        require(intervalTicks > 0L)
        var next =
            tower.cooldown.nextAttackTick +
                intervalTicks
        while (next <= gameTick) {
            next += intervalTicks
        }
        tower.cooldown.nextAttackTick = next
    }
}
