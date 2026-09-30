package dev.cubecrafttd.admin

import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.map.schematic.SchematicDimensions

sealed interface EngineeringPlaytestSetupRequest {
    data object StandingPlayer : EngineeringPlaytestSetupRequest
    data class ExplicitOrigin(val worldName: String, val origin: BlockPos) : EngineeringPlaytestSetupRequest

    companion object {
        const val USAGE = "Usage: /ctdplaytestsetup apply [<world> <originX> <originY> <originZ>]"

        fun parse(args: List<String>): EngineeringPlaytestSetupRequest {
            require(args.firstOrNull()?.lowercase() == "apply") { USAGE }
            if (args.size == 1) return StandingPlayer
            require(args.size == 5 && args[1].isNotBlank()) { USAGE }
            val coordinates = args.drop(2).map {
                it.toIntOrNull() ?: error("Map origin coordinates must be whole 32-bit integers")
            }
            return ExplicitOrigin(args[1], BlockPos(coordinates[0], coordinates[1], coordinates[2]))
        }
    }
}

/** Server deployment limits, never an assertion of original CubeCraft coordinates. */
object EngineeringMapPlacementBounds {
    fun validate(origin: BlockPos, dimensions: SchematicDimensions, minHeight: Int, maxHeight: Int) {
        require(dimensions.width > 0 && dimensions.height > 0 && dimensions.length > 0)
        require(minHeight < maxHeight)
        val maxX = origin.x.toLong() + dimensions.width - 1
        val maxY = origin.y.toLong() + dimensions.height - 1
        val maxZ = origin.z.toLong() + dimensions.length - 1
        require(origin.x >= -30_000_000 && maxX < 30_000_000 &&
            origin.z >= -30_000_000 && maxZ < 30_000_000) {
            "The entire schematic must fit inside Minecraft's horizontal world limits"
        }
        require(origin.y >= minHeight && maxY < maxHeight) {
            "The entire schematic must fit inside world Y range [$minHeight, $maxHeight)"
        }
    }
}
