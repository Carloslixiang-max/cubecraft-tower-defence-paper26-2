package dev.cubecrafttd.tower

import dev.cubecrafttd.combat.*
import dev.cubecrafttd.mob.DamageKind
import dev.cubecrafttd.status.*
import java.util.UUID

data class TowerAttackContext(
    val towerInstanceId: Long,
    val ownerUuid: UUID?,
    val gameTick: Long
)

object BasicTowerProcessors {
    fun archer(
        stage: TowerPathDefinition,
        targetUuid: UUID,
        context: TowerAttackContext
    ): AttackBatch {
        val damage = stage.stats.damage ?: error("Archer damage unresolved")
        return AttackBatch(
            targetUuid,
            listOf(
                AttackImpact(
                    targetUuid, AttackDeliveryKind.DIRECT,
                    listOf(DamageComponent(DamageKind.PHYSICAL, damage)),
                    sourceTowerInstanceId = context.towerInstanceId,
                    sourceOwnerUuid = context.ownerUuid
                )
            ),
            metadata = mapOf("tower" to "archer", "level" to stage.level.toString())
        )
    }

    fun artillery(
        stage: TowerPathDefinition,
        primaryTargetUuid: UUID,
        affectedTargets: Collection<UUID>,
        context: TowerAttackContext
    ): AttackBatch {
        val direct = stage.stats.damage ?: error("Artillery damage unresolved")
        val frag = stage.stats.extras["fragDamage"] ?: direct
        val stunSeconds = stage.stats.extras["stunSeconds"]

        val impacts = affectedTargets.distinct().map { uuid ->
            val effects = if (stunSeconds != null && stunSeconds > 0.0) {
                listOf(
                    StatusEffectInstance(
                        StatusEffectType.STUN,
                        "tower:${context.towerInstanceId}",
                        1.0,
                        context.gameTick,
                        context.gameTick + (stunSeconds * 20.0).toLong()
                    )
                )
            } else emptyList()
            AttackImpact(
                uuid,
                if (uuid == primaryTargetUuid) AttackDeliveryKind.DIRECT else AttackDeliveryKind.SPLASH,
                listOf(
                    DamageComponent(
                        DamageKind.PHYSICAL,
                        if (uuid == primaryTargetUuid) direct else frag
                    )
                ),
                effects,
                context.towerInstanceId,
                context.ownerUuid
            )
        }
        return AttackBatch(primaryTargetUuid, impacts, mapOf("tower" to "artillery"))
    }

    fun mage(
        stage: TowerPathDefinition,
        targetUuid: UUID,
        context: TowerAttackContext
    ): AttackBatch {
        val ignition = stage.stats.damage ?: error("Mage ignition damage unresolved")
        val fireDamage = stage.stats.extras["fireDamage"] ?: 0.0
        val durationSeconds = stage.stats.extras["burnDurationSeconds"] ?: 0.0
        val effect = StatusEffectInstance(
            StatusEffectType.BURN,
            "tower:${context.towerInstanceId}",
            fireDamage,
            context.gameTick,
            context.gameTick + (durationSeconds * 20.0).toLong()
        )
        return AttackBatch(
            targetUuid,
            listOf(
                AttackImpact(
                    targetUuid,
                    AttackDeliveryKind.DIRECT,
                    listOf(DamageComponent(DamageKind.FIRE, ignition)),
                    if (durationSeconds > 0.0) listOf(effect) else emptyList(),
                    context.towerInstanceId,
                    context.ownerUuid
                )
            ),
            mapOf("tower" to "mage")
        )
    }

    fun ice(
        stage: TowerPathDefinition,
        targetUuid: UUID,
        context: TowerAttackContext,
        chanceRoll: Double? = null
    ): AttackBatch {
        val effects = listOf(
            StatusEffectInstance(
                StatusEffectType.ICE_SLOW,
                "tower:${context.towerInstanceId}",
                1.0,
                context.gameTick,
                context.gameTick + 20L
            )
        )
        val chance = stage.stats.extras["damageChance2021"]
        val damage = when {
            chance == null -> stage.stats.damage ?: 0.0
            chanceRoll == null -> error("Ice chance damage requires deterministic arena RNG roll")
            chanceRoll < chance -> stage.stats.damage ?: 0.0
            else -> 0.0
        }
        return AttackBatch(
            targetUuid,
            listOf(
                AttackImpact(
                    targetUuid,
                    AttackDeliveryKind.DIRECT,
                    if (damage > 0.0) listOf(DamageComponent(DamageKind.PHYSICAL, damage)) else emptyList(),
                    effects,
                    context.towerInstanceId,
                    context.ownerUuid
                )
            ),
            mapOf("tower" to "ice")
        )
    }

    fun poison(
        stage: TowerPathDefinition,
        targetUuid: UUID,
        context: TowerAttackContext
    ): AttackBatch {
        val poisonDamage = stage.stats.extras["poisonDamage"]
        val duration = stage.stats.extras["durationSeconds"]
            ?: stage.stats.extras["durationSecondsTable"]
        val effects = if (poisonDamage != null && duration != null) {
            listOf(
                StatusEffectInstance(
                    StatusEffectType.POISON,
                    "tower:${context.towerInstanceId}",
                    poisonDamage,
                    context.gameTick,
                    context.gameTick + (duration * 20.0).toLong()
                )
            )
        } else emptyList()
        val immediate = stage.stats.damage ?: 0.0
        return AttackBatch(
            targetUuid,
            listOf(
                AttackImpact(
                    targetUuid,
                    if (effects.isEmpty()) AttackDeliveryKind.DIRECT else AttackDeliveryKind.DOT,
                    if (immediate > 0.0) listOf(DamageComponent(DamageKind.POISON, immediate)) else emptyList(),
                    effects,
                    context.towerInstanceId,
                    context.ownerUuid
                )
            ),
            mapOf("tower" to "poison")
        )
    }

    fun quake(
        stage: TowerPathDefinition,
        primaryTargetUuid: UUID,
        affectedTargets: Collection<UUID>,
        context: TowerAttackContext
    ): AttackBatch {
        val damage = stage.stats.damage ?: error("Quake damage unresolved")
        val stunSeconds = stage.stats.extras["stunSeconds"] ?: 0.0
        val impacts = affectedTargets.distinct().map { uuid ->
            AttackImpact(
                uuid,
                AttackDeliveryKind.SPLASH,
                listOf(DamageComponent(DamageKind.PHYSICAL, damage)),
                if (stunSeconds > 0.0) listOf(
                    StatusEffectInstance(
                        StatusEffectType.STUN,
                        "tower:${context.towerInstanceId}",
                        1.0,
                        context.gameTick,
                        context.gameTick + (stunSeconds * 20.0).toLong()
                    )
                ) else emptyList(),
                context.towerInstanceId,
                context.ownerUuid
            )
        }
        return AttackBatch(primaryTargetUuid, impacts, mapOf("tower" to "quake"))
    }
}
