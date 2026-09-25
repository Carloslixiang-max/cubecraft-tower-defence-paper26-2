package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.ArenaTaskHandle
import dev.cubecrafttd.paper.PaperSchedulerAdapter
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

class BukkitSchedulerAdapter(
    private val plugin: Plugin
) : PaperSchedulerAdapter {
    override fun repeat(
        delayTicks: Long,
        periodTicks: Long,
        action: () -> Unit
    ): ArenaTaskHandle {
        require(delayTicks >= 0L)
        require(periodTicks > 0L)
        val task = plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable(action),
            delayTicks,
            periodTicks
        )
        return BukkitArenaTaskHandle(task)
    }

    override fun later(
        delayTicks: Long,
        action: () -> Unit
    ): ArenaTaskHandle {
        require(delayTicks >= 0L)
        val task = plugin.server.scheduler.runTaskLater(
            plugin,
            Runnable(action),
            delayTicks
        )
        return BukkitArenaTaskHandle(task)
    }
}

private class BukkitArenaTaskHandle(
    private val task: BukkitTask
) : ArenaTaskHandle {
    override val isCancelled: Boolean
        get() = task.isCancelled

    override fun cancel() {
        if (!task.isCancelled) task.cancel()
    }
}
