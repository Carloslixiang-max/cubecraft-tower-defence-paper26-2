package dev.cubecrafttd.player

import kotlin.math.abs
import kotlin.math.max

/**
 * Engineering-only world geometry for the first live AoE playtest.
 *
 * Recovered tables expose an effect "length" for several potions but do not
 * prove the exact hidden world-shape implementation. The current playtest
 * therefore interprets that length as a horizontal diameter and uses a bounded
 * vertical tolerance. This must not be promoted to original truth.
 */
object AoEEngineeringWorldGeometry {
    fun radiusFromEffectLength(
        effectLengthBlocks: Double
    ): Double {
        require(effectLengthBlocks>0.0)
        return effectLengthBlocks / 2.0
    }

    fun inside(
        targetX: Double,
        targetY: Double,
        targetZ: Double,
        entityX: Double,
        entityY: Double,
        entityZ: Double,
        radiusBlocks: Double
    ): Boolean {
        require(radiusBlocks>0.0)
        val dx=entityX-targetX
        val dz=entityZ-targetZ
        val dy=abs(entityY-targetY)
        return dx*dx + dz*dz <=
            radiusBlocks*radiusBlocks &&
            dy <= max(
                3.0,
                radiusBlocks
            )
    }
}
