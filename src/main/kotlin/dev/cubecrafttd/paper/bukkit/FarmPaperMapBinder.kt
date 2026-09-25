package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.map.*
import dev.cubecrafttd.map.schematic.*
import java.util.UUID

/**
 * Pure planning/binding step for the currently verified Farm community asset.
 * It does not mutate a Bukkit world.
 */
data class FarmPaperMapPlan(
    val worldUid: UUID,
    val origin: BlockPos,
    val volume: DecodedMapVolume,
    val runtime: MapRuntimeDefinition
)

class FarmPaperMapBinder {
    fun prepare(
        bytes: ByteArray,
        worldUid: UUID,
        origin: BlockPos
    ): FarmPaperMapPlan {
        val volume=SpongeSchematicV2Decoder().decode(bytes)
        check(volume.schematicEntityCount==0) {
            "Farm paste refuses schematic entities until entity NBT import is implemented"
        }
        check(volume.blockEntities.all { it.id=="minecraft:beacon" }) {
            "Farm paste currently supports only the two verified beacon BlockEntities"
        }
        /*
         * The verified source asset contains exactly two beacon BlockEntities.
         * Raw NBT audit: Primary=0, Secondary=0, Levels=1 for both.
         * There is no custom beacon effect state to preserve; Levels is
         * recalculated by the live beacon from its block structure.
         */

        val observation=SchematicMarkerClassifier(
            SchematicMarkerConfig.farm()
        ).classify(volume)
        val local=SchematicObservationRuntimeCompiler().compile(
            volume,
            observation,
            SchematicImportMetadata(
                mapId="farm_improved_2022_candidate",
                revision="paper-binding-v1",
                authenticity=MapAuthenticity.COMMUNITY_IMPROVEMENT_BY_ORIGINAL_TRACK_AUTHOR,
                productionOriginal=false,
                evidenceRefs=listOf("verified ImprovedFarm.schem runtime fixture")
            )
        )
        val translated=MapRuntimeWorldTranslator.translate(
            local,
            MapWorldTranslation(origin.x,origin.y,origin.z)
        )
        return FarmPaperMapPlan(
            worldUid,origin,volume,translated
        )
    }
}
