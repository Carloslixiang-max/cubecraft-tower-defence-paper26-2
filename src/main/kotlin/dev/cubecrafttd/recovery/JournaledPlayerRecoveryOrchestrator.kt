package dev.cubecrafttd.recovery

import dev.cubecrafttd.player.*
import java.util.UUID

class JournaledPlayerRecoveryOrchestrator(
    private val adapter: PlayerStateAdapter,
    private val store: PlayerSnapshotStore,
    private val journal: PlayerRecoveryJournal
) : PlayerRecoveryCoordinator {
    fun recoverJournalIntoMemory() {
        journal.loadAll().forEach { snapshot ->
            if (store.record(snapshot.playerUuid) == null) store.put(snapshot)
        }
    }

    override fun captureBeforeMatch(playerUuid: UUID, arenaTick: Long) {
        check(store.record(playerUuid) == null) { "Player already has active snapshot" }
        val snapshot = adapter.capture(playerUuid, arenaTick)
        journal.save(snapshot) // durable before game mutation
        store.put(snapshot)
        adapter.prepareForMatch(playerUuid)
    }

    override fun restoreIfPossible(playerUuid: UUID): Boolean {
        if (!adapter.isOnline(playerUuid)) return false
        val snapshot = store.beginRestore(playerUuid) ?: return false
        return try {
            adapter.restore(snapshot)
            store.markRestored(playerUuid)
            journal.delete(playerUuid)
            true
        } catch (t: Throwable) {
            store.markRestoreFailed(playerUuid)
            false
        }
    }

    override fun pending(): Set<UUID> = store.unresolved()
}
