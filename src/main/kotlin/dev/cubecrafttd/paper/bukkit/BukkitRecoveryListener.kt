package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.recovery.JournaledPlayerRecoveryOrchestrator
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.Plugin
import java.util.UUID

/**
 * One recovery authority for both:
 * - snapshots captured during the current process, and
 * - snapshots loaded from the durable journal after restart.
 */
class BukkitRecoveryListener(
    private val plugin: Plugin,
    private val recovery:
        JournaledPlayerRecoveryOrchestrator
) : Listener {
    init {
        recovery.recoverJournalIntoMemory()
        plugin.server.pluginManager
            .registerEvents(this,plugin)
    }

    fun pendingCount(): Int =
        recovery.pending().size

    fun recoverAlreadyOnline() {
        recovery.pending()
            .toList()
            .forEach { uuid ->
                if(
                    plugin.server
                        .getPlayer(uuid)
                        ?.isOnline == true
                ) {
                    scheduleRestore(uuid)
                }
            }
    }

    @EventHandler
    fun onJoin(
        event: PlayerJoinEvent
    ) {
        val uuid=event.player.uniqueId
        if(uuid in recovery.pending()) {
            scheduleRestore(uuid)
        }
    }

    private fun scheduleRestore(
        uuid: UUID
    ) {
        plugin.server.scheduler
            .runTask(
                plugin,
                Runnable {
                    if(
                        uuid !in
                            recovery.pending()
                    ) {
                        return@Runnable
                    }

                    val ok=
                        recovery
                            .restoreIfPossible(uuid)
                    if(ok) {
                        plugin.logger.info(
                            "Restored pending CubeCraft TD snapshot for $uuid"
                        )
                    } else {
                        plugin.logger.warning(
                            "Pending snapshot for $uuid was not restored; durable journal remains."
                        )
                    }
                }
            )
    }
}
