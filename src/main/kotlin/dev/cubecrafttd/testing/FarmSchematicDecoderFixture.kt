package dev.cubecrafttd.testing

import dev.cubecrafttd.map.schematic.SpongeSchematicV2Decoder

object FarmSchematicDecoderFixture {
    const val EXPECTED_SHA256 = "28d24136afe80358b89556d0fbe3b08d00e518af38c7c518819fa7fc225e613e"

    fun run(bytes: ByteArray): List<FixtureResult> {
        val volume = SpongeSchematicV2Decoder().decode(bytes)
        return listOf(
            FixtureResult("schem-sha256", volume.sha256 == EXPECTED_SHA256),
            FixtureResult(
                "schem-dimensions",
                volume.dimensions.width == 125 &&
                    volume.dimensions.height == 18 &&
                    volume.dimensions.length == 191
            ),
            FixtureResult(
                "schem-offset",
                volume.offset.x == -61 && volume.offset.y == 54 && volume.offset.z == -95
            ),
            FixtureResult("schem-has-red-wool", volume.countBlock("minecraft:red_wool") > 0),
            FixtureResult("schem-has-blue-wool", volume.countBlock("minecraft:blue_wool") > 0),
            FixtureResult("schem-has-red-terracotta", volume.countBlock("minecraft:red_terracotta") > 0),
            FixtureResult("schem-has-blue-terracotta", volume.countBlock("minecraft:blue_terracotta") > 0),
            FixtureResult("schem-red-spawn-beacon", volume.blockAt(26, 7, 70) == "minecraft:beacon"),
            FixtureResult("schem-blue-spawn-beacon", volume.blockAt(98, 7, 117) == "minecraft:beacon"),
            FixtureResult("schem-blue-route-start-surface", volume.blockAt(98, 8, 116) == "minecraft:jungle_planks"),
            FixtureResult("schem-red-terminal-surface", volume.blockAt(62, 3, 122) == "minecraft:jungle_planks"),
            FixtureResult("schem-blue-terminal-surface", volume.blockAt(62, 3, 65) == "minecraft:jungle_planks")
        )
    }
}
