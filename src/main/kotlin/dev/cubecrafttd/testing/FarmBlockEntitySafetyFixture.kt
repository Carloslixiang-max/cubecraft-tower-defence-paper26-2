package dev.cubecrafttd.testing

import dev.cubecrafttd.map.schematic.SpongeSchematicV2Decoder

object FarmBlockEntitySafetyFixture {
    fun run(bytes: ByteArray): List<FixtureResult> {
        val volume=SpongeSchematicV2Decoder().decode(bytes)
        val entities=volume.blockEntities
        val allowed=entities.all { it.id=="minecraft:beacon" }
        val positions=entities.map { Triple(it.x,it.y,it.z) }.toSet()
        return listOf(
            FixtureResult(
                "farm-schematic-no-entities",
                volume.schematicEntityCount==0
            ),
            FixtureResult(
                "farm-schematic-only-supported-beacon-blockentities",
                entities.size==2 && allowed
            ),
            FixtureResult(
                "farm-schematic-beacon-blockentity-positions",
                positions==setOf(
                    Triple(26,7,70),
                    Triple(98,7,117)
                )
            )
        )
    }
}
