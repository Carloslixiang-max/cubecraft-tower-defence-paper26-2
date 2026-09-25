package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.*

object MapWorldTranslationFixture {
    fun run(): List<FixtureResult> {
        val source=TestingMapFactory.minimal()
        val moved=MapRuntimeWorldTranslator.translate(
            source,
            MapWorldTranslation(100,64,-50)
        )
        val redSource=source.routesById.getValue("red-route")
        val redMoved=moved.routesById.getValue("red-route")
        val originalDistance=redSource.totalLength
        val movedDistance=redMoved.totalLength
        val sourceSpot=source.explicitTowerSpots.first()
        val movedSpot=moved.explicitTowerSpots.first()

        return listOf(
            FixtureResult(
                "map-world-translation-route-nodes",
                redMoved.nodes.first()==Vec3(100.0,64.0,-50.0) &&
                    redMoved.nodes.last()==Vec3(110.0,64.0,-50.0)
            ),
            FixtureResult(
                "map-world-translation-preserves-route-length",
                originalDistance==movedDistance
            ),
            FixtureResult(
                "map-world-translation-placement",
                movedSpot.center==BlockPos(
                    sourceSpot.center.x+100,
                    sourceSpot.center.y+64,
                    sourceSpot.center.z-50
                )
            ),
            FixtureResult(
                "map-world-translation-coordinate-space-tag",
                moved.coordinateSpace.type==CoordinateSpaceType.RECONSTRUCTION_WORLD &&
                    moved.coordinateSpace.sourceOffset==BlockPos(100,64,-50) &&
                    !moved.coordinateSpace.absoluteProductionCoordinatesKnown
            ),
            FixtureResult(
                "map-world-translation-source-truth-preserved",
                moved.authenticity==source.authenticity &&
                    moved.productionOriginal==source.productionOriginal &&
                    moved.sourceHash==source.sourceHash
            )
        )
    }
}
