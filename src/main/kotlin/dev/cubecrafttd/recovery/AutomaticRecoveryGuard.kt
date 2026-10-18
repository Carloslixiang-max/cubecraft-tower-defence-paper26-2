package dev.cubecrafttd.recovery

import java.util.UUID

/** A durable snapshot is also held throughout a healthy match, not just recovery. */
class AutomaticRecoveryGuard(
    private val activeMatchOwnsSnapshot: (UUID) -> Boolean
) {
    private val heldUntilNextProcess = linkedSetOf<UUID>()

    fun deferUntilNextProcess(player: UUID) {
        heldUntilNextProcess += player
    }

    /** Evaluate again when the scheduled callback runs; ownership can change. */
    fun permitsRestore(player: UUID): Boolean =
        player !in heldUntilNextProcess &&
            runCatching { !activeMatchOwnsSnapshot(player) }.getOrDefault(false)
}
