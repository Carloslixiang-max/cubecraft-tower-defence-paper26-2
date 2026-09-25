package dev.cubecrafttd.troop

import dev.cubecrafttd.economy.*

data class TroopPurchaseRequest(
    val entry: TroopQueueEntry,
    val payer: EconomyAccount,
    val totalCost: Long,
    val costTruthFieldKey: String?,
    val transactionId: Long
)

class TroopPurchaseService(
    private val ledger: EconomyLedger
) {
    /**
     * Intended for the single arena-owned mutation thread/tick.
     * All deterministic preconditions are checked before the debit is committed.
     */
    fun purchaseAndEnqueue(
        queue: TroopSendQueue,
        request: TroopPurchaseRequest,
        arenaTick: Long
    ) {
        require(request.totalCost >= 0)
        check(queue.canAccept(request.entry)) { "Troop queue cannot accept entry" }
        check(ledger.balance(request.payer) >= request.totalCost) { "Insufficient match currency" }

        val debit = EconomyTransaction(
            transactionId = request.transactionId,
            arenaTick = arenaTick,
            account = request.payer,
            delta = -request.totalCost,
            reason = EconomyReason.TROOP_PURCHASE,
            correlationId = request.entry.purchaseCorrelationId,
            truthFieldKey = request.costTruthFieldKey,
            metadata = mapOf(
                "mobId" to request.entry.mobId,
                "level" to request.entry.level.toString(),
                "quantity" to request.entry.quantity.toString()
            )
        )
        check(ledger.apply(debit)) { "Purchase correlation already applied" }
        queue.enqueueAfterPreflight(request.entry)
    }
}
