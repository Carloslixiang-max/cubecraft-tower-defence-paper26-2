package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.ArenaId
import dev.cubecrafttd.arena.ArenaSpatialEnvelopeResolver
import dev.cubecrafttd.map.MapRuntimeDefinition
import org.bukkit.World
import org.bukkit.plugin.Plugin
import java.util.UUID
import kotlin.math.floor

/** Keep arena entities loaded independently of player view/simulation distance.
 * Shared boundary chunks are reference-counted. Never remove a pre-existing
 * ticket owned by another component of this plugin, or another plugin's ticket.
 */
class BukkitArenaChunkLeases(private val plugin: Plugin) {
    private data class Key(val world: UUID, val x: Int, val z: Int)
    private data class Ref(val world: World, var count: Int, val addedHere: Boolean)
    private val refs = linkedMapOf<Key, Ref>()
    private val leases = linkedMapOf<ArenaId, MutableSet<Key>>()

    fun acquire(arena: ArenaId, world: World, map: MapRuntimeDefinition) {
        // Decorative scene bounds reserve space, but don't need live entities.
        val bounds = ArenaSpatialEnvelopeResolver.fromMap(map.copy(sceneryReservationPoints = emptyList()))
        fun chunk(v: Double) = Math.floorDiv(floor(v).toInt(), 16)
        val chunks = buildSet {
            for (x in chunk(bounds.minX)..chunk(bounds.maxX)) {
                for (z in chunk(bounds.minZ)..chunk(bounds.maxZ)) add(x to z)
            }
        }
        acquireChunks(arena, world, chunks)
    }

    internal fun acquireChunks(arena: ArenaId, world: World, chunks: Set<Pair<Int, Int>>) {
        check(arena !in leases) { "Arena already holds chunk leases: $arena" }
        val held = linkedSetOf<Key>()
        leases[arena] = held
        try {
            for ((x, z) in chunks) {
                val key = Key(world.uid, x, z)
                val ref = refs[key] ?: Ref(world, 0, world.addPluginChunkTicket(x, z, plugin)).also { refs[key] = it }
                ref.count++
                held += key
            }
        } catch (error: Throwable) {
            runCatching { release(arena) }.onFailure(error::addSuppressed)
            throw error
        }
    }

    fun release(arena: ArenaId) {
        val held = leases[arena] ?: return
        var failure: Throwable? = null
        for (key in held.toList()) {
            try {
                val ref = refs.getValue(key)
                if (ref.count == 1) {
                    if (ref.addedHere) ref.world.removePluginChunkTicket(key.x, key.z, plugin)
                    refs.remove(key)
                } else ref.count--
                held.remove(key)
            } catch (error: Throwable) {
                if (failure == null) failure = error else failure.addSuppressed(error)
            }
        }
        if (held.isEmpty()) leases.remove(arena)
        failure?.let { throw it }
    }

    fun close() {
        var failure: Throwable? = null
        for (arena in leases.keys.toList()) {
            runCatching { release(arena) }.onFailure { error ->
                if (failure == null) failure = error else failure!!.addSuppressed(error)
            }
        }
        failure?.let { throw it }
    }
}
