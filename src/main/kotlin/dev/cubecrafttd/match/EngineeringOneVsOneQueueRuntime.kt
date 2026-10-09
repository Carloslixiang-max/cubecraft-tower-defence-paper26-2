package dev.cubecrafttd.match

import java.util.UUID

data class EngineeringOneVsOneQueueJoin(
    val playerUuid: UUID,
    val added: Boolean,
    val position: Int,
    val waitingCount: Int
)

data class EngineeringOneVsOneQueuePair(
    val redPlayer: UUID,
    val bluePlayer: UUID
) {
    init {
        require(redPlayer!=bluePlayer)
    }

    val players: List<UUID>
        get() = listOf(
            redPlayer,
            bluePlayer
        )
}

/**
 * Engineering-only FIFO queue for the single configured Farm arena.
 *
 * This is an accessibility/runtime bridge, not a claim about original
 * CubeCraft matchmaking. FIFO order and first=RED/second=BLUE are explicit
 * Engineering behavior until original queue/team-assignment evidence is
 * recovered.
 */
class EngineeringOneVsOneQueueState {
    private val queue = EngineeringTeamQueueState(1)
    fun join(playerUuid: UUID) = queue.join(playerUuid)
    fun leave(playerUuid: UUID) = queue.leave(playerUuid)
    fun contains(playerUuid: UUID) = queue.contains(playerUuid)
    fun position(playerUuid: UUID) = queue.position(playerUuid)
    fun snapshot() = queue.snapshot()
    fun retainEligible(eligiblePlayers: Set<UUID>) = queue.retainEligible(eligiblePlayers)
    fun pairIfArenaAvailable(arenaAvailable: Boolean): EngineeringOneVsOneQueuePair? =
        queue.matchIfArenaAvailable(arenaAvailable)?.let { EngineeringOneVsOneQueuePair(it.players[0], it.players[1]) }
    fun restorePairToFront(pair: EngineeringOneVsOneQueuePair) =
        queue.restoreMatchToFront(EngineeringTeamQueueMatch(pair.players, 1))
    fun size() = queue.size()
}
