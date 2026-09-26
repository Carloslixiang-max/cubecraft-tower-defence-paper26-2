package dev.cubecrafttd.match

import java.util.UUID

enum class EngineeringArmageddonVoteResolution {
    UNIQUE_HIGHEST_VOTE,
    DEFAULT_NO_VOTES,
    DEFAULT_TIE
}

data class EngineeringArmageddonVoteSnapshot(
    val eligiblePlayers: Set<UUID>,
    val allowedTypes: Set<ArmageddonType>,
    val defaultType: ArmageddonType,
    val votes: Map<UUID,ArmageddonType>,
    val selection: ResolvedArmageddonSelection,
    val resolution: EngineeringArmageddonVoteResolution
) {
    fun count(type: ArmageddonType): Int =
        votes.values.count { it==type }
}

data class EngineeringArmageddonVoteReceipt(
    val playerUuid: UUID,
    val previousVote: ArmageddonType?,
    val newVote: ArmageddonType,
    val snapshot: EngineeringArmageddonVoteSnapshot
)

/**
 * Engineering-only player vote bridge.
 *
 * Recovered material confirms the original menu offered Random / Wither /
 * Lightning / Horde, but the exact original winner, tie and Random-resolution
 * semantics are not recovered with enough confidence. This runtime therefore
 * accepts only concrete runnable Armageddon types. A unique highest vote wins;
 * no-vote and tied states fall back to the operator-selected Engineering
 * default. These rules are never promoted to original truth.
 */
class EngineeringArmageddonVoteRuntime(
    eligiblePlayers: Set<UUID>,
    allowedTypes: Set<ArmageddonType>,
    private val defaultType: ArmageddonType
) {
    private val eligible=eligiblePlayers.toSet()
    private val allowed=allowedTypes.toSet()
    private val votes=linkedMapOf<UUID,ArmageddonType>()

    init {
        require(eligible.isNotEmpty())
        require(allowed.isNotEmpty())
        require(defaultType in allowed)
    }

    fun cast(
        playerUuid: UUID,
        type: ArmageddonType
    ): EngineeringArmageddonVoteReceipt {
        require(playerUuid in eligible) {
            "Player is not eligible to vote in this arena"
        }
        require(type in allowed) {
            "Armageddon $type is not runnable in this arena"
        }
        val previous=votes.put(playerUuid,type)
        return EngineeringArmageddonVoteReceipt(
            playerUuid,previous,type,snapshot()
        )
    }

    fun withdraw(
        playerUuid: UUID
    ): EngineeringArmageddonVoteSnapshot {
        votes.remove(playerUuid)
        return snapshot()
    }

    fun snapshot(): EngineeringArmageddonVoteSnapshot {
        val resolution=resolve()
        return EngineeringArmageddonVoteSnapshot(
            eligiblePlayers=eligible,
            allowedTypes=allowed,
            defaultType=defaultType,
            votes=LinkedHashMap(votes),
            selection=resolution.first,
            resolution=resolution.second
        )
    }

    private fun resolve():
        Pair<ResolvedArmageddonSelection,EngineeringArmageddonVoteResolution> {
        if(votes.isEmpty()) {
            return ResolvedArmageddonSelection(
                defaultType,
                ArmageddonSelectionSource.ENGINEERING_VOTE_FALLBACK
            ) to EngineeringArmageddonVoteResolution.DEFAULT_NO_VOTES
        }
        val counts=allowed.associateWith { type ->
            votes.values.count { it==type }
        }
        val highest=counts.values.maxOrNull() ?: 0
        val leaders=counts.filterValues { it==highest }.keys
        if(highest>0 && leaders.size==1) {
            return ResolvedArmageddonSelection(
                leaders.single(),
                ArmageddonSelectionSource.ENGINEERING_PLAYER_VOTE_RESULT
            ) to EngineeringArmageddonVoteResolution.UNIQUE_HIGHEST_VOTE
        }
        return ResolvedArmageddonSelection(
            defaultType,
            ArmageddonSelectionSource.ENGINEERING_VOTE_FALLBACK
        ) to EngineeringArmageddonVoteResolution.DEFAULT_TIE
    }
}
