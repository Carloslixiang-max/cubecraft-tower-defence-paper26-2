package dev.cubecrafttd.mob

import dev.cubecrafttd.combat.*
import dev.cubecrafttd.truth.ResolvedTruth

enum class MobLethalOutcomeKind {
    SURVIVED,
    SHRUNK,
    FINAL_DEATH
}

data class MobLethalOutcome(
    val kind: MobLethalOutcomeKind,
    val attribution: KillAttributionDecision? = null
)

data class SlimeLethalConfig(
    val maxShrinkPhaseIndex:
        ResolvedTruth<Int>
) {
    init {
        require(
            maxShrinkPhaseIndex.value >= 0
        )
    }
}

class MobLethalHitResolver(
    private val slimeConfig:
        SlimeLethalConfig? = null
) {
    fun resolve(
        mob: MobRuntimeState,
        source: DamageSourceIdentity
    ): MobLethalOutcome {
        if(mob.combat.health > 0.0) {
            return MobLethalOutcome(
                MobLethalOutcomeKind.SURVIVED
            )
        }

        if(mob.identity.mobId == "slime") {
            val config=slimeConfig
                ?: error(
                    "Slime lethal phase count is unresolved; explicit SlimeLethalConfig required"
                )

            val beforeLifecycle=
                mob.combat.lifecycle
            val result=
                SlimeShrinkPhaseService
                    .shrinkAfterLethalHit(
                        current=mob.form,
                        currentMaxHealth=
                            mob.combat.maxHealth,
                        maxPhaseIndexInclusive=
                            config
                                .maxShrinkPhaseIndex
                                .value
                    )

            if(result.shouldContinue) {
                mob.form=result.nextForm
                mob.combat.maxHealth=
                    result.nextMaxHealth
                mob.combat.health=
                    result.nextHealth
                mob.combat.lifecycle=
                    beforeLifecycle
                return MobLethalOutcome(
                    MobLethalOutcomeKind.SHRUNK
                )
            }
        }

        mob.combat.health=0.0
        mob.combat.lifecycle=
            MobLifecycleState.DEAD
        return MobLethalOutcome(
            MobLethalOutcomeKind.FINAL_DEATH,
            KillAttributionService
                .matureFinalBlow(source)
        )
    }
}
