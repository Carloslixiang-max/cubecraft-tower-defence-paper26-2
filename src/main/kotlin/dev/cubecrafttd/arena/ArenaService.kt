package dev.cubecrafttd.arena

class ArenaService {
    private val arenas = linkedMapOf<ArenaId, ArenaContext>()

    fun register(context: ArenaContext) {
        check(context.arenaId !in arenas) { "Arena already registered: ${context.arenaId.value}" }
        arenas[context.arenaId] = context
    }

    fun get(arenaId: ArenaId): ArenaContext? = arenas[arenaId]

    fun contexts(): Collection<ArenaContext> = arenas.values.toList()

    fun close(arenaId: ArenaId, teardown: (ArenaContext) -> Unit) {
        val context = arenas[arenaId] ?: return
        if (context.state != ArenaState.CLOSED) {
            teardown(context)
            context.taskGroup.close()
            context.entityIndex.clearAll()
            context.state = ArenaState.CLOSED
        }
        arenas.remove(arenaId)
    }

    fun closeAll(teardown: (ArenaContext) -> Unit) {
        arenas.keys.toList().forEach { close(it, teardown) }
    }
}
