package dev.cubecrafttd.tower

import dev.cubecrafttd.combat.*
import dev.cubecrafttd.mob.DamageKind
import dev.cubecrafttd.truth.ResolvedTruth
import java.util.UUID

enum class SummonKind {
    SORCERER_ANIMAL,
    SORCERER_KAMIKAZE,
    BABY_ZEUS,
    IRON_GOLEM,
    SHULKER,
    SNOWMAN
}

data class SummonRequest(
    val kind: SummonKind,
    val towerInstanceId: Long,
    val ownerUuid: UUID?,
    val targetUuid: UUID?,
    val maxActive: Int?,
    val metadata: Map<String,String> = emptyMap()
)

data class ChainStep(
    val targetUuid: UUID,
    val damage: Double,
    val stepIndex: Int
)

object DeterministicChainResolver {
    /**
     * `orderedCandidates` must already be deterministically sorted by the caller
     * according to the tower-specific next-target rule.
     */
    fun resolve(
        primary: UUID,
        orderedCandidates: List<UUID>,
        maxAdditionalTargets: Int,
        baseDamage: Double,
        damageMultiplierPerStep: Double = 1.0
    ): List<ChainStep> {
        require(maxAdditionalTargets >= 0)
        require(baseDamage >= 0.0)
        require(damageMultiplierPerStep >= 0.0)

        val unique = linkedSetOf<UUID>()
        unique += primary
        orderedCandidates.forEach {
            if (unique.size < maxAdditionalTargets + 1) unique += it
        }

        return unique.mapIndexed { index, uuid ->
            ChainStep(
                targetUuid = uuid,
                damage = baseDamage * Math.pow(damageMultiplierPerStep, index.toDouble()),
                stepIndex = index
            )
        }
    }
}

data class LeachChargeConfig(
    val maxCharge: ResolvedTruth<Double>,
    val chargeGainPerEligibleTargetTick: ResolvedTruth<Double>
) {
    init {
        require(maxCharge.value > 0.0)
        require(chargeGainPerEligibleTargetTick.value >= 0.0)
    }
}

data class LeachChargeState(
    var charge: Double = 0.0
)

class LeachChargeService(
    private val config: LeachChargeConfig
) {
    fun addEligibleTargets(
        state: LeachChargeState,
        eligibleTargetCount: Int
    ): Double {
        require(eligibleTargetCount >= 0)
        state.charge = (
            state.charge +
                config.chargeGainPerEligibleTargetTick.value * eligibleTargetCount
            ).coerceAtMost(config.maxCharge.value)
        return state.charge
    }

    fun ready(state: LeachChargeState): Boolean =
        state.charge >= config.maxCharge.value - 1e-12

    fun consume(state: LeachChargeState): Boolean {
        if (!ready(state)) return false
        state.charge = 0.0
        return true
    }
}

object SpecialTowerProcessors {
    fun sorcererSummon(
        stage: TowerPathDefinition,
        targetUuid: UUID?,
        context: TowerAttackContext
    ): SummonRequest {
        val kind = when {
            "kamikaze" in stage.abilityTags -> SummonKind.SORCERER_KAMIKAZE
            else -> SummonKind.SORCERER_ANIMAL
        }
        val maxActive = when {
            "up_to_4_animals" in stage.abilityTags -> 4
            else -> null
        }
        return SummonRequest(
            kind, context.towerInstanceId, context.ownerUuid,
            targetUuid, maxActive
        )
    }

    fun necromancerSummon(
        stage: TowerPathDefinition,
        targetUuid: UUID?,
        context: TowerAttackContext
    ): SummonRequest {
        val kind = when {
            "shulker" in stage.abilityTags -> SummonKind.SHULKER
            "snowman" in stage.abilityTags -> SummonKind.SNOWMAN
            else -> SummonKind.IRON_GOLEM
        }
        return SummonRequest(
            kind, context.towerInstanceId, context.ownerUuid,
            targetUuid, null
        )
    }

    fun babyZeusSummon(
        stage: TowerPathDefinition,
        targetUuid: UUID?,
        context: TowerAttackContext
    ): SummonRequest {
        check("baby_zeus" in stage.abilityTags)
        return SummonRequest(
            SummonKind.BABY_ZEUS,
            context.towerInstanceId,
            context.ownerUuid,
            targetUuid,
            null,
            metadata = mapOf(
                "exactBabyStats" to "UNRESOLVED_OR_STRUCTURED_IMPORT_PENDING"
            )
        )
    }

    fun zeusChain(
        stage: TowerPathDefinition,
        primaryTarget: UUID,
        orderedNextTargets: List<UUID>,
        context: TowerAttackContext
    ): AttackBatch {
        val damage = stage.stats.damage ?: error("Zeus damage unresolved")
        val maxAdditional = if ("bounce_up_to_5" in stage.abilityTags) 4 else 0
        val chain = DeterministicChainResolver.resolve(
            primaryTarget,
            orderedNextTargets,
            maxAdditional,
            damage
        )
        return AttackBatch(
            primaryTarget,
            chain.map {
                AttackImpact(
                    it.targetUuid,
                    AttackDeliveryKind.CHAIN,
                    listOf(DamageComponent(DamageKind.LIGHTNING, it.damage)),
                    sourceTowerInstanceId = context.towerInstanceId,
                    sourceOwnerUuid = context.ownerUuid
                )
            },
            mapOf("tower" to "zeus")
        )
    }

    fun turretBounce(
        stage: TowerPathDefinition,
        primaryTarget: UUID,
        orderedNextTargets: List<UUID>,
        context: TowerAttackContext
    ): AttackBatch {
        val damage = stage.stats.damage ?: error("Turret damage unresolved")
        val maxAdditional = if ("bounce_up_to_5" in stage.abilityTags) 4 else 0
        val chain = DeterministicChainResolver.resolve(
            primaryTarget,
            orderedNextTargets,
            maxAdditional,
            damage
        )
        return AttackBatch(
            primaryTarget,
            chain.map {
                AttackImpact(
                    it.targetUuid,
                    AttackDeliveryKind.BOUNCE,
                    listOf(DamageComponent(DamageKind.PHYSICAL, it.damage)),
                    sourceTowerInstanceId = context.towerInstanceId,
                    sourceOwnerUuid = context.ownerUuid
                )
            },
            mapOf("tower" to "turret")
        )
    }

    fun leachBeam(
        stage: TowerPathDefinition,
        targetUuids: Collection<UUID>,
        context: TowerAttackContext
    ): AttackBatch {
        val direct = stage.stats.damage ?: error("Leach direct damage unresolved")
        val beam = stage.stats.extras["beamDamage2021"]
            ?: stage.stats.extras["bonusDeathRay2020"]
            ?: error("Leach beam damage metadata unresolved")

        val targets = targetUuids.distinct().sortedBy(UUID::toString)
        require(targets.isNotEmpty())

        return AttackBatch(
            targets.first(),
            targets.map { uuid ->
                AttackImpact(
                    uuid,
                    AttackDeliveryKind.BEAM,
                    listOf(
                        DamageComponent(
                            DamageKind.LIGHTNING,
                            if (uuid == targets.first()) direct + beam else beam
                        )
                    ),
                    sourceTowerInstanceId = context.towerInstanceId,
                    sourceOwnerUuid = context.ownerUuid
                )
            },
            mapOf("tower" to "leach")
        )
    }
}
