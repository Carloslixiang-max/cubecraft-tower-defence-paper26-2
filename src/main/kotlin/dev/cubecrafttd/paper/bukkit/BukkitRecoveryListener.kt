package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.player.PlayerSnapshotComparator
import dev.cubecrafttd.player.PlayerSnapshotComparison
import dev.cubecrafttd.player.PlayerStateAdapter
import dev.cubecrafttd.recovery.JournaledPlayerRecoveryOrchestrator
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

class BukkitRecoveryListener(
    private val plugin: Plugin,
    private val recovery:
        JournaledPlayerRecoveryOrchestrator,
    private val stateAdapter:
        PlayerStateAdapter,
    private val restartEvidence:
        (UUID,PlayerSnapshotComparison?)->Unit,
    private val recoverySuccessEvidence:
        (UUID)->Unit = {}
) : Listener {
    companion object {
        private const val
            MAX_AUTO_RESTORE_ATTEMPTS=3
        private const val
            RETRY_DELAY_TICKS=20L
        private const val
            SWEEP_PERIOD_TICKS=20L
    }

    private val loadedFromPreviousProcess=
        recovery.recoverJournalIntoMemory()
            .toMutableSet()

    private val scheduledRestores=
        linkedSetOf<UUID>()

    private val retryAttempts=
        linkedMapOf<UUID,Int>()

    private val exhaustedAutoRetries=
        linkedSetOf<UUID>()

    private val sweepTask:
        BukkitTask

    init {
        plugin.server.pluginManager
            .registerEvents(this,plugin)

        sweepTask=
            plugin.server.scheduler
                .runTaskTimer(
                    plugin,
                    Runnable {
                        sweepPendingOnline()
                    },
                    SWEEP_PERIOD_TICKS,
                    SWEEP_PERIOD_TICKS
                )
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
                    scheduleRestore(
                        uuid
                    )
                }
            }
    }

    fun close() {
        sweepTask.cancel()
        scheduledRestores.clear()
        retryAttempts.clear()
        exhaustedAutoRetries.clear()
    }

    @EventHandler
    fun onJoin(
        event: PlayerJoinEvent
    ) {
        val uuid=
            event.player.uniqueId

        retryAttempts.remove(uuid)
        exhaustedAutoRetries
            .remove(uuid)

        if(
            uuid in
                recovery.pending()
        ) {
            scheduleRestore(uuid)
        }
    }

    private fun sweepPendingOnline() {
        recovery.pending()
            .toList()
            .forEach { uuid ->
                if(
                    uuid !in
                        exhaustedAutoRetries &&
                    plugin.server
                        .getPlayer(uuid)
                        ?.isOnline == true
                ) {
                    scheduleRestore(
                        uuid
                    )
                }
            }
    }

    private fun scheduleRestore(
        uuid: UUID,
        delayTicks: Long = 1L
    ) {
        if(
            uuid !in
                recovery.pending() ||
            !scheduledRestores.add(uuid)
        ) {
            return
        }

        plugin.server.scheduler
            .runTaskLater(
                plugin,
                Runnable {
                    scheduledRestores
                        .remove(uuid)

                    if(
                        uuid !in
                            recovery.pending()
                    ) {
                        retryAttempts
                            .remove(uuid)
                        exhaustedAutoRetries
                            .remove(uuid)
                        return@Runnable
                    }

                    if(
                        plugin.server
                            .getPlayer(uuid)
                            ?.isOnline != true
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
                        retryAttempts
                            .remove(uuid)
                        exhaustedAutoRetries
                            .remove(uuid)
                        runCatching {
                            recoverySuccessEvidence(
                                uuid
                            )
                        }.onFailure {
                            plugin.logger.warning(
                                "Could not persist TD recovery-success live evidence for " +
                                    uuid +
                                    ": " +
                                    it.javaClass.simpleName +
                                    ": " +
                                    it.message
                            )
                        }
                        plugin.logger.info(
                            "Restored pending CubeCraft TD snapshot for " +
                                uuid +
                                if(fromPreviousProcess)
                                    " with cross-process verification"
                                else ""
                        )
                    } else {
                        val attempt=
                            (
                                retryAttempts[
                                    uuid
                                ] ?: 0
                            ) + 1
                        retryAttempts[uuid]=
                            attempt

                        if(
                            attempt <
                                MAX_AUTO_RESTORE_ATTEMPTS &&
                            uuid in
                                recovery.pending() &&
                            plugin.server
                                .getPlayer(uuid)
                                ?.isOnline == true
                        ) {
                            plugin.logger.warning(
                                "Pending snapshot for " +
                                    uuid +
                                    " was not restored/verified; durable journal remains. " +
                                    "Automatic retry " +
                                    (attempt+1) +
                                    "/" +
                                    MAX_AUTO_RESTORE_ATTEMPTS +
                                    " is scheduled."
                            )
                            scheduleRestore(
                                uuid,
                                RETRY_DELAY_TICKS
                            )
                        } else {
                            exhaustedAutoRetries +=
                                uuid
                            plugin.logger.warning(
                                "Pending snapshot for " +
                                    uuid +
                                    " remains unresolved after " +
                                    attempt +
                                    " online restore attempt(s). Durable journal remains; reconnect to reset the automatic retry budget."
                            )
                        }
                    }
                },
                delayTicks
            )
    }
}
