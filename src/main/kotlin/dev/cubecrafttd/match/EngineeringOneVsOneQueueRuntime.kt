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
    private val waiting=
        linkedSetOf<UUID>()

    fun join(
        playerUuid: UUID
    ): EngineeringOneVsOneQueueJoin {
        val added=
            waiting.add(
                playerUuid
            )
        val position=
            waiting.indexOf(
                playerUuid
            ) + 1

        return EngineeringOneVsOneQueueJoin(
            playerUuid=playerUuid,
            added=added,
            position=position,
            waitingCount=
                waiting.size
        )
    }

    fun leave(
        playerUuid: UUID
    ): Boolean =
        waiting.remove(
            playerUuid
        )

    fun contains(
        playerUuid: UUID
    ): Boolean =
        playerUuid in waiting

    fun position(
        playerUuid: UUID
    ): Int? {
        val index=
            waiting.indexOf(
                playerUuid
            )
        return if(index<0)
            null
        else
            index+1
    }

    fun snapshot():
        List<UUID> =
        waiting.toList()

    fun retainEligible(
        eligiblePlayers: Set<UUID>
    ): Set<UUID> {
        val removed=
            waiting.filterTo(
                linkedSetOf()
            ) {
                it !in eligiblePlayers
            }
        waiting.removeAll(
            removed
        )
        return removed
    }

    fun pairIfArenaAvailable(
        arenaAvailable: Boolean
    ): EngineeringOneVsOneQueuePair? {
        if(
            !arenaAvailable ||
            waiting.size<2
        ) return null

        val iterator=
            waiting.iterator()
        val red=
            iterator.next()
        iterator.remove()
        val blue=
            iterator.next()
        iterator.remove()

        return EngineeringOneVsOneQueuePair(
            red,blue
        )
    }

    fun restorePairToFront(
        pair:
            EngineeringOneVsOneQueuePair
    ) {
        val existing=
            waiting.toList()
        waiting.clear()

        pair.players.forEach {
            waiting += it
        }
        existing.forEach {
            waiting += it
        }
    }

    fun size(): Int =
        waiting.size

    private fun Iterable<UUID>.indexOf(
        target: UUID
    ): Int {
        var index=0
        for(value in this) {
            if(value==target) {
                return index
            }
            index++
        }
        return -1
    }
}
