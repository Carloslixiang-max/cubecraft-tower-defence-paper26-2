package dev.cubecrafttd.testing

import dev.cubecrafttd.ui.HistoricalMatchSidebarProjector

object HistoricalMatchSidebarFixture {
    fun run(): List<FixtureResult> {
        val view=
            HistoricalMatchSidebarProjector
                .project(
                    3490L,
                    565L
                )

        return listOf(
            FixtureResult(
                "historical-match-sidebar-title-and-footer",
                view.title=="Tower Defence" &&
                    view.footer==
                        "play.cubecraft.net"
            ),
            FixtureResult(
                "historical-match-sidebar-coins-and-exp-lines",
                view.coinsLine=="3490 Coins" &&
                    view.expLine=="565 EXP"
            )
        )
    }
}
