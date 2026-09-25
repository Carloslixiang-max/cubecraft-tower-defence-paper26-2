package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.troop.*
import java.util.UUID

object EconomyAndQueueFixture {
    fun run(): List<FixtureResult> {
        val sender = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val account = EconomyAccount(TeamId.RED, sender, EconomyCurrency.MATCH_COINS)
        val ledger = EconomyLedger()
        ledger.apply(EconomyTransaction(1, 0, account, 100, EconomyReason.MATCH_INITIALIZATION, "init"))
        val duplicateApplied = ledger.apply(
            EconomyTransaction(2, 1, account, 100, EconomyReason.MATCH_INITIALIZATION, "init")
        )

        val queue = TroopSendQueue(TeamId.BLUE, capacityUnits = 12)
        val e = TroopQueueEntry(
            TroopQueueEntryId(1), sender, TeamId.BLUE, "zombie", 1,
            quantity = 3, enqueuedAtTick = 10, purchaseCorrelationId = "purchase-1"
        )
        queue.enqueue(e)
        queue.recordOneSpawn(e.entryId)
        queue.recordOneSpawn(e.entryId)
        queue.recordOneSpawn(e.entryId)

        return listOf(
            FixtureResult("economy-idempotent-correlation", !duplicateApplied && ledger.balance(account) == 100L),
            FixtureResult("troop-fifo-completion", queue.snapshot().isEmpty() && e.state == TroopQueueState.COMPLETE)
        )
    }
}
