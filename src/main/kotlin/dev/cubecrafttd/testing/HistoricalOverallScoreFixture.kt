package dev.cubecrafttd.testing

import dev.cubecrafttd.stats.*
import java.util.UUID

object HistoricalOverallScoreFixture {
    fun run(): List<FixtureResult> {
        val p1=UUID.fromString(
            "00000000-0000-0000-0000-000000081001"
        )
        val p2=UUID.fromString(
            "00000000-0000-0000-0000-000000081002"
        )

        val stats=
            MatchStatsRecorder()

        stats.recordTowerBuilt(
            p1,
            200L
        )
        stats.recordTroopsSent(
            p1,
            12,
            360L
        )
        stats.recordTroopKill(
            p1,
            45L
        )

        stats.recordTowerBuilt(
            p2,
            75L
        )
        stats.recordTroopsSent(
            p2,
            2,
            40L
        )
        stats.recordTroopKill(
            p2,
            15L
        )

        // These unrelated match statistics must not alter the official
        // cumulative-cost contribution score.
        stats.recordCoinsEarned(
            p1,
            99_999L
        )
        stats.recordExpEarned(
            p1,
            88_888L
        )
        stats.recordTowerSold(
            p1
        )
        stats.recordCastleDamageDone(
            p1,
            123.0
        )

        stats.synchronizeHistoricalOverallScores()
        val snapshot=
            stats.snapshot()
        val scores=
            HistoricalOverallScoreCalculator
                .byPlayer(
                    snapshot
                )

        return listOf(
            FixtureResult(
                "historical-overall-score-sums-three-official-cumulative-cost-buckets",
                scores[p1]==605L &&
                    scores[p2]==130L
            ),
            FixtureResult(
                "historical-overall-score-ignores-unrelated-match-stats",
                snapshot.byPlayer
                    .getValue(p1)
                    .overallScore==605L &&
                    snapshot.byPlayer
                        .getValue(p1)
                        .coinsEarned==99_999L &&
                    snapshot.byPlayer
                        .getValue(p1)
                        .expEarned==88_888L
            ),
            FixtureResult(
                "historical-overall-score-synchronizes-into-final-player-stats",
                snapshot.byPlayer
                    .getValue(p1)
                    .overallScore==605L &&
                    snapshot.byPlayer
                        .getValue(p2)
                        .overallScore==130L
            )
        )
    }
}
