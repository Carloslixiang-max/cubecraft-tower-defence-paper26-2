package dev.cubecrafttd.ui

data class HistoricalCastleHealthHudView(
    val title: String,
    val progress: Double
)

object HistoricalCastleHealthHudProjector {
    fun project(
        health: Double,
        maxHealth: Double
    ): HistoricalCastleHealthHudView {
        require(maxHealth>0.0)
        return HistoricalCastleHealthHudView(
            title="Castle Health",
            progress=
                (health/maxHealth)
                    .coerceIn(0.0,1.0)
        )
    }
}
