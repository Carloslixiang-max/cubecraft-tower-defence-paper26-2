package dev.cubecrafttd.arena

/**
 * Scheduler abstraction. Paper-specific task handles are adapted behind ArenaTaskHandle.
 * Arena close must be idempotent and cancel every registered handle.
 */
interface ArenaTaskHandle {
    val isCancelled: Boolean
    fun cancel()
}

class ArenaTaskGroup {
    private val handles = linkedSetOf<ArenaTaskHandle>()
    var closed: Boolean = false
        private set

    fun register(handle: ArenaTaskHandle): ArenaTaskHandle {
        check(!closed) { "TaskGroup already closed" }
        handles += handle
        return handle
    }

    fun unregister(handle: ArenaTaskHandle) {
        handles -= handle
    }

    fun close() {
        if (closed) return
        closed = true
        handles.toList().forEach { it.cancel() }
        handles.clear()
    }

    fun activeCount(): Int = handles.count { !it.isCancelled }
}
