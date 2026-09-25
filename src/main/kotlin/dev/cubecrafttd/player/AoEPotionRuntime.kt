package dev.cubecrafttd.player

import dev.cubecrafttd.arena.ArenaEntityIndex
import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.status.*
import dev.cubecrafttd.truth.ResolvedTruth
import dev.cubecrafttd.ui.AoEPotionDefinition
import java.util.UUID
import kotlin.math.min

enum class AoEPotionTargetRelation {
    ENEMY_TROOPS,
    FRIENDLY_TROOPS
}

data class AoEPotionTargetCandidate(
    val mobUuid: UUID,
    val relation: AoEPotionTargetRelation,
    /**
     * Geometry is resolved outside core. This boolean says the mob is inside
     * the potion's actual world/path footprint.
     */
    val insideEffectArea: Boolean
)

data class FreezePotionResolvedConfig(
    val slowMagnitude:
        ResolvedTruth<Double>,
    val durationTicks:
        ResolvedTruth<Long>
)

data class InfernoPotionResolvedConfig(
    val damagePerSecond:
        ResolvedTruth<Double>,
    val pulseIntervalTicks:
        ResolvedTruth<Long>
)

data class MeteorPotionResolvedConfig(
    val pulseIntervalTicks:
        ResolvedTruth<Long>
)

data class ZeusPotionResolvedConfig(
    val damagePerBolt:
        ResolvedTruth<Double>,
    val boltCount:
        ResolvedTruth<Int>
)

data class SpeedPotionResolvedConfig(
    val speedMultiplier:
        ResolvedTruth<Double>,
    val durationTicks:
        ResolvedTruth<Long>
)

data class HealPotionResolvedConfig(
    val healAmount:
        ResolvedTruth<Double>
)

sealed interface AoEPotionRuntimeConfig {
    data class Freeze(
        val value:
            FreezePotionResolvedConfig
    ) : AoEPotionRuntimeConfig
    data class Inferno(
        val value:
            InfernoPotionResolvedConfig
    ) : AoEPotionRuntimeConfig
    data class Meteor(
        val value:
            MeteorPotionResolvedConfig
    ) : AoEPotionRuntimeConfig
    data class Zeus(
        val value:
            ZeusPotionResolvedConfig
    ) : AoEPotionRuntimeConfig
    data class Speed(
        val value:
            SpeedPotionResolvedConfig
    ) : AoEPotionRuntimeConfig
    data class Heal(
        val value:
            HealPotionResolvedConfig
    ) : AoEPotionRuntimeConfig
}

data class AoEPotionPulse(
    val tickOffset: Long,
    val targetMobUuids: List<UUID>,
    val damagePerTarget: Double = 0.0,
    val effect: StatusEffectInstance? = null,
    val healPerTarget: Double = 0.0,
    val speedMultiplier: Double? = null
)

data class AoEPotionActionPlan(
    val potionId: String,
    val ownerUuid: UUID,
    val pulses: List<AoEPotionPulse>,
    val attributionDeferred: Boolean = true
)

object AoEPotionRuntimePlanner {
    fun plan(
        definition: AoEPotionDefinition,
        config: AoEPotionRuntimeConfig,
        ownerUuid: UUID,
        gameTick: Long,
        candidates:
            Collection<AoEPotionTargetCandidate>,
        index: ArenaEntityIndex
    ): AoEPotionActionPlan {
        val eligible = candidates.asSequence()
            .filter {
                it.insideEffectArea
            }
            .mapNotNull {
                val mob =
                    index.mobsByUuid[it.mobUuid]
                        ?: return@mapNotNull null
                if (
                    mob.combat.lifecycle ==
                        MobLifecycleState.DEAD ||
                    mob.combat.lifecycle ==
                        MobLifecycleState.REMOVED
                ) return@mapNotNull null

                // Wither is immune to AoE potions.
                if (mob.form.formId == "wither") {
                    return@mapNotNull null
                }

                val expectedRelation =
                    when (definition.potionId) {
                        "speed","heal" ->
                            AoEPotionTargetRelation
                                .FRIENDLY_TROOPS
                        else ->
                            AoEPotionTargetRelation
                                .ENEMY_TROOPS
                    }
                if (
                    it.relation !=
                        expectedRelation
                ) return@mapNotNull null
                mob
            }
            .sortedBy {
                it.identity.entityUuid
                    .toString()
            }
            .toList()

        val capped =
            definition.maxAffectedTroops
                ?.let {
                    eligible.take(
                        min(it,eligible.size)
                    )
                }
                ?: eligible

        val uuids =
            capped.map {
                it.identity.entityUuid
            }

        val pulses = when (config) {
            is AoEPotionRuntimeConfig.Freeze -> {
                require(
                    definition.potionId=="freeze"
                )
                listOf(
                    AoEPotionPulse(
                        tickOffset=0,
                        targetMobUuids=uuids,
                        effect=StatusEffectInstance(
                            StatusEffectType.ICE_SLOW,
                            "potion:$ownerUuid",
                            config.value
                                .slowMagnitude.value,
                            gameTick,
                            gameTick +
                                config.value
                                    .durationTicks.value
                        )
                    )
                )
            }

            is AoEPotionRuntimeConfig.Inferno -> {
                require(
                    definition.potionId=="inferno"
                )
                val durationTicks =
                    (
                        (
                            definition.durationSeconds
                                ?: error(
                                    "Inferno duration unresolved"
                                )
                        ) * 20.0
                    ).toLong()
                val step =
                    config.value
                        .pulseIntervalTicks.value
                require(step > 0L)
                buildList {
                    var offset=0L
                    while (
                        offset < durationTicks
                    ) {
                        add(
                            AoEPotionPulse(
                                offset,
                                uuids,
                                damagePerTarget =
                                    config.value
                                        .damagePerSecond
                                        .value *
                                        step.toDouble() /
                                        20.0
                            )
                        )
                        offset += step
                    }
                }
            }

            is AoEPotionRuntimeConfig.Meteor -> {
                require(
                    definition.potionId=="meteor"
                )
                val count =
                    definition.count
                        ?: error(
                            "Meteor count unresolved"
                        )
                val damage =
                    definition
                        .damagePerTickOrMeteor
                        ?: error(
                            "Meteor damage unresolved"
                        )
                val step =
                    config.value
                        .pulseIntervalTicks.value
                require(step > 0L)
                List(count) { i ->
                    AoEPotionPulse(
                        i * step,
                        uuids,
                        damagePerTarget=damage
                    )
                }
            }

            is AoEPotionRuntimeConfig.Zeus -> {
                require(
                    definition.potionId=="zeus"
                )
                val count =
                    config.value.boltCount.value
                require(count > 0)
                List(count) { i ->
                    AoEPotionPulse(
                        i.toLong(),
                        uuids,
                        damagePerTarget =
                            config.value
                                .damagePerBolt.value
                    )
                }
            }

            is AoEPotionRuntimeConfig.Speed -> {
                require(
                    definition.potionId=="speed"
                )
                listOf(
                    AoEPotionPulse(
                        tickOffset=0L,
                        targetMobUuids=uuids,
                        effect=
                            StatusEffectInstance(
                                type=
                                    StatusEffectType
                                        .SPEED_BOOST,
                                sourceId=
                                    "potion:$ownerUuid",
                                magnitude=
                                    config.value
                                        .speedMultiplier
                                        .value,
                                appliedTick=
                                    gameTick,
                                expireTick=
                                    gameTick +
                                        config.value
                                            .durationTicks
                                            .value
                            ),
                        speedMultiplier =
                            config.value
                                .speedMultiplier.value
                    )
                )
            }

            is AoEPotionRuntimeConfig.Heal -> {
                require(
                    definition.potionId=="heal"
                )
                listOf(
                    AoEPotionPulse(
                        0L,uuids,
                        healPerTarget =
                            config.value
                                .healAmount.value
                    )
                )
            }
        }

        return AoEPotionActionPlan(
            definition.potionId,
            ownerUuid,
            pulses
        )
    }
}
