package dev.cubecrafttd.ui

import dev.cubecrafttd.stats.MatchStatsSnapshot
import java.util.UUID

enum class HistoricalTopPlayersTiePolicyEvidence {
    /**
     * Exact historical equal-score ordering is not recovered.
     * UUID ascending exists only to keep tests/runtime deterministic.
     */
    ENGINEERING_UUID_ASCENDING
}

data class HistoricalTopPlayerEntry(
    val rank: Int,
    val playerUuid: UUID,
    val towersPlaced: Int,
    val troopsSent: Int,
    val enemiesKilled: Int,
    val overallScore: Long
)

data class HistoricalTopPlayersProjection(
    val heading: String,
    val entries: List<HistoricalTopPlayerEntry>,
    val tiePolicyEvidence:
        HistoricalTopPlayersTiePolicyEvidence
)

object HistoricalMatchEndLeaderboardProjector {
    fun project(
        stats: MatchStatsSnapshot
    ): HistoricalTopPlayersProjection {
        val ordered=
            stats.byPlayer
                .entries
                .sortedWith(
                    compareByDescending<
                        Map.Entry<
                            UUID,
                            dev.cubecrafttd.stats.MatchPlayerStats
                        >
                    > {
                        it.value.overallScore
                    }.thenBy {
                        it.key.toString()
                    }
                )
                .take(3)
                .mapIndexed {
                    index,(uuid,player) ->
                    HistoricalTopPlayerEntry(
                        rank=index+1,
                        playerUuid=uuid,
                        towersPlaced=
                            player.towersBuilt,
                        troopsSent=
                            player.troopsSent,
                        enemiesKilled=
                            player.troopsKilled,
                        overallScore=
                            player.overallScore
                    )
                }

        return HistoricalTopPlayersProjection(
            heading="Top players:",
            entries=ordered,
            tiePolicyEvidence=
                HistoricalTopPlayersTiePolicyEvidence
                    .ENGINEERING_UUID_ASCENDING
        )
    }
}
