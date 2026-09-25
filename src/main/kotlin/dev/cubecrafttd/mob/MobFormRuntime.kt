package dev.cubecrafttd.mob

enum class MobFormTransitionReason {
    LEVEL_DEFINED,
    SHRINK_ON_DEATH_TRIGGER,
    PHASE_THRESHOLD,
    ARMAGEDDON,
    ENGINEERING_TEST
}

data class MobFormState(
    val formId: String,
    val phaseIndex: Int = 0,
    val transitionReason: MobFormTransitionReason = MobFormTransitionReason.LEVEL_DEFINED
)

object RecommendedMatureMobFormResolver {
    fun initialForm(mobId: String, level: Int): MobFormState = when (mobId) {
        "spider" -> {
            if (level >= 4) MobFormState("cave_spider")
            else MobFormState("spider")
        }
        "skeleton" -> {
            if (level >= 4) MobFormState("wither_skeleton")
            else MobFormState("skeleton")
        }
        "silverfish" -> {
            if (level >= 4) MobFormState("endermite")
            else MobFormState("silverfish")
        }
        "slime" -> {
            if (level >= 4) MobFormState("magma_cube")
            else MobFormState("slime")
        }
        else -> MobFormState(mobId)
    }
}

data class ShrinkPhaseResult(
    val nextForm: MobFormState,
    val nextMaxHealth: Double,
    val nextHealth: Double,
    val shouldContinue: Boolean
)

object SlimeShrinkPhaseService {
    /**
     * 2021 mechanic: every shrink step removes 25% of current max health.
     * Exact number of allowed shrink phases is kept caller-defined because the
     * structured data confirms the per-step loss, not a universal phase count.
     */
    fun shrink(
        current: MobFormState,
        currentMaxHealth: Double,
        currentHealth: Double,
        maxPhaseIndexInclusive: Int
    ): ShrinkPhaseResult {
        require(currentMaxHealth > 0.0)
        require(currentHealth >= 0.0)
        require(maxPhaseIndexInclusive >= current.phaseIndex)

        if (current.phaseIndex >= maxPhaseIndexInclusive) {
            return ShrinkPhaseResult(current, currentMaxHealth, currentHealth, false)
        }

        val nextMax = currentMaxHealth * 0.75
        val preservedFraction = if (currentMaxHealth == 0.0) 0.0
            else (currentHealth / currentMaxHealth).coerceIn(0.0, 1.0)
        val nextHealth = (nextMax * preservedFraction).coerceIn(0.0, nextMax)

        return ShrinkPhaseResult(
            nextForm = current.copy(
                phaseIndex = current.phaseIndex + 1,
                transitionReason = MobFormTransitionReason.SHRINK_ON_DEATH_TRIGGER
            ),
            nextMaxHealth = nextMax,
            nextHealth = nextHealth,
            shouldContinue = true
        )
    }
    /**
     * 2021 lethal trigger behavior: after a non-final "kill", the same logical
     * troop continues in a smaller phase with 25% less max health.
     *
     * Since a continuing troop cannot remain at zero HP, the new phase starts
     * at its reduced maximum. The exact number of phases remains externally
     * resolved.
     */
    fun shrinkAfterLethalHit(
        current: MobFormState,
        currentMaxHealth: Double,
        maxPhaseIndexInclusive: Int
    ): ShrinkPhaseResult {
        require(currentMaxHealth > 0.0)
        require(maxPhaseIndexInclusive >= current.phaseIndex)

        if(current.phaseIndex >= maxPhaseIndexInclusive) {
            return ShrinkPhaseResult(
                current,
                currentMaxHealth,
                0.0,
                false
            )
        }

        val nextMax=currentMaxHealth*0.75
        return ShrinkPhaseResult(
            nextForm=current.copy(
                phaseIndex=current.phaseIndex+1,
                transitionReason=
                    MobFormTransitionReason
                        .SHRINK_ON_DEATH_TRIGGER
            ),
            nextMaxHealth=nextMax,
            nextHealth=nextMax,
            shouldContinue=true
        )
    }


}

data class GiantRunState(
    val running: Boolean,
    val thresholdFraction: Double
)

object GiantPhaseService {
    fun runState(
        currentHealth: Double,
        maxHealth: Double,
        thresholdFraction: Double = 0.20
    ): GiantRunState {
        require(maxHealth > 0.0)
        require(thresholdFraction in 0.0..1.0)
        return GiantRunState(
            running = currentHealth / maxHealth <= thresholdFraction,
            thresholdFraction = thresholdFraction
        )
    }
}
