package dev.cubecrafttd.combat

import dev.cubecrafttd.arena.ArenaEntityIndex
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.status.StatusEffectType
import dev.cubecrafttd.tower.*
import java.util.UUID

data class AppliedMobImpact(
    val targetUuid: UUID,
    val damageApplied: Double,
    val effectsApplied: List<StatusEffectType>,
    val killed: Boolean,
    val attribution: KillAttributionDecision?
)

data class AttackApplicationReport(
    val primaryTargetUuid: UUID,
    val impacts: List<AppliedMobImpact>
) {
    val killedCount: Int get() = impacts.count { it.killed }
    val totalDamage: Double get() = impacts.sumOf { it.damageApplied }
}

class AttackBatchApplier(
    private val index: ArenaEntityIndex,
    private val towerDefinitions: TowerDefinitionRepository =
        RecommendedMatureTowerDefinitions,
    private val lethalResolver:
        MobLethalHitResolver =
        MobLethalHitResolver()
) {
    fun apply(batch: AttackBatch): AttackApplicationReport {
        val towerType = batch.metadata["tower"]
            ?: error("AttackBatch missing tower metadata")
        val definition = towerDefinitions.get(towerType)
        val capability = TowerAttackCapability(
            towerType = towerType,
            targetLayers =
                definition.targetLayers +
                    TargetLayer.BOSS,
            damageKinds = definition.damageKinds,
            effectKinds = definition.effectKinds
        )

        val results = batch.impacts.map { impact ->
            applyImpact(impact, capability)
        }
        return AttackApplicationReport(
            batch.primaryTargetUuid,
            results
        )
    }

    private fun applyImpact(
        impact: AttackImpact,
        capability: TowerAttackCapability
    ): AppliedMobImpact {
        val mob = index.mobsByUuid[impact.targetUuid]
            ?: return AppliedMobImpact(
                impact.targetUuid,0.0,emptyList(),false,null
            )
        if (mob.combat.lifecycle == MobLifecycleState.DEAD ||
            mob.combat.lifecycle == MobLifecycleState.REMOVED
        ) {
            return AppliedMobImpact(
                impact.targetUuid,0.0,emptyList(),false,null
            )
        }

        val profile =
            RecommendedMatureMobCombatProfiles.get(mob.form.formId)
        val targetable = when (impact.delivery) {
            AttackDeliveryKind.SPLASH ->
                MobCombatEligibility.canApplyIndirectSplashDamage(
                    profile,capability
                )
            else ->
                MobCombatEligibility.directTargetable(
                    profile,capability
                )
        }

        if (!targetable) {
            return AppliedMobImpact(
                impact.targetUuid,0.0,emptyList(),false,null
            )
        }

        val allowedDamages = impact.damages.filter {
            it.kind !in profile.damageImmunities
        }
        val damage = allowedDamages.sumOf { it.amount }

        val appliedEffects = impact.effects.filter { effect ->
            effectAllowed(profile,effect.type)
        }
        appliedEffects.forEach(mob.statusEffects::apply)

        var killed = false
        var attribution: KillAttributionDecision? = null

        if (damage > 0.0) {
            val source = impact.sourceOwnerUuid?.let {
                DamageSourceIdentity.PlayerTower(
                    ownerUuid = it,
                    towerInstanceId =
                        impact.sourceTowerInstanceId
                )
            } ?: DamageSourceIdentity.System(
                "tower-without-owner"
            )
            mob.combat.lastEligibleDamageSource = source
            mob.combat.health =
                (mob.combat.health - damage).coerceAtLeast(0.0)

            if(
                mob.combat.health <= 0.0 &&
                mob.combat.lifecycle !=
                    MobLifecycleState.DEAD
            ) {
                val lethal=
                    lethalResolver.resolve(
                        mob,source
                    )
                killed=
                    lethal.kind ==
                        MobLethalOutcomeKind
                            .FINAL_DEATH
                attribution=
                    lethal.attribution
            }
        }

        return AppliedMobImpact(
            impact.targetUuid,
            damage,
            appliedEffects.map { it.type },
            killed,
            attribution
        )
    }

    private fun effectAllowed(
        profile: MobCombatProfile,
        type: StatusEffectType
    ): Boolean = when (type) {
        StatusEffectType.BURN ->
            DamageKind.FIRE !in profile.damageImmunities
        StatusEffectType.POISON ->
            DamageKind.POISON !in profile.damageImmunities
        StatusEffectType.ICE_SLOW ->
            MobCombatEligibility.canApplyEffect(
                profile,
                EffectKind.ICE_SLOW
            )
        StatusEffectType.STUN ->
            MobCombatEligibility.canApplyEffect(
                profile,
                EffectKind.STUN
            )
    }
}
