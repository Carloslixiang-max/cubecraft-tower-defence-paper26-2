package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.paper.LiveLineOfSightPort
import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import java.util.UUID

class BukkitLineOfSightPort(
    private val server: Server
) : LiveLineOfSightPort {
    override fun hasLineOfSight(
        origin: Vec3,
        targetEntityUuid: UUID
    ): Boolean = server.getEntity(targetEntityUuid)?.let { trace(origin, it) { false } } ?: false

    fun hasTowerLineOfSight(origin: Vec3, targetEntityUuid: UUID, owns: (Block) -> Boolean): Boolean =
        server.getEntity(targetEntityUuid)?.let { trace(origin, it, owns) } ?: false

    fun hasTowerLineOfSight(origin: Vec3, target: Entity, owns: (Block) -> Boolean): Boolean =
        trace(origin, target, owns)

    private fun trace(origin: Vec3, target: Entity, owns: (Block) -> Boolean): Boolean {
        val targetLocation = target.location
        val start = Location(
            target.world,
            origin.x,
            origin.y,
            origin.z
        )
        val delta = targetLocation.toVector()
            .subtract(start.toVector())
        val distance = delta.length()
        if (distance <= 1.0e-9) return true

        val direction = delta.normalize()
        val original = start.toVector()
        var cursor = start
        repeat(64) {
            val remaining = distance - original.distance(cursor.toVector())
            if (remaining <= 1.0e-6) return true
            val hit = target.world.rayTraceBlocks(cursor, direction, remaining, FluidCollisionMode.NEVER, true)
                ?: return true
            val block = hit.hitBlock ?: return true
            if (!owns(block)) return false
            // Skip only this tower-owned voxel, including partial slab/fence collisions.
            // Advance to its far cell boundary; a nearby wall or other tower is still tested.
            val point = hit.hitPosition
            fun exit(p: Double, cell: Int, d: Double): Double = when {
                d > 1.0e-9 -> (cell + 1.0 - p) / d
                d < -1.0e-9 -> (cell.toDouble() - p) / d
                else -> Double.POSITIVE_INFINITY
            }
            val step = minOf(exit(point.x, block.x, direction.x), exit(point.y, block.y, direction.y),
                exit(point.z, block.z, direction.z)).coerceAtLeast(0.0) + 1.0e-5
            val next = point.clone().add(direction.clone().multiply(step))
            cursor = Location(target.world, next.x, next.y, next.z)
        }
        return false
    }
}
