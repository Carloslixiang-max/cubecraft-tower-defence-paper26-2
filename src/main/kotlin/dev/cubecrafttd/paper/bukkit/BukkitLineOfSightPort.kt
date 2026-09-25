package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.paper.LiveLineOfSightPort
import org.bukkit.FluidCollisionMode
import org.bukkit.Location
import org.bukkit.Server
import java.util.UUID

class BukkitLineOfSightPort(
    private val server: Server
) : LiveLineOfSightPort {
    override fun hasLineOfSight(
        origin: Vec3,
        targetEntityUuid: UUID
    ): Boolean {
        val target = server.getEntity(targetEntityUuid)
            ?: return false
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

        val hit = target.world.rayTraceBlocks(
            start,
            delta.normalize(),
            distance,
            FluidCollisionMode.NEVER,
            true
        )
        return hit == null
    }
}
