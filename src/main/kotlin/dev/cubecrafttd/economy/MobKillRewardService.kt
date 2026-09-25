package dev.cubecrafttd.economy

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.combat.KillAttributionDecision
import dev.cubecrafttd.mob.MobRuntimeState
import dev.cubecrafttd.truth.ResolvedTruth

fun interface MobKillRewardResolver {
    fun rewardCoins(
        mobId: String,
        level: Int
    ): ResolvedTruth<Long>
}

class MobKillRewardService(
    private val ledger: EconomyLedger,
    private val resolver: MobKillRewardResolver,
    private val pricingMode:
        PricingMode = PricingMode.NORMAL
) {
    fun award(
        mob: MobRuntimeState,
        attribution: KillAttributionDecision,
        gameTick: Long,
        transactionId: Long
    ): Long {
        if (
            !attribution.awardsPlayerKillCoins ||
            attribution.creditedPlayerUuid ==
                null
        ) return 0L

        val truth = resolver.rewardCoins(
            mob.identity.mobId,
            mob.identity.level
        )
        require(truth.value >= 0L)
        val multiplier =
            if(pricingMode==
                PricingMode.DOUBLE_INCOME)
                2L else 1L
        val amount =
            Math.multiplyExact(
                truth.value,multiplier
            )

        val defendingTeam =
            mob.identity.attackedTeam
        val account = EconomyAccount(
            defendingTeam,
            attribution.creditedPlayerUuid,
            EconomyCurrency.MATCH_COINS
        )
        val correlation =
            "mob-kill:${mob.identity.instanceId.value}"

        if (
            ledger.hasAppliedCorrelation(
                correlation
            )
        ) return 0L

        check(
            ledger.apply(
                EconomyTransaction(
                    transactionId,
                    gameTick,
                    account,
                    amount,
                    EconomyReason
                        .MOB_KILL_REWARD,
                    correlation,
                    truthFieldKey =
                        "troops.${mob.identity.mobId}." +
                            "level${mob.identity.level}." +
                            "kill_coins"
                )
            )
        )
        return amount
    }
}
