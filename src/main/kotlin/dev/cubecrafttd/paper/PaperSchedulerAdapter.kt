package dev.cubecrafttd.paper

import dev.cubecrafttd.arena.ArenaTaskHandle

/**
 * Paper implementation will wrap BukkitScheduler/Paper scheduler handles.
 * Core domain only knows ArenaTaskHandle.
 */
interface PaperSchedulerAdapter {
    fun repeat(delayTicks: Long, periodTicks: Long, action: () -> Unit): ArenaTaskHandle
    fun later(delayTicks: Long, action: () -> Unit): ArenaTaskHandle
    fun nextTick(action: () -> Unit): ArenaTaskHandle = later(1L, action)
}
