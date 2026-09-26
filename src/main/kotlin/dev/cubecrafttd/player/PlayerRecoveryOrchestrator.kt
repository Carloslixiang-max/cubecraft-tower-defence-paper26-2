package dev.cubecrafttd.player

import java.util.UUID

interface PlayerRecoveryCoordinator {
    fun captureBeforeMatch(
        playerUuid: UUID,
        arenaTick: Long
    )

    fun restoreIfPossible(
        playerUuid: UUID
    ): Boolean

    fun pending(): Set<UUID>
}

class PlayerRecoveryOrchestrator(
    private val adapter: PlayerStateAdapter,
    private val store: PlayerSnapshotStore
) : PlayerRecoveryCoordinator {
    override fun captureBeforeMatch(
        playerUuid: UUID,
        arenaTick: Long
    ) {
        check(
            store.canCapture(
                playerUuid
            )
        ) {
            "Player already has active snapshot: $playerUuid"
        }
        val snapshot=
            adapter.capture(
                playerUuid,arenaTick
            )
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
                        "Immediate player-state rollback failed for $playerUuid; snapshot remains pending"
                    )
                )
            }
            throw t
        }
    }

    override fun restoreIfPossible(
        playerUuid: UUID
    ): Boolean {
        if (
            !adapter.isOnline(playerUuid)
        ) return false
        val snapshot =
            store.beginRestore(playerUuid)
                ?: return false
        return try {
            adapter.restore(snapshot)
            store.markRestored(playerUuid)
            true
        } catch (t: Throwable) {
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
