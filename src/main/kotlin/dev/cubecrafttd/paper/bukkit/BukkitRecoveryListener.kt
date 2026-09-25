package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.player.PlayerSnapshotComparator
import dev.cubecrafttd.player.PlayerSnapshotComparison
import dev.cubecrafttd.player.PlayerStateAdapter
import dev.cubecrafttd.recovery.JournaledPlayerRecoveryOrchestrator
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.Plugin
import java.util.UUID

class BukkitRecoveryListener(
    private val plugin: Plugin,
    private val recovery:
        JournaledPlayerRecoveryOrchestrator,
    private val stateAdapter:
        PlayerStateAdapter,
    private val restartEvidence:
        (UUID,PlayerSnapshotComparison?)->Unit
) : Listener {
    private val loadedFromPreviousProcess=
        recovery.recoverJournalIntoMemory()
            .toMutableSet()

    init {
        plugin.server.pluginManager
            .registerEvents(this,plugin)
    }

    fun pendingCount(): Int =
        recovery.pending().size

    fun pendingFromPreviousProcessCount():
        Int =
        loadedFromPreviousProcess
            .count {
                it in recovery.pending()
            }

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
        val uuid=
            event.player.uniqueId
        if(
            uuid in
                recovery.pending()
        ) {
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

                    val fromPreviousProcess=
                        uuid in
                            loadedFromPreviousProcess

                    val ok=
                        if(fromPreviousProcess) {
                            recovery
                                .restoreIfPossible(
                                    uuid
                                ) { expected ->
                                    val actual=
                                        runCatching {
                                            stateAdapter
                                                .capture(
                                                    uuid,
                                                    expected
                                                        .capturedAtArenaTick
                                                )
                                        }
                                    if(actual.isFailure) {
                                        restartEvidence(
                                            uuid,
                                            null
                                        )
                                        false
                                    } else {
                                        val comparison=
                                            PlayerSnapshotComparator
                                                .compare(
                                                    expected,
                                                    actual.getOrThrow()
                                                )
                                        restartEvidence(
                                            uuid,
                                            comparison
                                        )
                                        comparison.passed
                                    }
                                }
                        } else {
                            recovery
                                .restoreIfPossible(
                                    uuid
                                )
                        }

                    if(ok) {
                        loadedFromPreviousProcess
                            .remove(uuid)
                        plugin.logger.info(
                            "Restored pending CubeCraft TD snapshot for " +
                                uuid +
                                if(fromPreviousProcess)
                                    " with cross-process verification"
                                else ""
                        )
                    } else {
                        plugin.logger.warning(
                            "Pending snapshot for " +
                                uuid +
                                " was not restored/verified; durable journal remains."
                        )
                    }
                }
            )
    }
}
