package dev.cubecrafttd.tower.visual

import dev.cubecrafttd.map.BlockPos

data class RotatedBodyKey(
    val towerId: String,
    val level: Int,
    val path: TowerPath?,
    val bodyRevision: String,
    val rotation: QuarterTurn
)

class RotationCache(
    private val blockDataRotator: (String, QuarterTurn) -> String
) {
    private val cache = linkedMapOf<RotatedBodyKey, List<RelativeBodyBlock>>()

    fun resolve(definition: TowerBodyDefinition, rotation: QuarterTurn): List<RelativeBodyBlock> {
        val key = RotatedBodyKey(
            definition.towerId,
            definition.level,
            definition.path,
            definition.bodyRevision,
            rotation
        )
        return cache.getOrPut(key) {
            definition.blocks.map { block ->
                RelativeBodyBlock(
                    pos = rotate(block.pos, rotation),
                    blockData = blockDataRotator(block.blockData, rotation)
                )
            }
        }
    }

    private fun rotate(pos: BlockPos, rotation: QuarterTurn): BlockPos = when (rotation) {
        QuarterTurn.R0 -> pos
        QuarterTurn.R90 -> BlockPos(-pos.z, pos.y, pos.x)
        QuarterTurn.R180 -> BlockPos(-pos.x, pos.y, -pos.z)
        QuarterTurn.R270 -> BlockPos(pos.z, pos.y, -pos.x)
    }

    fun clear() = cache.clear()
    fun size(): Int = cache.size
}
