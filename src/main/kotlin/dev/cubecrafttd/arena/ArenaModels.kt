package dev.cubecrafttd.arena

import java.util.UUID

@JvmInline
value class ArenaId(val value: String)

enum class ArenaState {
    CREATED,
    PREPARED,
    RUNNING,
    ENDING,
    CLOSED
}

enum class TeamId { RED, BLUE }

data class TeamRuntime(
    val team: TeamId,
    val players: MutableSet<UUID> = linkedSetOf()
)
