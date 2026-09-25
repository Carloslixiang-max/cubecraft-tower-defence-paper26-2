package dev.cubecrafttd.mob

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.status.*
import dev.cubecrafttd.truth.ResolvedTruth

data class MobStatusDamageResolvedConfig(
    val burnIntervalTicks:
        ResolvedTruth<Long>
) {
    init {
        require(burnIntervalTicks.value > 0L)
    }
}

data class MobStatusDamageTickMetrics(
    var mobsVisited: Int = 0,
    var pulsesApplied: Int = 0,
    var totalDamage: Double = 0.0,
    var lethalResolutions: Int = 0
)

/**
 * Applies periodic damage for already-authorized damage-over-time statuses.
 *
 * Poison carries its recovered per-effect interval from the tower definition.
 * Burn cadence remains external TruthGate/Engineering fallback data.
 */
class MobStatusDamageTickPhase(
    private val config:
        MobStatusDamageResolvedConfig,
    private val lethalResolver:
        MobLethalHitResolver =
        MobLethalHitResolver()
) : ArenaTickPhase {
    override val order: Int =
        NormalArenaPhaseOrder.STATUS_DAMAGE
    override val id: String =
        "status-damage"

    private val metrics=
        MobStatusDamageTickMetrics()

    override fun tick(
        context: ArenaContext
    ) {
        context.entityIndex
            .mobsByUuid
            .values
            .toList()
            .sortedBy {
                it.identity.instanceId.value
            }
            .forEach { mob ->
                if(
                    mob.combat.lifecycle ==
                        MobLifecycleState.DEAD ||
                    mob.combat.lifecycle ==
                        MobLifecycleState.REMOVED
                ) return@forEach

                metrics.mobsVisited++

                // StatusEffectSet uses expireTick as an exclusive boundary.
                mob.statusEffects
                    .removeExpired(
                        context.gameTick
                    )

                listOf(
                    StatusEffectType.POISON,
                    StatusEffectType.BURN
                ).forEach { type ->
                    if(
                        mob.combat.lifecycle ==
                            MobLifecycleState.DEAD ||
                        mob.combat.lifecycle ==
                            MobLifecycleState.REMOVED
                    ) return@forEach

                    val effect=
                        mob.statusEffects
                            .get(type)
                            ?: return@forEach
                    val interval=
                        effect.tickIntervalTicks
                            ?: when(type) {
                                StatusEffectType.BURN ->
                                    config
                                        .burnIntervalTicks
                                        .value
                                else ->
                                    error(
                                        "Periodic status $type has no tick interval"
                                    )
                            }
                    require(interval > 0L)

                    val elapsed=
                        context.gameTick -
                            effect.appliedTick
                    if(
                        elapsed <= 0L ||
                        elapsed % interval != 0L
                    ) return@forEach

                    val damage=
                        effect.magnitude
                    if(damage <= 0.0)
                        return@forEach

                    val source=
                        if(
                            effect.sourcePlayerUuid !=
                                null &&
                            effect.sourceTowerInstanceId !=
                                null
                        ) {
                            DamageSourceIdentity
                                .PlayerTower(
                                    effect.sourcePlayerUuid,
                                    effect.sourceTowerInstanceId
                                )
                        } else {
                            mob.combat
                                .lastEligibleDamageSource
                                ?: DamageSourceIdentity
                                    .System(
                                        "status:$type"
                                    )
                        }

                    mob.combat
                        .lastEligibleDamageSource=
                        source
                    mob.combat.health=
                        (
                            mob.combat.health -
                                damage
                        ).coerceAtLeast(0.0)
                    metrics.pulsesApplied++
                    metrics.totalDamage += damage

                    if(
                        mob.combat.health <= 0.0
                    ) {
                        lethalResolver.resolve(
                            mob,
                            source
                        )
                        metrics.lethalResolutions++
                    }
                }
            }
    }

    fun metricsSnapshot():
        MobStatusDamageTickMetrics =
        metrics.copy()
}
