package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.ArenaId
import org.bukkit.World
import org.bukkit.plugin.Plugin

/** CI-owned world only. Real Paper tickets, overlapping leases and prior owner. */
object BukkitArenaChunkLeaseSmoke {
    fun run(plugin: Plugin, world: World) {
        val leases = BukkitArenaChunkLeases(plugin)
        val first = ArenaId("chunk-lease-smoke-a")
        val second = ArenaId("chunk-lease-smoke-b")
        check(world.addPluginChunkTicket(12, 12, plugin))
        try {
            leases.acquireChunks(first, world, setOf(12 to 12, 13 to 12))
            leases.acquireChunks(second, world, setOf(13 to 12))
            check(world.isChunkLoaded(13, 12))
            check(plugin in world.getPluginChunkTickets(13, 12))
            leases.release(first)
            check(plugin in world.getPluginChunkTickets(12, 12)) { "Pre-existing ticket removed" }
            check(plugin in world.getPluginChunkTickets(13, 12)) { "Shared lease removed too early" }
            leases.release(second)
            check(plugin !in world.getPluginChunkTickets(13, 12)) { "Last owned lease leaked" }
            leases.release(second)
            leases.acquireChunks(first, world, setOf(13 to 12))
            leases.close()
            check(plugin !in world.getPluginChunkTickets(13, 12))
        } finally {
            leases.close()
            world.removePluginChunkTicket(12, 12, plugin)
            world.removePluginChunkTicket(13, 12, plugin)
        }
        plugin.logger.info("Arena chunk leases PASS: shared references, pre-existing ownership, final release and close")
    }
}
