package dev.cubecrafttd.economy

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.truth.ResolvedTruth
import java.util.UUID

data class MatchStartingBalance(
    val coins: Long,
    val exp: Long
)

object RecommendedMaturePricing {
    const val QUICK_START_COINS: Long = 1500L
    const val QUICK_START_EXP: Long = 100L

    fun startingBalance(
        mode: PricingMode,
        normalStartCoins: ResolvedTruth<Long>,
        normalStartExp: ResolvedTruth<Long>
    ): MatchStartingBalance = when (mode) {
        PricingMode.NORMAL,
        PricingMode.DOUBLE_INCOME ->
            MatchStartingBalance(
                normalStartCoins.value,
                normalStartExp.value
            )
        PricingMode.QUICK_START ->
            MatchStartingBalance(
                QUICK_START_COINS,
                QUICK_START_EXP
            )
    }

    fun seedPlayer(
        ledger: EconomyLedger,
        team: TeamId,
        playerUuid: UUID,
        balance: MatchStartingBalance,
        transactionBaseId: Long,
        correlationPrefix: String
    ) {
        ledger.apply(
            EconomyTransaction(
                transactionBaseId,
                0L,
                EconomyAccount(
                    team,playerUuid,
                    EconomyCurrency.MATCH_COINS
                ),
                balance.coins,
                EconomyReason.MATCH_INITIALIZATION,
                "$correlationPrefix:coins"
            )
        )
        ledger.apply(
            EconomyTransaction(
                transactionBaseId + 1,
                0L,
                EconomyAccount(
                    team,playerUuid,
                    EconomyCurrency.MATCH_EXP
                ),
                balance.exp,
                EconomyReason.MATCH_INITIALIZATION,
                "$correlationPrefix:exp"
            )
        )
    }
}
