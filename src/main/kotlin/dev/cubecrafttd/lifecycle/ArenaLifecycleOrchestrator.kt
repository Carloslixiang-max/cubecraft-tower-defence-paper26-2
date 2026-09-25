package dev.cubecrafttd.lifecycle

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.player.PlayerRecoveryOrchestrator
import java.util.UUID

class ArenaLifecycleOrchestrator(
    private val arenaService: ArenaService,
    private val playerRecovery: PlayerRecoveryOrchestrator
) {
    fun prepare(context: ArenaContext, participants: Collection<UUID>) {
        check(context.state == ArenaState.CREATED)
        participants.forEach { uuid ->
            playerRecovery.captureBeforeMatch(uuid, context.gameTick)
        }
        context.state = ArenaState.PREPARED
    }

    fun start(context: ArenaContext) {
        check(context.state == ArenaState.PREPARED)
        context.state = ArenaState.RUNNING
    }

    fun beginEnding(context: ArenaContext) {
        if (context.state == ArenaState.CLOSED || context.state == ArenaState.ENDING) return
        check(context.state == ArenaState.RUNNING || context.state == ArenaState.PREPARED)
        context.state = ArenaState.ENDING
    }

    fun restoreOnlineParticipants(context: ArenaContext) {
        (context.redTeam.players + context.blueTeam.players).forEach(playerRecovery::restoreIfPossible)
    }

    fun close(context: ArenaContext, paperTeardown: (ArenaContext) -> Unit) {
        beginEnding(context)
        restoreOnlineParticipants(context)
        arenaService.close(context.arenaId, paperTeardown)
    }
}
