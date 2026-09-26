package dev.cubecrafttd.recovery

import dev.cubecrafttd.player.*
import java.util.UUID

class JournaledPlayerRecoveryOrchestrator(
    private val adapter: PlayerStateAdapter,
    private val store: PlayerSnapshotStore,
    private val journal: PlayerRecoveryJournal
) : PlayerRecoveryCoordinator {
    fun recoverJournalIntoMemory():
        Set<UUID> {
        val loaded=
            linkedSetOf<UUID>()
        journal.loadAll().forEach {
            snapshot ->
            if(
                store.record(
                    snapshot.playerUuid
                ) == null
            ) {
                store.put(snapshot)
                loaded += snapshot.playerUuid
            }
        }
        return loaded
    }

    fun pendingSnapshot(
        playerUuid: UUID
    ): PlayerSnapshot? =
        store.record(playerUuid)
            ?.snapshot

    override fun captureBeforeMatch(
        playerUuid: UUID,
        arenaTick: Long
    ) {
        check(
            store.canCapture(
                playerUuid
            )
        ) {
            "Player already has active snapshot"
        }
        val snapshot=
            adapter.capture(
                playerUuid,
                arenaTick
            )
        journal.save(snapshot)
        store.put(snapshot)

        try {
            adapter.prepareForMatch(
                playerUuid
            )
        } catch(t:Throwable) {
            val rolledBack=
                restoreIfPossible(
                    playerUuid
                )
            if(!rolledBack) {
                t.addSuppressed(
                    IllegalStateException(
                        "Immediate durable player-state rollback failed for $playerUuid; recovery journal remains pending"
                    )
                )
            }
            throw t
        }
    }

    override fun restoreIfPossible(
        playerUuid: UUID
    ): Boolean =
        restoreIfPossible(
            playerUuid
        ) { true }

    fun restoreIfPossible(
        playerUuid: UUID,
        verifyAfterRestore:
            (PlayerSnapshot)->Boolean
    ): Boolean {
        if(
            !adapter.isOnline(
                playerUuid
            )
        ) return false

        val snapshot=
            store.beginRestore(
                playerUuid
            ) ?: return false

        return try {
            adapter.restore(snapshot)
            val verified=
                verifyAfterRestore(
                    snapshot
                )
            if(!verified) {
                runCatching {
                    adapter.restore(
                        snapshot
                    )
                }
                store.markRestoreFailed(
                    playerUuid
                )
                false
            } else {
                // Durable state is authoritative across process boundaries.
                // Delete the on-disk journal before exposing RESTORED in memory;
                // if deletion fails, the catch path returns this record to
                // CAPTURED so the same authoritative snapshot remains pending
                // and can be retried safely.
                journal.delete(
                    playerUuid
                )
                store.markRestored(
                    playerUuid
                )
                true
            }
        } catch(_:Throwable) {
            runCatching {
                adapter.restore(snapshot)
            }
            store.markRestoreFailed(
                playerUuid
            )
            false
        }
    }

    override fun pending():
        Set<UUID> =
        store.unresolved()
}
