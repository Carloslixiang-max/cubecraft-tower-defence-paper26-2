package dev.cubecrafttd.match

import java.util.UUID

/** Six-player team capacity; minimum start population/matchmaking remain unrecovered. */
object TeamRosterPolicy {
    const val MAX_TEAM_PLAYERS = 6

    fun names(raw: String): List<String> {
        val names = raw.split(',').map { it.trim() }
        require(names.size in 1..MAX_TEAM_PLAYERS && names.all { it.isNotEmpty() }) {
            "Team must contain 1 to $MAX_TEAM_PLAYERS comma-separated names"
        }
        require(names.map { it.lowercase(java.util.Locale.ROOT) }.distinct().size == names.size) {
            "Duplicate team player name"
        }
        return names
    }

    fun validate(red: Set<UUID>, blue: Set<UUID>) {
        require(red.size in 1..MAX_TEAM_PLAYERS && blue.size in 1..MAX_TEAM_PLAYERS)
        require(red.intersect(blue).isEmpty()) { "A player cannot be on both teams" }
    }
}
