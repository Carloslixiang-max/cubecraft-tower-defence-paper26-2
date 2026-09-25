package dev.cubecrafttd.tower.visual

import dev.cubecrafttd.map.BlockPos

fun interface TowerBodyDefinitionProvider {
    fun definition(
        towerId: String,
        level: Int,
        path: TowerPath
    ): TowerBodyDefinition
}

/**
 * Stage-4 functional placeholder only.
 *
 * It deliberately creates a tiny visible marker rather than pretending the
 * unrecovered Mature block model is known. Placement footprint/range/economy
 * still come from the real tower definition/runtime.
 */
object EngineeringPlaceholderTowerBodyProvider :
    TowerBodyDefinitionProvider {
    override fun definition(
        towerId: String,
        level: Int,
        path: TowerPath
    ): TowerBodyDefinition {
        require(level in 1..4)
        return TowerBodyDefinition(
            towerId=towerId,
            level=level,
            path=path,
            bodyRevision=
                "engineering-placeholder-v1",
            blocks=listOf(
                RelativeBodyBlock(
                    BlockPos(0,0,0),
                    "minecraft:stone"
                ),
                RelativeBodyBlock(
                    BlockPos(0,1,0),
                    "minecraft:glass"
                )
            ),
            evidenceStatus=
                BodyEvidenceStatus
                    .ENGINEERING_PLACEHOLDER,
            evidenceRefs=listOf(
                "Stage-4 functional placeholder; not CubeCraft visual truth"
            )
        )
    }
}
