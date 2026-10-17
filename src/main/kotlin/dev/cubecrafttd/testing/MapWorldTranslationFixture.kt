package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.arena.ArenaSpatialEnvelopeResolver
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
        val withScenery = source.copy(sceneryReservationPoints = listOf(
            Vec3(-60.0,-3.0,-45.0), Vec3(184.0,78.0,239.0)))
        val movedScenery = MapRuntimeWorldTranslator.translate(withScenery, MapWorldTranslation(100,64,-50))
        val sceneryEnvelope = ArenaSpatialEnvelopeResolver.fromMap(withScenery, 0.0)

        return listOf(
            FixtureResult("map-world-translation-includes-scenery-reservation-extents",
                movedScenery.sceneryReservationPoints == listOf(Vec3(40.0,61.0,-95.0),Vec3(284.0,142.0,189.0)) &&
                    withScenery.sceneryReservationPoints.first() == Vec3(-60.0,-3.0,-45.0)),
            FixtureResult("map-spatial-reservation-contains-full-scenery-beyond-track",
                sceneryEnvelope.minX <= -60.0 && sceneryEnvelope.minY <= -3.0 && sceneryEnvelope.minZ <= -45.0 &&
                    sceneryEnvelope.maxX >= 184.0 && sceneryEnvelope.maxY >= 78.0 && sceneryEnvelope.maxZ >= 239.0),
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
