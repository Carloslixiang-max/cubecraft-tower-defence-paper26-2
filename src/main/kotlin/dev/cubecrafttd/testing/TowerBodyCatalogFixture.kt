package dev.cubecrafttd.testing

import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.tower.visual.*

object TowerBodyCatalogFixture {
    fun run(): List<FixtureResult> {
        val top = TowerBodyDefinition("archer", 1, TowerPath.TOP, "fixture-top", listOf(
            RelativeBodyBlock(BlockPos(0, 0, 0), "minecraft:oak_planks")),
            BodyEvidenceStatus.COMMUNITY_RECONSTRUCTION, listOf("https://example.invalid/synthetic-fixture"))
        val bottom = top.copy(path = TowerPath.BOTTOM, bodyRevision = "fixture-bottom",
            blocks = listOf(RelativeBodyBlock(BlockPos(1, 1, 0), "minecraft:stone")))
        val catalog = TowerBodyCatalog(listOf(top, bottom))
        fun rejects(vararg bodies: TowerBodyDefinition) = runCatching { TowerBodyCatalog(bodies.toList()) }.isFailure
        return listOf(
            FixtureResult("tower-catalog-selects-exact-path-and-keeps-absent-stage-placeholder-explicit",
                catalog.definition("archer", 1, TowerPath.TOP) == top &&
                    catalog.definition("archer", 1, TowerPath.BOTTOM) == bottom && catalog.loadedCount() == 2 &&
                    catalog.definition("archer", 2, TowerPath.TOP).evidenceStatus == BodyEvidenceStatus.ENGINEERING_PLACEHOLDER),
            FixtureResult("tower-catalog-strict-coverage-refuses-incomplete-and-empty-catalogs",
                runCatching { TowerBodyCatalog(listOf(top), true) }.isFailure &&
                    runCatching { TowerBodyCatalog(emptyList(), true) }.isFailure && catalog.missingStages().isNotEmpty()),
            FixtureResult("tower-catalog-rejects-duplicate-stages-and-voxel-coordinates",
                rejects(top, top) && rejects(top.copy(blocks = top.blocks + top.blocks))),
            FixtureResult("tower-catalog-rejects-footprint-spill-and-invalid-height",
                listOf(BlockPos(2, 0, 0), BlockPos(0, -1, 0), BlockPos(0, 64, 0)).all { pos ->
                    rejects(top.copy(blocks = listOf(RelativeBodyBlock(pos, "minecraft:stone"))))
                }),
            FixtureResult("tower-catalog-rejects-unknown-id-invalid-stage-and-unresolved-path",
                rejects(top.copy(towerId = "missing")) && rejects(top.copy(level = 5)) && rejects(top.copy(path = null))),
            FixtureResult("tower-catalog-requires-provenance-and-forbids-placeholder-relabeling",
                rejects(top.copy(evidenceRefs = emptyList())) &&
                    rejects(top.copy(evidenceRefs = listOf("unverified"))) &&
                    rejects(top.copy(evidenceStatus = BodyEvidenceStatus.ENGINEERING_PLACEHOLDER)))
        )
    }
}
