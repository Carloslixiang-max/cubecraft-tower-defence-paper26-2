package dev.cubecrafttd.match

import dev.cubecrafttd.economy.PricingMode
import java.util.UUID

enum class HistoricalPregamePricingVoteOption {
    NORMAL,
    DOUBLE_INCOME,
    QUICK_START;

    fun pricingMode(): PricingMode =
        when(this) {
            NORMAL ->
                PricingMode.NORMAL
            DOUBLE_INCOME ->
                PricingMode.DOUBLE_INCOME
            QUICK_START ->
                PricingMode.QUICK_START
        }
}

enum class HistoricalPregamePricingResolution {
    NO_VOTES_NORMAL,
    UNIQUE_HIGHEST,
    ENGINEERING_TIE_NORMAL_FALLBACK
}

data class HistoricalPregamePricingVoteSnapshot(
    val eligiblePlayers: Set<UUID>,
    val votes:
        Map<UUID,HistoricalPregamePricingVoteOption>
) {
    fun count(
        option:
            HistoricalPregamePricingVoteOption
    ): Int =
        votes.values.count {
            it==option
        }
}

data class HistoricalPregamePricingResolved(
    val pricingMode: PricingMode,
    val resolution:
        HistoricalPregamePricingResolution,
    val winningOption:
        HistoricalPregamePricingVoteOption
)

/**
 * Historical Tower Defence pricing vote.
 *
 * Confirmed options are Normal / Double Income / Quick Start. A direct client
 * log confirms no votes -> Normal pricing. The exact original tied-vote rule
 * remains unresolved, so tied highest votes use an explicitly Engineering
 * Normal fallback.
 */
class HistoricalPregamePricingVoteRuntime(
    eligiblePlayers: Set<UUID>
) {
    private val eligible=
        eligiblePlayers.toSet()
    private val votes=
        linkedMapOf<
            UUID,
            HistoricalPregamePricingVoteOption
        >()

    init {
        require(eligible.isNotEmpty())
    }

    fun cast(
        playerUuid: UUID,
        option:
            HistoricalPregamePricingVoteOption
    ): HistoricalPregamePricingVoteSnapshot {
        require(playerUuid in eligible) {
            "Player is not eligible for this pregame pricing vote"
        }
        votes[playerUuid]=option
        return snapshot()
    }

    fun withdraw(
        playerUuid: UUID
    ): HistoricalPregamePricingVoteSnapshot {
        votes.remove(playerUuid)
        return snapshot()
    }

    fun snapshot():
        HistoricalPregamePricingVoteSnapshot =
        HistoricalPregamePricingVoteSnapshot(
            eligiblePlayers=eligible,
            votes=LinkedHashMap(votes)
        )

    fun resolve():
        HistoricalPregamePricingResolved {
        if(votes.isEmpty()) {
            return HistoricalPregamePricingResolved(
                pricingMode=
                    PricingMode.NORMAL,
                resolution=
                    HistoricalPregamePricingResolution
                        .NO_VOTES_NORMAL,
                winningOption=
                    HistoricalPregamePricingVoteOption
                        .NORMAL
            )
        }

        val counts=
            HistoricalPregamePricingVoteOption
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
            return HistoricalPregamePricingResolved(
                pricingMode=
                    winner.pricingMode(),
                resolution=
                    HistoricalPregamePricingResolution
                        .UNIQUE_HIGHEST,
                winningOption=winner
            )
        }

        return HistoricalPregamePricingResolved(
            pricingMode=
                PricingMode.NORMAL,
            resolution=
                HistoricalPregamePricingResolution
                    .ENGINEERING_TIE_NORMAL_FALLBACK,
            winningOption=
                HistoricalPregamePricingVoteOption
                    .NORMAL
        )
    }
}
