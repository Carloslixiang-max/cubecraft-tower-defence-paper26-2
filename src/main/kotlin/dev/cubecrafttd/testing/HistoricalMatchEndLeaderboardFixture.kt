package dev.cubecrafttd.testing

import dev.cubecrafttd.stats.*
import dev.cubecrafttd.ui.*
import java.util.UUID

object HistoricalMatchEndLeaderboardFixture {
    fun run(): List<FixtureResult> {
        val p1=UUID.fromString(
            "00000000-0000-0000-0000-000000082001"
        )
        val p2=UUID.fromString(
            "00000000-0000-0000-0000-000000082002"
        )
        val p3=UUID.fromString(
            "00000000-0000-0000-0000-000000082003"
        )
        val p4=UUID.fromString(
            "00000000-0000-0000-0000-000000082004"
        )

        val stats=
            MatchStatsSnapshot(
                linkedMapOf(
                    p1 to
                        MatchPlayerStats(
                            overallScore=100L,
                            towersBuilt=1,
                            troopsSent=2,
                            troopsKilled=3
                        ),
                    p2 to
                        MatchPlayerStats(
                            overallScore=300L,
                            towersBuilt=4,
                            troopsSent=5,
                            troopsKilled=6
                        ),
                    p3 to
                        MatchPlayerStats(
                            overallScore=200L,
                            towersBuilt=7,
                            troopsSent=8,
                            troopsKilled=9
                        ),
                    p4 to
                        MatchPlayerStats(
                            overallScore=200L,
                            towersBuilt=10,
                            troopsSent=11,
                            troopsKilled=12
                        )
                )
            )

        val projection=
            HistoricalMatchEndLeaderboardProjector
                .project(
                    stats
                )

        return listOf(
            FixtureResult(
                "historical-top-players-heading-and-score-order",
                projection.heading==
                    "Top players:" &&
                    projection.entries
                        .map {
                            it.playerUuid
                        }==
                    listOf(
                        p2,p3,p4
                    ) &&
                    projection.entries
                        .map {
                            it.rank
                        }==
                    listOf(1,2,3)
            ),
            FixtureResult(
                "historical-top-players-hover-stats-preserved",
                projection.entries[0]
                    .let {
                        it.overallScore==300L &&
                            it.towersPlaced==4 &&
                            it.troopsSent==5 &&
                            it.enemiesKilled==6
                    }
            ),
            FixtureResult(
                "historical-top-players-equal-score-order-remains-explicit-fallback",
                projection.tiePolicyEvidence==
                    HistoricalTopPlayersTiePolicyEvidence
                        .ENGINEERING_UUID_ASCENDING &&
                    projection.entries[1]
                        .playerUuid==p3 &&
                    projection.entries[2]
                        .playerUuid==p4
            )
        )
    }
}
