package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.match.ArmageddonType
import dev.cubecrafttd.match.EngineeringOneVsOneQueuePair
import dev.cubecrafttd.match.EngineeringOneVsOneQueueState
import dev.cubecrafttd.match.HistoricalPregameArmageddonResolution
import dev.cubecrafttd.match.HistoricalPregameArmageddonVoteOption
import dev.cubecrafttd.match.HistoricalPregameArmageddonVoteRuntime
import dev.cubecrafttd.match.HistoricalTowerDefenceStartCountdown
import dev.cubecrafttd.recovery.JournaledPlayerRecoveryOrchestrator
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

data class BukkitQueueJoinReport(
    val added: Boolean,
    val position: Int,
    val waitingCount: Int,
    val blockingCode: String?
)

data class BukkitQueueLeaveReport(
    val removedFromWaiting: Boolean,
    val cancelledStartCountdown: Boolean,
    val leftActiveMatch: Boolean,
    val restoredNow: Boolean,
    val arenaCleaned: Boolean
)

data class BukkitPregameArmageddonVoteReport(
    val option:
        HistoricalPregameArmageddonVoteOption,
    val waitingPosition: Int?,
    val countdownRunning: Boolean
)

/**
 * Engineering player-facing bridge for the single configured Farm arena.
 *
 * FIFO order and first=RED/second=BLUE remain Engineering behavior. Once two
 * eligible players are paired, the service uses the recovered historical
 * 3 -> 2 -> 1 Tower Defence chat countdown before creating the live arena.
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

    private var failedStartCooldownTicks=
        0

    private var pendingPair:
        EngineeringOneVsOneQueuePair? =
        null

    private var pendingArenaId:
        String? =
        null

    private var pendingCountdown:
        HistoricalTowerDefenceStartCountdown? =
        null

    private val pregameArmageddonVotes=
        linkedMapOf<
            UUID,
            HistoricalPregameArmageddonVoteOption
        >()

    private val task: BukkitTask =
        plugin.server.scheduler
            .runTaskTimer(
                plugin,
                Runnable {
                    tick()
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
        check(
            playerUuid !in
                (
                    pendingPair
                        ?.players
                        ?: emptyList()
                )
        ) {
            "You are already matched and the TD start countdown is running"
        }

        val joined=
            queue.join(
                playerUuid
            )

        val blocker=
            nonLiveBlockers()
                .firstOrNull()
                ?.code

        return BukkitQueueJoinReport(
            added=joined.added,
            position=
                joined.position,
            waitingCount=
                queue.size(),
            blockingCode=
                blocker
        )
    }

    fun castArmageddonVote(
        playerUuid: UUID,
        option:
            HistoricalPregameArmageddonVoteOption
    ): BukkitPregameArmageddonVoteReport {
        val inWaiting=
            queue.contains(
                playerUuid
            )
        val inCountdown=
            playerUuid in
                (
                    pendingPair
                        ?.players
                        ?: emptyList()
                )
        check(inWaiting || inCountdown) {
            "Join the TD queue before voting"
        }

        val concrete=
            option.concreteTypeOrNull()
        check(
            concrete==null ||
                concrete in
                    controller
                        .runnableArmageddonTypes()
        ) {
            "That Armageddon mode is not runnable in the current server profile"
        }

        pregameArmageddonVotes[
            playerUuid
        ]=option

        return BukkitPregameArmageddonVoteReport(
            option=option,
            waitingPosition=
                queue.position(
                    playerUuid
                ),
            countdownRunning=
                inCountdown
        )
    }

    fun leave(
        playerUuid: UUID
    ): BukkitQueueLeaveReport {
        if(queue.leave(playerUuid)) {
            pregameArmageddonVotes
                .remove(playerUuid)
            return BukkitQueueLeaveReport(
                removedFromWaiting=true,
                cancelledStartCountdown=false,
                leftActiveMatch=false,
                restoredNow=false,
                arenaCleaned=false
            )
        }

        if(
            playerUuid in
                (
                    pendingPair
                        ?.players
                        ?: emptyList()
                )
        ) {
            pregameArmageddonVotes
                .remove(playerUuid)
            cancelPendingCountdown(
                dropPlayers=
                    setOf(playerUuid),
                reason=
                    "A matched player left before the game started."
            )
            return BukkitQueueLeaveReport(
                removedFromWaiting=false,
                cancelledStartCountdown=true,
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
                    cancelledStartCountdown=false,
                    leftActiveMatch=false,
                    restoredNow=false,
                    arenaCleaned=false
                )

        return BukkitQueueLeaveReport(
            removedFromWaiting=false,
            cancelledStartCountdown=false,
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

    fun queuedPlayerCount(): Int =
        queue.size() +
            (
                pendingPair
                    ?.players
                    ?.size
                    ?: 0
            )

    fun close() {
        task.cancel()
    }

    private fun tick() {
        pruneWaitingPlayers()

        val pair=
            pendingPair
        if(pair!=null) {
            val ineligible=
                pair.players
                    .filterTo(
                        linkedSetOf()
                    ) {
                        !eligibleForStart(
                            it
                        )
                    }

            if(ineligible.isNotEmpty()) {
                cancelPendingCountdown(
                    dropPlayers=
                        ineligible,
                    reason=
                        "TD start countdown cancelled because a matched player became unavailable."
                )
                return
            }

            if(
                controller.activeArenaIds()
                    .isNotEmpty()
            ) {
                cancelPendingCountdown(
                    emptySet(),
                    "TD start countdown cancelled because the configured arena became busy."
                )
                return
            }

            if(nonLiveBlockers().isNotEmpty()) {
                cancelPendingCountdown(
                    emptySet(),
                    "TD start countdown paused by server readiness; players returned to queue."
                )
                return
            }

            advancePendingCountdown()
            return
        }

        if(failedStartCooldownTicks>0) {
            failedStartCooldownTicks--
            return
        }

        prepareCountdownIfPossible()
    }

    private fun prepareCountdownIfPossible() {
        if(pendingPair!=null) {
            return
        }
        if(failedStartCooldownTicks>0) {
            return
        }
        if(
            controller.activeArenaIds()
                .isNotEmpty()
        ) return
        if(nonLiveBlockers().isNotEmpty()) {
            return
        }

        pruneWaitingPlayers()

        val pair=
            queue.pairIfArenaAvailable(
                true
            ) ?: return

        pendingPair=pair
        pendingArenaId=
            "queue-" +
                nextArenaSequence++
        pendingCountdown=
            HistoricalTowerDefenceStartCountdown()

        // Announce "3" immediately when the second eligible player is paired.
        advancePendingCountdown()
    }

    private fun advancePendingCountdown() {
        val pair=
            pendingPair
                ?: return
        val countdown=
            pendingCountdown
                ?: error(
                    "Pending pair has no start countdown"
                )
        val arenaId=
            pendingArenaId
                ?: error(
                    "Pending pair has no arena id"
                )

        val step=
            countdown.advance()

        val seconds=
            step.announceSeconds
        if(seconds!=null) {
            val unit=
                if(seconds==1)
                    "second"
                else
                    "seconds"
            pair.players.forEach {
                uuid ->
                plugin.server
                    .getPlayer(uuid)
                    ?.sendMessage(
                        "Tower Defence is starting in " +
                            seconds +
                            " " +
                            unit +
                            "."
                    )
            }
            return
        }

        check(step.startNow)

        clearPendingCountdown()
        startPair(
            pair,
            arenaId
        )
    }

    private fun startPair(
        pair:
            EngineeringOneVsOneQueuePair,
        arenaId: String
    ) {
        try {
            val runnable=
                controller
                    .runnableArmageddonTypes()
                    .sortedBy {
                        it.name
                    }
            check(runnable.isNotEmpty()) {
                "No runnable Armageddon mode is available"
            }

            val pregameVote=
                HistoricalPregameArmageddonVoteRuntime(
                    eligiblePlayers=
                        pair.players.toSet(),
                    runnableTypes=
                        runnable.toSet()
                )
            pair.players.forEach { uuid ->
                pregameArmageddonVotes[
                    uuid
                ]?.let { option ->
                    pregameVote.cast(
                        uuid,
                        option
                    )
                }
            }

            val randomChoice=
                runnable[
                    java.util.concurrent
                        .ThreadLocalRandom
                        .current()
                        .nextInt(
                            runnable.size
                        )
                ]
            val resolved=
                pregameVote.resolve(
                    randomChoice
                )

            controller
                .startOneVsOneResolvedTest(
                    arenaId,
                    pair.redPlayer,
                    pair.bluePlayer,
                    resolved.selection
                )

            pair.players.forEach {
                pregameArmageddonVotes
                    .remove(it)
            }

            val armageddonReason=
                when(resolved.resolution) {
                    HistoricalPregameArmageddonResolution
                        .NO_VOTES_RANDOM ->
                        "Due to no votes."
                    HistoricalPregameArmageddonResolution
                        .VOTED_RANDOM ->
                        "Random won the vote."
                    HistoricalPregameArmageddonResolution
                        .UNIQUE_HIGHEST_CONCRETE ->
                        "Due to votes."
                    HistoricalPregameArmageddonResolution
                        .ENGINEERING_TIE_RANDOM_FALLBACK ->
                        "Engineering tie fallback: Random."
                }

            pair.players.forEach {
                uuid ->
                plugin.server
                    .getPlayer(uuid)
                    ?.apply {
                        sendMessage(
                            "Selected " +
                                resolved.selection
                                    .type.name
                                    .lowercase()
                                    .replaceFirstChar {
                                        it.uppercase()
                                    } +
                                " armageddon mode! " +
                                armageddonReason
                        )
                        sendMessage(
                            "Selected Normal pricing! Due to no votes."
                        )
                        sendMessage(
                            "Selected Normal gamemode! Due to no votes."
                        )
                        sendMessage(
                            "TD Engineering 1v1 started: " +
                                arenaId
                        )
                    }
            }
        } catch(t:Throwable) {
            queue.restorePairToFront(
                pair
            )
            failedStartCooldownTicks=5

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
        }
    }

    private fun cancelPendingCountdown(
        dropPlayers: Set<UUID>,
        reason: String
    ) {
        val pair=
            pendingPair
                ?: return

        clearPendingCountdown()
        queue.restorePairToFront(
            pair
        )
        dropPlayers.forEach {
            queue.leave(it)
            pregameArmageddonVotes
                .remove(it)
        }
        pruneWaitingPlayers()

        pair.players
            .filterNot {
                it in dropPlayers
            }
            .forEach { uuid ->
                plugin.server
                    .getPlayer(uuid)
                    ?.sendMessage(reason)
            }
    }

    private fun clearPendingCountdown() {
        pendingPair=null
        pendingArenaId=null
        pendingCountdown=null
    }

    private fun pruneWaitingPlayers() {
        val eligible=
            queue.snapshot()
                .filterTo(
                    linkedSetOf()
                ) {
                    eligibleForStart(
                        it
                    )
                }
        val removed=
            queue.retainEligible(
                eligible
            )
        removed.forEach {
            pregameArmageddonVotes
                .remove(it)
        }
    }

    private fun eligibleForStart(
        playerUuid: UUID
    ): Boolean =
        plugin.server
            .getPlayer(playerUuid)
            ?.isOnline == true &&
        !controller
            .isActivePlayer(
                playerUuid
            ) &&
        playerUuid !in
            recovery.pending()

    private fun nonLiveBlockers():
        List<ReadinessIssue> =
        readiness.inspect()
            .blockers
            .filterNot {
                it.code==
                    "PAPER_LIVE_GATE_NOT_CERTIFIED"
            }
}
