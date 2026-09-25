package dev.cubecrafttd.paper

import dev.cubecrafttd.arena.ArenaTaskHandle
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

private class BukkitArenaTaskHandle(
    private val task: BukkitTask
) : ArenaTaskHandle {
    override val isCancelled: Boolean get() = task.isCancelled
    override fun cancel() = task.cancel()
}

class BukkitSchedulerAdapter(
    private val plugin: Plugin
) : PaperSchedulerAdapter {
    override fun repeat(
        delayTicks: Long,
        periodTicks: Long,
        action: () -> Unit
    ): ArenaTaskHandle {
        require(delayTicks >= 0)
        require(periodTicks > 0)
        return BukkitArenaTaskHandle(
            plugin.server.scheduler.runTaskTimer(
                plugin,
                Runnable(action),
                delayTicks,
                periodTicks
            )
        )
    }

    override fun later(
        delayTicks: Long,
        action: () -> Unit
    ): ArenaTaskHandle {
        require(delayTicks >= 0)
        return BukkitArenaTaskHandle(
            plugin.server.scheduler.runTaskLater(
                plugin,
                Runnable(action),
                delayTicks
            )
        )
    }
}
