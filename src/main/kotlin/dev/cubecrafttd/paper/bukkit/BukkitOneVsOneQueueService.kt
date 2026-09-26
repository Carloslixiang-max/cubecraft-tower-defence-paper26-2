package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.match.ArmageddonType
import dev.cubecrafttd.match.EngineeringOneVsOneQueueState
import dev.cubecrafttd.recovery.JournaledPlayerRecoveryOrchestrator
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

data class BukkitQueueJoinReport(
    val added: Boolean,
    val position: Int?,
    val waitingCount: Int,
    val startedArenaId: String?,
    val blockingCode: String?
)

data class BukkitQueueLeaveReport(
    val removedFromWaiting: Boolean,
    val leftActiveMatch: Boolean,
    val restoredNow: Boolean,
    val arenaCleaned: Boolean
)

/**
 * Engineering player-facing bridge for the single configured Farm arena.
 *
 * It intentionally does not claim to reproduce CubeCraft matchmaking. One
 * physical map binding currently means one live arena at a time, so further
 * pairs remain in FIFO order until the arena becomes free.
 */
class BukkitOneVsOneQueueService(
    private val plugin: JavaPlugin,
    private val controller:
        BukkitNormalArenaController,
    private val readiness:
        PaperGameplayReadinessService,
    private val recovery:
        JournaledPlayerRecoveryOrchestrator
) {
    private val queue=
        EngineeringOneVsOneQueueState()

    private var nextArenaSequence=
        1L

    private var failedStartCooldownPumps=
        0

    private val task: BukkitTask =
        plugin.server.scheduler
            .runTaskTimer(
                plugin,
                Runnable {
                    pump()
                },
                20L,
                20L
            )

    fun join(
        playerUuid: UUID
    ): BukkitQueueJoinReport {
        check(
            !controller.isActivePlayer(
                playerUuid
            )
        ) {
            "You are already in an active TD match"
        }
        check(
            playerUuid !in
                recovery.pending()
        ) {
            "Your previous TD player state is still pending restore"
        }
        check(
            plugin.server
                .getPlayer(playerUuid)
                ?.isOnline == true
        ) {
            "Player must be online"
        }

        val joined=
            queue.join(
                playerUuid
            )
        val started=
            pump()
        val blocker=
            nonLiveBlockers()
                .firstOrNull()
                ?.code

        return BukkitQueueJoinReport(
            added=joined.added,
            position=
                queue.position(
                    playerUuid
                ),
            waitingCount=
                queue.size(),
            startedArenaId=
                started,
            blockingCode=
                blocker
        )
    }

    fun leave(
        playerUuid: UUID
    ): BukkitQueueLeaveReport {
        if(queue.leave(playerUuid)) {
            return BukkitQueueLeaveReport(
                removedFromWaiting=true,
                leftActiveMatch=false,
                restoredNow=false,
                arenaCleaned=false
            )
        }

        val active=
            controller
                .leaveActivePlayer(
                    playerUuid
                )
                ?: return BukkitQueueLeaveReport(
                    removedFromWaiting=false,
                    leftActiveMatch=false,
                    restoredNow=false,
                    arenaCleaned=false
                )

        return BukkitQueueLeaveReport(
            removedFromWaiting=false,
            leftActiveMatch=true,
            restoredNow=
                active.restoredNow,
            arenaCleaned=
                active.arenaCleaned
        )
    }

    fun position(
        playerUuid: UUID
    ): Int? =
        queue.position(
            playerUuid
        )

    fun waitingPlayers():
        List<UUID> =
        queue.snapshot()

    fun close() {
        task.cancel()
    }

    private fun pump():
        String? {
        val eligible=
            queue.snapshot()
                .filterTo(
                    linkedSetOf()
                ) { uuid ->
                    plugin.server
                        .getPlayer(uuid)
                        ?.isOnline == true &&
                    !controller
                        .isActivePlayer(uuid) &&
                    uuid !in
                        recovery.pending()
                }
        queue.retainEligible(
            eligible
        )

        if(failedStartCooldownPumps>0) {
            failedStartCooldownPumps--
            return null
        }

        if(
            controller.activeArenaIds()
                .isNotEmpty()
        ) return null

        if(nonLiveBlockers().isNotEmpty()) {
            return null
        }

        val pair=
            queue.pairIfArenaAvailable(
                true
            ) ?: return null

        val arenaId=
            "queue-" +
                nextArenaSequence++

        return try {
            controller
                .startOneVsOneTest(
                    arenaId,
                    pair.redPlayer,
                    pair.bluePlayer,
                    ArmageddonType.WITHER
                )

            pair.players.forEach {
                uuid ->
                plugin.server
                    .getPlayer(uuid)
                    ?.sendMessage(
                        "TD Engineering 1v1 started: " +
                            arenaId +
                            ". WITHER is only the Engineering fallback; " +
                            "use Settings -> Armageddon vote to change it."
                    )
            }
            arenaId
        } catch(t:Throwable) {
            queue.restorePairToFront(
                pair
            )
            failedStartCooldownPumps=5

            pair.players.forEach {
                uuid ->
                plugin.server
                    .getPlayer(uuid)
                    ?.sendMessage(
                        "TD queue start delayed: " +
                            (
                                t.message
                                    ?: t.javaClass
                                        .simpleName
                            )
                    )
            }
            plugin.logger.warning(
                "Engineering 1v1 queue start failed for " +
                    arenaId +
                    ": " +
                    t.stackTraceToString()
            )
            null
        }
    }

    private fun nonLiveBlockers():
        List<ReadinessIssue> =
        readiness.inspect()
            .blockers
            .filterNot {
                it.code==
                    "PAPER_LIVE_GATE_NOT_CERTIFIED"
            }
}
