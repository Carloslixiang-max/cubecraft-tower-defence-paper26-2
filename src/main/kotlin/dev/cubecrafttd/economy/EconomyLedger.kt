package dev.cubecrafttd.economy

import dev.cubecrafttd.arena.TeamId
import java.util.UUID

enum class EconomyCurrency {
    MATCH_COINS,
    MATCH_EXP,
    GOLDMINE,
    PROFILE_POINTS,
    PROFILE_EXPERIENCE
}

enum class EconomyReason {
    MATCH_INITIALIZATION,
    TROOP_PURCHASE,
    TROOP_SEND_REWARD,
    MOB_KILL_REWARD,
    TOWER_PURCHASE,
    TOWER_UPGRADE,
    TOWER_SELL_REFUND,
    GOLDMINE_TICK,
    BAZAAR,
    ARMAGEDDON,
    ADMIN_TEST,
    ENGINEERING_FALLBACK,
    TRANSACTION_ROLLBACK,
    TROOP_UNLOCK_UPGRADE,
    TROOP_UNLOCK_ROLLBACK,
    TEAM_SHARE
}

data class EconomyAccount(
    val team: TeamId,
    val playerUuid: UUID?,
    val currency: EconomyCurrency
)

data class EconomyTransaction(
    val transactionId: Long,
    val arenaTick: Long,
    val account: EconomyAccount,
    val delta: Long,
    val reason: EconomyReason,
    val correlationId: String,
    val truthFieldKey: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

data class EconomyBalanceKey(
    val account: EconomyAccount
)

class EconomyLedger {
    private val balances = linkedMapOf<EconomyBalanceKey, Long>()
    private val appliedCorrelationIds = linkedSetOf<String>()
    private val transactions = mutableListOf<EconomyTransaction>()

    fun balance(account: EconomyAccount): Long = balances[EconomyBalanceKey(account)] ?: 0L

    /**
     * Idempotent by correlationId. The caller decides whether a currency is allowed to go negative.
     */
    fun apply(transaction: EconomyTransaction, allowNegative: Boolean = false): Boolean {
        if (!appliedCorrelationIds.add(transaction.correlationId)) return false
        val key = EconomyBalanceKey(transaction.account)
        val next = (balances[key] ?: 0L) + transaction.delta
        if (!allowNegative && next < 0L) {
            appliedCorrelationIds.remove(transaction.correlationId)
            error("Insufficient ${transaction.account.currency}: attempted balance $next")
        }
        balances[key] = next
        transactions += transaction
        return true
    }

    fun hasAppliedCorrelation(correlationId: String): Boolean =
        correlationId in appliedCorrelationIds

    fun history(): List<EconomyTransaction> = transactions.toList()
}
