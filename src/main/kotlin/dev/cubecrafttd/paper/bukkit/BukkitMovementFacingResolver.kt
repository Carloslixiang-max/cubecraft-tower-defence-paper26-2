package dev.cubecrafttd.paper.bukkit

import kotlin.math.atan2

/**
 * Paper-only visual facing helper.
 *
 * Core route position remains authoritative. This only derives the yaw used by
 * the live entity teleport so AI-disabled mobs face their actual movement
 * direction instead of visually sliding sideways around route turns.
 */
object BukkitMovementFacingResolver {
    private const val HORIZONTAL_EPSILON_SQUARED =
        1.0e-12

    fun yawDegrees(
        fromX: Double,
        fromZ: Double,
        toX: Double,
        toZ: Double,
        currentYaw: Float
    ): Float {
        val dx=toX-fromX
        val dz=toZ-fromZ
        if(
            dx*dx + dz*dz <=
                HORIZONTAL_EPSILON_SQUARED
        ) return currentYaw

        return Math.toDegrees(
            atan2(
                -dx,
                dz
            )
        ).toFloat()
    }
}
