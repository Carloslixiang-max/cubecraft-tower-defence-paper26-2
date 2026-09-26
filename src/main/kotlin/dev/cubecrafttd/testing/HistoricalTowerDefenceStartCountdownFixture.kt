package dev.cubecrafttd.testing

import dev.cubecrafttd.match.HistoricalTowerDefenceStartCountdown

object HistoricalTowerDefenceStartCountdownFixture {
    fun run(): List<FixtureResult> {
        val countdown=
            HistoricalTowerDefenceStartCountdown()

        val first=countdown.advance()
        val second=countdown.advance()
        val third=countdown.advance()
        val start=countdown.advance()
        val repeatedStartRejected=
            runCatching {
                countdown.advance()
            }.isFailure

        return listOf(
            FixtureResult(
                "historical-td-start-countdown-announces-3-2-1",
                listOf(
                    first.announceSeconds,
                    second.announceSeconds,
                    third.announceSeconds
                )==listOf(3,2,1) &&
                    listOf(
                        first.startNow,
                        second.startNow,
                        third.startNow
                    ).none { it }
            ),
            FixtureResult(
                "historical-td-start-countdown-starts-on-next-second-once",
                start.startNow &&
                    start.announceSeconds==null &&
                    countdown.isCompleted() &&
                    repeatedStartRejected
            )
        )
    }
}
