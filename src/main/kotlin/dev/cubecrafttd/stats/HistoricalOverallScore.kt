package dev.cubecrafttd.stats

import java.util.UUID

/**
 * Historical CubeCraft Tower Defence end-of-match score.
 *
 * Direct official 2017 update evidence says the Top 3 is based on towers built,
 * mobs sent and mobs killed, using cumulative cost. The most faithful direct
 * projection is therefore the sum of those three already-recorded cumulative
 * cost buckets. Later community posts describing the score as generic Coins
 * spent are lower-confidence and do not override the official rule.
 */
object HistoricalOverallScoreCalculator {
    fun forPlayer(
        stats: MatchPlayerStats
    ): Long =
        Math.addExact(
            Math.addExact(
                stats.towersBuiltCumulativeCost,
                stats.troopsSentCumulativeCost
            ),
            stats.troopsKilledCumulativeCost
        )

    fun byPlayer(
        snapshot: MatchStatsSnapshot
    ): Map<UUID,Long> =
        snapshot.byPlayer
            .mapValues {
                (_,stats) ->
                forPlayer(stats)
            }
}
