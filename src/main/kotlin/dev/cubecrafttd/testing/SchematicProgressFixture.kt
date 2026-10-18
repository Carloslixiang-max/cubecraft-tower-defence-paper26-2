package dev.cubecrafttd.testing

import dev.cubecrafttd.paper.bukkit.SchematicPasteProgress

object SchematicProgressFixture {
    fun run(): List<FixtureResult> = listOf(
        FixtureResult("sparse-schematic-air-cells-contribute-to-progress",
            SchematicPasteProgress("PASTE", 100, 1, 100, 100).percentage == 100,
            listOf("A mostly AIR scene must not finish at 50%")),
        FixtureResult("schematic-phase-boundary-progress-is-monotonic",
            SchematicPasteProgress("PREFLIGHT", 100, 0, 100, 0).percentage == 50 &&
                SchematicPasteProgress("PASTE", 100, 0, 100, 0).percentage == 50,
            listOf("Scan to paste remains at 50%")),
        FixtureResult("schematic-completion-is-one-hundred-percent",
            SchematicPasteProgress("COMPLETE", 100, 1, 100, 100).percentage == 100,
            listOf("Completion reports 100% regardless of sparse mutation count")),
        FixtureResult("large-schematic-progress-does-not-overflow",
            SchematicPasteProgress("PASTE", Int.MAX_VALUE, 1, Int.MAX_VALUE, Int.MAX_VALUE).percentage == 100,
            listOf("Progress multiplication uses Long arithmetic"))
    )
}
