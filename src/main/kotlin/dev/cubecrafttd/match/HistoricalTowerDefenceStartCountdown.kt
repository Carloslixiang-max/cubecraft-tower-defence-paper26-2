package dev.cubecrafttd.match

data class HistoricalTowerDefenceStartCountdownStep(
    val announceSeconds: Int?,
    val startNow: Boolean
)

/**
 * Historical direct-log behavior:
 * Tower Defence announced 3, 2, 1 seconds in chat immediately before the game
 * selected voting outcomes and started. This small state machine preserves that
 * recovered three-second cadence without making any claim about older/newer
 * lobby implementations outside the observed log.
 */
class HistoricalTowerDefenceStartCountdown(
    initialSeconds: Int = 3
) {
    private var remaining=
        initialSeconds
    private var completed=
        false

    init {
        require(initialSeconds>0)
    }

    fun advance():
        HistoricalTowerDefenceStartCountdownStep {
        check(!completed) {
            "Start countdown already completed"
        }

        if(remaining>0) {
            val announce=remaining
            remaining--
            return HistoricalTowerDefenceStartCountdownStep(
                announceSeconds=announce,
                startNow=false
            )
        }

        completed=true
        return HistoricalTowerDefenceStartCountdownStep(
            announceSeconds=null,
            startNow=true
        )
    }

    fun remainingSeconds(): Int =
        remaining

    fun isCompleted(): Boolean =
        completed
}
