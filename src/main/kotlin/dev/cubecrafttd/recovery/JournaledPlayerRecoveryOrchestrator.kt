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
            store.record(playerUuid)==null
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
        adapter.prepareForMatch(
            playerUuid
        )
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
                store.markRestored(
                    playerUuid
                )
                journal.delete(
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
