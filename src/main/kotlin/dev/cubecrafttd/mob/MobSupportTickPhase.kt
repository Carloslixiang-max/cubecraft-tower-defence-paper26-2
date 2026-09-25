package dev.cubecrafttd.mob

import dev.cubecrafttd.arena.*
import java.util.UUID
import kotlin.math.min

fun interface MobSupportGeometryProvider {
    fun distanceBlocks(
        fromEntityUuid: UUID,
        toEntityUuid: UUID
    ): Double?
}

data class MobSupportTickMetrics(
    var witchesVisited: Int = 0,
    var witchBursts: Int = 0,
    var targetsHealed: Int = 0,
    var creeperRegenTicks: Int = 0,
    var giantRegenTicks: Int = 0
)

class MobSupportTickPhase(
    private val witch: WitchHealConfig,
    private val creeperRegen:
        PassiveRegenConfig,
    private val giantRegen:
        GiantRegenConfig,
    private val geometry:
        MobSupportGeometryProvider
) : ArenaTickPhase {
    override val order: Int = 50
    override val id: String = "mob-support"

    private val witchClocks=
        linkedMapOf<
            MobInstanceId,
            WitchHealClock
        >()
    private val metrics=
        MobSupportTickMetrics()

    override fun tick(
        context: ArenaContext
    ) {
        val mobs=context.entityIndex
            .mobsByUuid.values
            .toList()
            .sortedBy {
                it.identity.instanceId.value
            }

        mobs.forEach { mob ->
            if(
                mob.combat.lifecycle !=
                    MobLifecycleState.MOVING &&
                mob.combat.lifecycle !=
                    MobLifecycleState
                        .ATTACKING_CASTLE &&
                mob.combat.lifecycle !=
                    MobLifecycleState
                        .ARMAGEDDON_BOSS
            ) return@forEach

            when(mob.identity.mobId) {
                "creeper" -> {
                    mob.combat.health=
                        PassiveRegenerationService
                            .regenerate(
                                mob.combat.health,
                                mob.combat.maxHealth,
                                creeperRegen,
                                1L
                            )
                    metrics.creeperRegenTicks++
                }

                "giant" -> {
                    mob.combat.health=
                        GiantRegenerationService
                            .regenerate(
                                mob.combat.health,
                                mob.combat.maxHealth,
                                giantRegen,
                                1L
                            )
                    metrics.giantRegenTicks++
                }

                "witch" -> {
                    metrics.witchesVisited++
                    val clock=
                        witchClocks.getOrPut(
                            mob.identity.instanceId
                        ) {
                            WitchHealClock(witch)
                        }
                    clock.arm(context.gameTick)
                    if(!clock.consume(
                            context.gameTick
                        )
                    ) {
                        return@forEach
                    }

                    val targets=mobs.asSequence()
                        .filter {
                            it.identity.attackedTeam ==
                                mob.identity.attackedTeam
                        }
                        .filter {
                            it.identity.entityUuid !=
                                mob.identity.entityUuid
                        }
                        // Mature evidence: Witches do not heal themselves
                        // and are not healed by other Witches.
                        .filter {
                            it.identity.mobId !=
                                "witch"
                        }
                        .filter {
                            it.combat.lifecycle ==
                                MobLifecycleState.MOVING ||
                            it.combat.lifecycle ==
                                MobLifecycleState
                                    .ATTACKING_CASTLE ||
                            it.combat.lifecycle ==
                                MobLifecycleState
                                    .ARMAGEDDON_BOSS
                        }
                        .mapNotNull { target ->
                            val distance=
                                geometry.distanceBlocks(
                                    mob.identity.entityUuid,
                                    target.identity.entityUuid
                                ) ?: return@mapNotNull null
                            if(
                                distance >
                                    witch
                                        .healRadiusBlocks
                                        .value
                            ) null
                            else target to distance
                        }
                        .sortedWith(
                            compareBy<Pair<MobRuntimeState,Double>> {
                                it.second
                            }.thenBy {
                                it.first.identity
                                    .entityUuid.toString()
                            }
                        )
                        .take(
                            witch.targetCap.value
                        )
                        .map { it.first }
                        .toList()

                    targets.forEach { target ->
                        val amount=
                            witch.healAmountFor(
                                target.combat
                                    .maxHealth
                            )
                        target.combat.health=
                            min(
                                target.combat
                                    .maxHealth,
                                target.combat.health+
                                    amount
                            )
                    }
                    metrics.witchBursts++
                    metrics.targetsHealed +=
                        targets.size
                }
            }
        }

        val activeIds=mobs
            .filter {
                it.identity.mobId=="witch" &&
                    it.combat.lifecycle !=
                        MobLifecycleState.DEAD &&
                    it.combat.lifecycle !=
                        MobLifecycleState.REMOVED
            }
            .mapTo(linkedSetOf()) {
                it.identity.instanceId
            }
        witchClocks.keys
            .removeIf {
                it !in activeIds
            }
    }

    fun metricsSnapshot():
        MobSupportTickMetrics =
        metrics.copy()
}
