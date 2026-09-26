package dev.cubecrafttd.match

import java.util.UUID

enum class HistoricalPregameArmageddonVoteOption {
    RANDOM,
    WITHER,
    LIGHTNING,
    HORDE;

    fun concreteTypeOrNull():
        ArmageddonType? =
        when(this) {
            RANDOM -> null
            WITHER -> ArmageddonType.WITHER
            LIGHTNING -> ArmageddonType.LIGHTNING
            HORDE -> ArmageddonType.HORDE
        }
}

enum class HistoricalPregameArmageddonResolution {
    NO_VOTES_RANDOM,
    VOTED_RANDOM,
    UNIQUE_HIGHEST_CONCRETE,
    ENGINEERING_TIE_RANDOM_FALLBACK
}

data class HistoricalPregameArmageddonVoteSnapshot(
    val eligiblePlayers: Set<UUID>,
    val runnableTypes: Set<ArmageddonType>,
    val votes:
        Map<UUID,HistoricalPregameArmageddonVoteOption>
) {
    fun count(
        option:
            HistoricalPregameArmageddonVoteOption
    ): Int =
        votes.values.count {
            it==option
        }
}

data class HistoricalPregameArmageddonResolved(
    val selection:
        ResolvedArmageddonSelection,
    val resolution:
        HistoricalPregameArmageddonResolution,
    val winningOption:
        HistoricalPregameArmageddonVoteOption
)

/**
 * Pregame Armageddon voting recovered from Mature-era Tower Defence evidence.
 *
 * Confirmed:
 * - options: Random / Wither / Lightning / Horde;
 * - no votes resolves through Random;
 * - Random means a concrete Armageddon is chosen randomly.
 *
 * Exact original tie handling has not been recovered. A tied vote therefore
 * falls back to Random and is explicitly marked ENGINEERING_TIE_RANDOM_FALLBACK.
 */
class HistoricalPregameArmageddonVoteRuntime(
    eligiblePlayers: Set<UUID>,
    runnableTypes: Set<ArmageddonType>
) {
    private val eligible=
        eligiblePlayers.toSet()
    private val runnable=
        runnableTypes.toSet()
    private val votes=
        linkedMapOf<
            UUID,
            HistoricalPregameArmageddonVoteOption
        >()

    init {
        require(eligible.isNotEmpty())
        require(runnable.isNotEmpty())
    }

    fun cast(
        playerUuid: UUID,
        option:
            HistoricalPregameArmageddonVoteOption
    ): HistoricalPregameArmageddonVoteSnapshot {
        require(playerUuid in eligible) {
            "Player is not eligible for this pregame vote"
        }

        val concrete=
            option.concreteTypeOrNull()
        require(
            concrete==null ||
                concrete in runnable
        ) {
            "Armageddon $option is not runnable in this server profile"
        }

        votes[playerUuid]=option
        return snapshot()
    }

    fun withdraw(
        playerUuid: UUID
    ): HistoricalPregameArmageddonVoteSnapshot {
        votes.remove(playerUuid)
        return snapshot()
    }

    fun snapshot():
        HistoricalPregameArmageddonVoteSnapshot =
        HistoricalPregameArmageddonVoteSnapshot(
            eligiblePlayers=eligible,
            runnableTypes=runnable,
            votes=LinkedHashMap(votes)
        )

    fun resolve(
        randomChoice: ArmageddonType
    ): HistoricalPregameArmageddonResolved {
        require(randomChoice in runnable) {
            "Random Armageddon choice must be runnable"
        }

        if(votes.isEmpty()) {
            return HistoricalPregameArmageddonResolved(
                selection=
                    ResolvedArmageddonSelection(
                        randomChoice,
                        ArmageddonSelectionSource
                            .RANDOM_RESULT_ALREADY_RESOLVED
                    ),
                resolution=
                    HistoricalPregameArmageddonResolution
                        .NO_VOTES_RANDOM,
                winningOption=
                    HistoricalPregameArmageddonVoteOption
                        .RANDOM
            )
        }

        val counts=
            HistoricalPregameArmageddonVoteOption
                .entries
                .associateWith { option ->
                    votes.values.count {
                        it==option
                    }
                }
        val highest=
            counts.values.maxOrNull()
                ?: 0
        val leaders=
            counts.filterValues {
                it==highest
            }.keys

        if(highest>0 && leaders.size==1) {
            val winner=
                leaders.single()
            val concrete=
                winner.concreteTypeOrNull()
            return if(concrete==null) {
                HistoricalPregameArmageddonResolved(
                    ResolvedArmageddonSelection(
                        randomChoice,
                        ArmageddonSelectionSource
                            .RANDOM_RESULT_ALREADY_RESOLVED
                    ),
                    HistoricalPregameArmageddonResolution
                        .VOTED_RANDOM,
                    winner
                )
            } else {
                HistoricalPregameArmageddonResolved(
                    ResolvedArmageddonSelection(
                        concrete,
                        ArmageddonSelectionSource
                            .EXPLICIT_VOTE_RESULT
                    ),
                    HistoricalPregameArmageddonResolution
                        .UNIQUE_HIGHEST_CONCRETE,
                    winner
                )
            }
        }

        return HistoricalPregameArmageddonResolved(
            ResolvedArmageddonSelection(
                randomChoice,
                ArmageddonSelectionSource
                    .ENGINEERING_VOTE_FALLBACK
            ),
            HistoricalPregameArmageddonResolution
                .ENGINEERING_TIE_RANDOM_FALLBACK,
            HistoricalPregameArmageddonVoteOption
                .RANDOM
        )
    }
}
