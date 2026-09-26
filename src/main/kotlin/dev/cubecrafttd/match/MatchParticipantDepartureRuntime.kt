package dev.cubecrafttd.match

import dev.cubecrafttd.arena.ArenaContext
import dev.cubecrafttd.arena.TeamId
import java.util.UUID

data class MatchParticipantDepartureReport(
    val playerUuid: UUID,
    val team: TeamId,
    val newlyDeparted: Boolean,
    val removedFromActiveSession: Boolean,
    val activePlayersRemaining: Int
)

/**
 * Keeps historical team membership in ArenaContext intact for tower ownership,
 * stats and final recovery, while removing a player who left from the active
 * live MatchSessionState.
 *
 * That distinction matters because historical Tower Defence matches could
 * continue after teammates left, and the old game did not provide a normal
 * rejoin path. The player's durable pre-match snapshot remains owned by the
 * recovery layer and is restored on reconnect.
 */
class MatchParticipantDepartureService(
    private val context: ArenaContext,
    private val session: MatchSessionState,
    private val departedPlayers:
        MutableSet<UUID>
) {
    fun depart(
        playerUuid: UUID
    ): MatchParticipantDepartureReport? {
        val team=when {
            playerUuid in
                context.redTeam.players ->
                TeamId.RED
            playerUuid in
                context.blueTeam.players ->
                TeamId.BLUE
            else -> return null
        }

        val newlyDeparted=
            departedPlayers.add(
                playerUuid
            )
        val removed=
            session.players.remove(
                playerUuid
            ) != null

        return MatchParticipantDepartureReport(
            playerUuid=playerUuid,
            team=team,
            newlyDeparted=newlyDeparted,
            removedFromActiveSession=removed,
            activePlayersRemaining=
                session.players.size
        )
    }
}
