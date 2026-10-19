package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.map.Vec3

/** Farm waypoints describe the measured path blocks, rather than entity feet.
 * Keep core geometry/distances intact and project only at the Paper boundary.
 */
object BukkitFarmMobCoordinates {
    fun feet(surface: Vec3): Vec3 =
        Vec3(surface.x + 0.5, surface.y + 1.0, surface.z + 0.5)
}
