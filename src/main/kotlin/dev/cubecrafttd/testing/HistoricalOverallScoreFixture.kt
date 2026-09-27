package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.economy.*
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
        val ledger=EconomyLedger()

        fun apply(
            id: Long,
            player: UUID,
            currency: EconomyCurrency,
            delta: Long,
            reason: EconomyReason,
            correlation: String
        ) {
            check(
                ledger.apply(
                    EconomyTransaction(
                        id,
                        id,
                        EconomyAccount(
                            TeamId.RED,
                            player,
                            currency
                        ),
                        delta,
                        reason,
                        correlation
                    ),
                    allowNegative=false
                )
            )
        }

        apply(
            1,p1,
            EconomyCurrency.MATCH_COINS,
            5_000L,
            EconomyReason.MATCH_INITIALIZATION,
            "seed-p1-coins"
        )
        apply(
            2,p2,
            EconomyCurrency.MATCH_COINS,
            5_000L,
            EconomyReason.MATCH_INITIALIZATION,
            "seed-p2-coins"
        )
        apply(
            3,p1,
            EconomyCurrency.MATCH_EXP,
            1_000L,
            EconomyReason.MATCH_INITIALIZATION,
            "seed-p1-exp"
        )

        apply(
            10,p1,
            EconomyCurrency.MATCH_COINS,
            -100L,
            EconomyReason.TROOP_PURCHASE,
            "score-troops"
        )
        apply(
            11,p1,
            EconomyCurrency.MATCH_COINS,
            -200L,
            EconomyReason.TOWER_PURCHASE,
            "score-tower"
        )
        apply(
            12,p1,
            EconomyCurrency.MATCH_COINS,
            -300L,
            EconomyReason.TOWER_UPGRADE,
            "score-upgrade"
        )
        apply(
            13,p1,
            EconomyCurrency.MATCH_COINS,
            -400L,
            EconomyReason.BAZAAR,
            "score-bazaar"
        )

        // Transfers move Coins but are not player spending for historical MVP.
        apply(
            14,p1,
            EconomyCurrency.MATCH_COINS,
            -500L,
            EconomyReason.TEAM_SHARE,
            "share-debit"
        )

        // EXP spending is intentionally outside Overall score.
        apply(
            15,p1,
            EconomyCurrency.MATCH_EXP,
            -125L,
            EconomyReason.BAZAAR,
            "goldmine-exp"
        )

        // Admin/debug debits must never leak into a real leaderboard score.
        apply(
            16,p1,
            EconomyCurrency.MATCH_COINS,
            -75L,
            EconomyReason.ADMIN_TEST,
            "admin-debit"
        )

        apply(
            20,p2,
            EconomyCurrency.MATCH_COINS,
            -250L,
            EconomyReason.BAZAAR,
            "p2-bazaar"
        )

        val scores=
            HistoricalOverallScoreCalculator
                .byPlayer(
                    ledger
                )
        val stats=
            MatchStatsRecorder()
        stats.synchronizeHistoricalOverallScores(
            ledger
        )
        val snapshot=
            stats.snapshot()

        return listOf(
            FixtureResult(
                "historical-overall-score-counts-only-real-coin-purchases",
                scores[p1]==1_000L &&
                    scores[p2]==250L
            ),
            FixtureResult(
                "historical-overall-score-excludes-share-exp-and-admin-flows",
                scores[p1]!=1_700L &&
                    HistoricalOverallScoreCalculator
                        .qualifyingReasons==
                    setOf(
                        EconomyReason.TROOP_PURCHASE,
                        EconomyReason.TOWER_PURCHASE,
                        EconomyReason.TOWER_UPGRADE,
                        EconomyReason.BAZAAR
                    )
            ),
            FixtureResult(
                "historical-overall-score-synchronizes-into-match-stats",
                snapshot.byPlayer
                    .getValue(p1)
                    .overallScore==1_000L &&
                    snapshot.byPlayer
                        .getValue(p2)
                        .overallScore==250L
            )
        )
    }
}
