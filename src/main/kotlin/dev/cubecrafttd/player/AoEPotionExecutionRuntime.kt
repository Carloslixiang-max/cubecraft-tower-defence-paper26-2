package dev.cubecrafttd.player

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.mob.*
import java.util.UUID

data class ScheduledAoEPotionPulse(
    val executeAtTick: Long,
    val potionId: String,
    val ownerUuid: UUID,
    val awardsPlayerKillCoins: Boolean,
    val pulse: AoEPotionPulse
)

class AoEPotionRuntimeQueue {
    private val pending=
        mutableListOf<
            ScheduledAoEPotionPulse
        >()

    fun schedule(
        plan: AoEPotionActionPlan,
        committedAtTick: Long,
        awardsPlayerKillCoins: Boolean
    ) {
        plan.pulses.forEach { pulse ->
            pending +=
                ScheduledAoEPotionPulse(
                    executeAtTick=
                        committedAtTick +
                            pulse.tickOffset,
                    potionId=
                        plan.potionId,
                    ownerUuid=
                        plan.ownerUuid,
                    awardsPlayerKillCoins=
                        awardsPlayerKillCoins,
                    pulse=pulse
                )
        }
    }

    fun due(
        gameTick: Long
    ): List<ScheduledAoEPotionPulse> {
        val out=
            pending
                .filter {
                    it.executeAtTick <=
                        gameTick
                }
                .sortedWith(
                    compareBy<
                        ScheduledAoEPotionPulse
                    > {
                        it.executeAtTick
                    }.thenBy {
                        it.ownerUuid
                            .toString()
                    }.thenBy {
                        it.potionId
                    }
                )
        pending.removeAll(
            out.toSet()
        )
        return out
    }

    fun pendingCount(): Int =
        pending.size

    fun clear() {
        pending.clear()
    }
}

data class AoEPotionPulseApplicationReport(
    val potionId: String,
    val ownerUuid: UUID,
    val affected: Int,
    val damaged: Int,
    val healed: Int,
    val effectsApplied: Int,
    val killed: Int
)

fun interface AoEPotionFeedbackPort {
    fun onPulse(
        context: ArenaContext,
        scheduled: ScheduledAoEPotionPulse,
        report: AoEPotionPulseApplicationReport
    )
}

object NoOpAoEPotionFeedbackPort :
    AoEPotionFeedbackPort {
    override fun onPulse(
        context: ArenaContext,
        scheduled: ScheduledAoEPotionPulse,
        report: AoEPotionPulseApplicationReport
    ) = Unit
}

data class AoEPotionTickMetrics(
    var pulsesApplied: Int = 0,
    var mobsAffected: Int = 0,
    var kills: Int = 0
)

class AoEPotionTickPhase(
    private val queue:
        AoEPotionRuntimeQueue,
    private val lethalResolver:
        MobLethalHitResolver =
        MobLethalHitResolver(),
    private val feedback:
        AoEPotionFeedbackPort =
        NoOpAoEPotionFeedbackPort
) : ArenaTickPhase {
    override val order: Int =
        NormalArenaPhaseOrder.AOE_POTION
    override val id: String =
        "aoe-potion"

    private val metrics=
        AoEPotionTickMetrics()

    override fun tick(
        context: ArenaContext
    ) {
        queue.due(
            context.gameTick
        ).forEach { scheduled ->
            val report=
                applyPulse(
                    context,
                    scheduled
                )
            metrics.pulsesApplied++
            metrics.mobsAffected +=
                report.affected
            metrics.kills +=
                report.killed
            runCatching {
                feedback.onPulse(
                    context,
                    scheduled,
                    report
                )
            }
        }
    }

    private fun applyPulse(
        context: ArenaContext,
        scheduled:
            ScheduledAoEPotionPulse
    ): AoEPotionPulseApplicationReport {
        var affected=0
        var damaged=0
        var healed=0
        var effects=0
        var killed=0

        scheduled.pulse
            .targetMobUuids
            .distinct()
            .sortedBy {
                it.toString()
            }
            .forEach { uuid ->
                val mob=
                    context.entityIndex
                        .mobsByUuid[uuid]
                        ?: return@forEach
                if(
                    mob.combat.lifecycle ==
                        MobLifecycleState.DEAD ||
                    mob.combat.lifecycle ==
                        MobLifecycleState.REMOVED
                ) return@forEach

                affected++

                scheduled.pulse.effect
                    ?.let { effect ->
                        mob.statusEffects
                            .apply(effect)
                        effects++
                    }

                val heal=
                    scheduled.pulse
                        .healPerTarget
                if(heal>0.0) {
                    mob.combat.health=
                        (
                            mob.combat.health +
                                heal
                        ).coerceAtMost(
                            mob.combat.maxHealth
                        )
                    healed++
                }

                val damage=
                    scheduled.pulse
                        .damagePerTarget
                if(damage>0.0) {
                    val source=
                        DamageSourceIdentity
                            .PlayerPotion(
                                scheduled.ownerUuid,
                                scheduled.potionId,
                                scheduled
                                    .awardsPlayerKillCoins
                            )
                    mob.combat
                        .lastEligibleDamageSource=
                        source
                    mob.combat.health=
                        (
                            mob.combat.health -
                                damage
                        ).coerceAtLeast(0.0)
                    damaged++

                    if(
                        mob.combat.health<=0.0
                    ) {
                        val lethal=
                            lethalResolver.resolve(
                                mob,
                                source
                            )
                        if(
                            lethal.kind ==
                                MobLethalOutcomeKind
                                    .FINAL_DEATH
                        ) {
                            killed++
                        }
                    }
                }
            }

        return AoEPotionPulseApplicationReport(
            potionId=
                scheduled.potionId,
            ownerUuid=
                scheduled.ownerUuid,
            affected=affected,
            damaged=damaged,
            healed=healed,
            effectsApplied=effects,
            killed=killed
        )
    }

    fun metricsSnapshot():
        AoEPotionTickMetrics =
        metrics.copy()
}
