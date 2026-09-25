package dev.cubecrafttd.economy

import dev.cubecrafttd.arena.*

data class GoldmineTickMetrics(
    var playersVisited: Int = 0,
    var totalCoinsGranted: Long = 0
)

class GoldmineTickPhase(
    private val runtime:
        NormalArenaRuntimeState,
    private val service:
        GoldmineIncomeService
) : ArenaTickPhase {
    override val order: Int = 20
    override val id: String = "goldmine"

    private val metrics=
        GoldmineTickMetrics()

    override fun tick(
        context: ArenaContext
    ) {
        runtime.goldmines.values
            .sortedBy {
                it.playerUuid.toString()
            }
            .forEach {
                metrics.playersVisited++
                metrics.totalCoinsGranted +=
                    service.tick(
                        it,context.gameTick
                    )
            }
    }

    fun metricsSnapshot():
        GoldmineTickMetrics =
        metrics.copy()
}
