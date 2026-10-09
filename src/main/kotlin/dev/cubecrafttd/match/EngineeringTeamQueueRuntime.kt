package dev.cubecrafttd.match

import java.util.UUID

/** Full-roster FIFO and alternating assignment are Engineering policy, not recovered matchmaking. */
class EngineeringTeamQueueMatch(players: List<UUID>, val teamSize: Int) {
    val players = players.toList()
    val redPlayers: Set<UUID>
    val bluePlayers: Set<UUID>

    init {
        require(teamSize in 1..TeamRosterPolicy.MAX_TEAM_PLAYERS)
        require(this.players.size == teamSize * 2 && this.players.distinct().size == this.players.size)
        redPlayers = this.players.filterIndexed { index, _ -> index % 2 == 0 }.toSet()
        bluePlayers = this.players.filterIndexed { index, _ -> index % 2 == 1 }.toSet()
        TeamRosterPolicy.validate(redPlayers, bluePlayers)
    }
}

class EngineeringTeamQueueState(val teamSize: Int) {
    init { require(teamSize in 1..TeamRosterPolicy.MAX_TEAM_PLAYERS) }
    val requiredPlayers: Int get() = teamSize * 2
    private val waiting = linkedSetOf<UUID>()

    fun join(playerUuid: UUID): EngineeringOneVsOneQueueJoin = EngineeringOneVsOneQueueJoin(
        playerUuid, waiting.add(playerUuid), waiting.indexOf(playerUuid) + 1, waiting.size)

    fun leave(playerUuid: UUID): Boolean = waiting.remove(playerUuid)
    fun contains(playerUuid: UUID): Boolean = playerUuid in waiting
    fun position(playerUuid: UUID): Int? = waiting.indexOf(playerUuid).takeIf { it >= 0 }?.plus(1)
    fun snapshot(): List<UUID> = waiting.toList()
    fun size(): Int = waiting.size

    fun retainEligible(eligiblePlayers: Set<UUID>): Set<UUID> =
        waiting.filterTo(linkedSetOf()) { it !in eligiblePlayers }.also { waiting.removeAll(it) }

    fun matchIfArenaAvailable(arenaAvailable: Boolean): EngineeringTeamQueueMatch? {
        if (!arenaAvailable || waiting.size < requiredPlayers) return null
        val match = EngineeringTeamQueueMatch(waiting.take(requiredPlayers), teamSize)
        waiting.removeAll(match.players.toSet())
        return match
    }

    fun restoreMatchToFront(match: EngineeringTeamQueueMatch) {
        require(match.teamSize == teamSize)
        val existing = waiting.toList()
        waiting.clear()
        waiting.addAll(match.players)
        waiting.addAll(existing)
    }
}
