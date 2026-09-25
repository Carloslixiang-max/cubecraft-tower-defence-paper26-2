package dev.cubecrafttd.troop

import dev.cubecrafttd.arena.TeamId
import java.util.UUID
import java.util.ArrayDeque

@JvmInline
value class TroopQueueEntryId(val value: Long)

enum class TroopQueueState { QUEUED, SPAWNING, COMPLETE, CANCELLED }

data class TroopQueueEntry(
    val entryId: TroopQueueEntryId,
    val senderPlayerUuid: UUID,
    val attackedTeam: TeamId,
    val mobId: String,
    val level: Int,
    val quantity: Int,
    val enqueuedAtTick: Long,
    val purchaseCorrelationId: String,
    var spawned: Int = 0,
    var state: TroopQueueState = TroopQueueState.QUEUED
) {
    init {
        require(quantity > 0)
        require(spawned in 0..quantity)
    }
    val remaining: Int get() = quantity - spawned
}

class TroopSendQueue(
    val attackedTeam: TeamId,
    val capacityUnits: Int
) {
    private val queue = ArrayDeque<TroopQueueEntry>()
    private val ids = linkedSetOf<TroopQueueEntryId>()

    fun usedUnits(): Int = queue
        .filter { it.state == TroopQueueState.QUEUED || it.state == TroopQueueState.SPAWNING }
        .sumOf { it.remaining }

    fun canEnqueue(quantity: Int): Boolean = quantity > 0 && usedUnits() + quantity <= capacityUnits

    fun canAccept(entry: TroopQueueEntry): Boolean =
        entry.attackedTeam == attackedTeam &&
        entry.entryId !in ids &&
        canEnqueue(entry.quantity)

    fun enqueue(entry: TroopQueueEntry) {
        check(canAccept(entry)) { "Troop queue cannot accept entry ${entry.entryId.value}" }
        ids += entry.entryId
        queue.addLast(entry)
    }

    internal fun enqueueAfterPreflight(entry: TroopQueueEntry) {
        check(canAccept(entry)) { "Troop queue state changed after deterministic preflight" }
        ids += entry.entryId
        queue.addLast(entry)
    }

    fun peek(): TroopQueueEntry? = queue.firstOrNull()

    /**
     * Called only by the arena tick loop. This gives deterministic FIFO ordering.
     */
    fun recordOneSpawn(entryId: TroopQueueEntryId) {
        val entry = queue.firstOrNull() ?: error("Queue empty")
        check(entry.entryId == entryId) { "Only FIFO head may spawn" }
        check(entry.state == TroopQueueState.QUEUED || entry.state == TroopQueueState.SPAWNING)
        entry.state = TroopQueueState.SPAWNING
        entry.spawned++
        if (entry.spawned == entry.quantity) {
            entry.state = TroopQueueState.COMPLETE
            queue.removeFirst()
            ids.remove(entry.entryId)
        }
    }

    fun cancelBySender(sender: UUID): List<TroopQueueEntry> {
        val removed = queue.filter { it.senderPlayerUuid == sender }.toList()
        if (removed.isEmpty()) return emptyList()
        queue.removeIf {
            if (it.senderPlayerUuid == sender) {
                it.state = TroopQueueState.CANCELLED
                ids.remove(it.entryId)
                true
            } else false
        }
        return removed
    }

    fun snapshot(): List<TroopQueueEntry> = queue.toList()
}
