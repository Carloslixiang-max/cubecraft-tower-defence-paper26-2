package dev.cubecrafttd.tower

import dev.cubecrafttd.arena.*
import java.util.UUID

fun interface AreaTowerTargetResolver {
    fun affectedTargets(
        context: ArenaContext,
        tower: TowerRuntimeState,
        primaryTargetUuid: UUID,
        radiusBlocks: Double
    ): List<UUID>
}

fun interface ChainTowerTargetResolver {
    /**
     * Caller must return a deterministic, already ordered list of NEXT targets.
     * Exact original bounce geometry is not assumed by core.
     */
    fun orderedNextTargets(
        context: ArenaContext,
        tower: TowerRuntimeState,
        primaryTargetUuid: UUID,
        maxAdditionalTargets: Int
    ): List<UUID>
}

class RecommendedTowerAttackPlannerProvider(
    private val rng:
        ArenaDeterministicRng,
    private val areaTargets:
        AreaTowerTargetResolver,
    private val chainTargets:
        ChainTowerTargetResolver
) : TowerAttackPlannerProvider {
    override fun planner(
        context: ArenaContext,
        tower: TowerRuntimeState
    ): TowerAttackPlanner =
        when(tower.identity.towerId) {
            "archer" ->
                DirectTowerAttackPlanners
                    .archer()

            "mage" ->
                DirectTowerAttackPlanners
                    .mage()

            "ice" ->
                DirectTowerAttackPlanners
                    .ice(rng)

            "poison" ->
                DirectTowerAttackPlanners
                    .poison()

            "artillery" ->
                TowerAttackPlanner {
                    stage,target,attackContext ->
                    val radius=
                        stage.stats.extras[
                            "aoeRange"
                        ] ?: 0.0
                    val affected=
                        areaTargets
                            .affectedTargets(
                                context,
                                tower,
                                target.entityUuid,
                                radius
                            )
                            .toMutableList()
                    if(
                        target.entityUuid
                            !in affected
                    ) {
                        affected +=
                            target.entityUuid
                    }
                    BasicTowerProcessors
                        .artillery(
                            stage,
                            target.entityUuid,
                            affected,
                            attackContext
                        )
                }

            "quake" ->
                TowerAttackPlanner {
                    stage,target,attackContext ->
                    val radius=
                        stage.stats.extras[
                            "aoeRange"
                        ] ?: stage.stats
                            .rangeBlocks
                            ?: error(
                                "Quake range unresolved"
                            )
                    val affected=
                        areaTargets
                            .affectedTargets(
                                context,
                                tower,
                                target.entityUuid,
                                radius
                            )
                            .toMutableList()
                    if(
                        target.entityUuid
                            !in affected
                    ) {
                        affected +=
                            target.entityUuid
                    }
                    BasicTowerProcessors
                        .quake(
                            stage,
                            target.entityUuid,
                            affected,
                            attackContext
                        )
                }

            "zeus" ->
                TowerAttackPlanner {
                    stage,target,attackContext ->
                    if(
                        "baby_zeus" in
                            stage.abilityTags
                    ) {
                        effectiveDirect(
                            stage,
                            target.entityUuid,
                            attackContext,
                            dev.cubecrafttd.mob
                                .DamageKind.LIGHTNING,
                            "baby_zeus_effective"
                        )
                    } else {
                        val maxAdditional=
                            if(
                                "bounce_up_to_5" in
                                    stage.abilityTags
                            ) 4 else 0
                        val next=
                            chainTargets
                                .orderedNextTargets(
                                    context,
                                    tower,
                                    target.entityUuid,
                                    maxAdditional
                                )
                        SpecialTowerProcessors
                            .zeusChain(
                                stage,
                                target.entityUuid,
                                next,
                                attackContext
                            )
                    }
                }

            "turret" ->
                TowerAttackPlanner {
                    stage,target,attackContext ->
                    val maxAdditional=
                        if(
                            "bounce_up_to_5" in
                                stage.abilityTags
                        ) 4 else 0
                    val next=
                        chainTargets
                            .orderedNextTargets(
                                context,
                                tower,
                                target.entityUuid,
                                maxAdditional
                            )
                    SpecialTowerProcessors
                        .turretBounce(
                            stage,
                            target.entityUuid,
                            next,
                            attackContext
                        )
                }

            "leach" ->
                DirectTowerAttackPlanners
                    .leachDirect()

            "sorcerer" ->
                TowerAttackPlanner {
                    stage,target,attackContext ->
                    if(
                        "kamikaze" in
                            stage.abilityTags
                    ) {
                        val radius=
                            stage.stats.extras[
                                "aoeRange"
                            ] ?: 0.0
                        val affected=
                            areaTargets
                                .affectedTargets(
                                    context,
                                    tower,
                                    target.entityUuid,
                                    radius
                                )
                                .toMutableList()
                        if(
                            target.entityUuid
                                !in affected
                        ) {
                            affected +=
                                target.entityUuid
                        }
                        effectiveArea(
                            stage,
                            affected,
                            attackContext,
                            dev.cubecrafttd.mob
                                .DamageKind.PHYSICAL,
                            "sorcerer_kamikaze_effective"
                        )
                    } else {
                        effectiveDirect(
                            stage,
                            target.entityUuid,
                            attackContext,
                            dev.cubecrafttd.mob
                                .DamageKind.PHYSICAL,
                            "sorcerer_summon_effective"
                        )
                    }
                }

            "necromancer" ->
                TowerAttackPlanner {
                    stage,target,attackContext ->
                    effectiveDirect(
                        stage,
                        target.entityUuid,
                        attackContext,
                        dev.cubecrafttd.mob
                            .DamageKind.PHYSICAL,
                        "necromancer_summon_effective"
                    )
                }

            else ->
                error(
                    "Unknown tower planner for ${tower.identity.towerId}"
                )
        }
    private fun effectiveDirect(
        stage: TowerPathDefinition,
        target: UUID,
        context: TowerAttackContext,
        kind: dev.cubecrafttd.mob.DamageKind,
        tag: String
    ): dev.cubecrafttd.combat.AttackBatch {
        val damage=
            stage.stats.damage
                ?: error(
                    "Effective summon damage unresolved"
                )
        return dev.cubecrafttd.combat.AttackBatch(
            target,
            listOf(
                dev.cubecrafttd.combat.AttackImpact(
                    target,
                    dev.cubecrafttd.combat
                        .AttackDeliveryKind.DIRECT,
                    listOf(
                        dev.cubecrafttd.combat
                            .DamageComponent(
                                kind,damage
                            )
                    ),
                    sourceTowerInstanceId=
                        context.towerInstanceId,
                    sourceOwnerUuid=
                        context.ownerUuid
                )
            ),
            mapOf(
                "towerAction" to tag,
                "combatModel" to
                    "ENGINEERING_EFFECTIVE_STAGE_STATS"
            )
        )
    }

    private fun effectiveArea(
        stage: TowerPathDefinition,
        targets: Collection<UUID>,
        context: TowerAttackContext,
        kind: dev.cubecrafttd.mob.DamageKind,
        tag: String
    ): dev.cubecrafttd.combat.AttackBatch {
        val damage=
            stage.stats.damage
                ?: error(
                    "Effective summon damage unresolved"
                )
        val ids=targets.distinct()
        require(ids.isNotEmpty())
        return dev.cubecrafttd.combat.AttackBatch(
            ids.first(),
            ids.map { uuid ->
                dev.cubecrafttd.combat
                    .AttackImpact(
                        uuid,
                        dev.cubecrafttd.combat
                            .AttackDeliveryKind.SPLASH,
                        listOf(
                            dev.cubecrafttd.combat
                                .DamageComponent(
                                    kind,damage
                                )
                        ),
                        sourceTowerInstanceId=
                            context.towerInstanceId,
                        sourceOwnerUuid=
                            context.ownerUuid
                    )
            },
            mapOf(
                "towerAction" to tag,
                "combatModel" to
                    "ENGINEERING_EFFECTIVE_STAGE_STATS"
            )
        )
    }

}
