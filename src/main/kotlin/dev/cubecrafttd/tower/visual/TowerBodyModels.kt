package dev.cubecrafttd.tower.visual

import dev.cubecrafttd.map.BlockPos

enum class BodyEvidenceStatus {
    MATURE_DIRECT,
    MATURE_CONTEXT,
    HISTORICAL_REFERENCE,
    COMMUNITY_RECONSTRUCTION,
    ENGINEERING_PLACEHOLDER
}

enum class TowerPath { TOP, BOTTOM }

enum class QuarterTurn(val degrees: Int) { R0(0), R90(90), R180(180), R270(270) }

data class RelativeBodyBlock(
    val pos: BlockPos,
    val blockData: String
)

data class TowerBodyDefinition(
    val towerId: String,
    val level: Int,
    val path: TowerPath?,
    val bodyRevision: String,
    val blocks: List<RelativeBodyBlock>,
    val evidenceStatus: BodyEvidenceStatus,
    val evidenceRefs: List<String>
)

data class BlockKey(val x: Int, val y: Int, val z: Int)

data class BlockSnapshot(
    val blockData: String,
    val optionalStatePayload: ByteArray? = null
)
