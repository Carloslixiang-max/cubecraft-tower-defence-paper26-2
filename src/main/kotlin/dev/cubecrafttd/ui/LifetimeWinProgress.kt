package dev.cubecrafttd.ui

/**
 * Small pure rule shared by the Paper persistence adapter and fixtures.
 *
 * The current recreation uses the recovered 20-win Auto-centre unlock rule.
 * Persistence is handled separately by the Paper adapter.
 */
object LifetimeWinProgress {
    const val AUTO_CENTRE_DISABLE_WINS:
        Int = 20

    fun nextAfterWin(
        currentWins: Int
    ): Int {
        require(currentWins>=0)
        return Math.addExact(
            currentWins,
            1
        )
    }

    fun canDisableAutoCentre(
        lifetimeWins: Int
    ): Boolean {
        require(lifetimeWins>=0)
        return lifetimeWins>=
            AUTO_CENTRE_DISABLE_WINS
    }
}
