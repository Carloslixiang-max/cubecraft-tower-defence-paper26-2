package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.*

object TestingMapFactory {
    fun minimal(): MapRuntimeDefinition {
        val redRoute = RouteRuntime.compile(
            "red-route",
            listOf(
                Vec3(0.0,0.0,0.0),
                Vec3(10.0,0.0,0.0)
            )
        )
        val blueRoute = RouteRuntime.compile(
            "blue-route",
            listOf(
                Vec3(0.0,0.0,10.0),
                Vec3(10.0,0.0,10.0)
            )
        )
        return MapRuntimeDefinition(
            mapId = "fixture",
            revision = "1",
            authenticity =
                MapAuthenticity.CUSTOM,
            productionOriginal = false,
            coordinateSpace = CoordinateSpace(
                CoordinateSpaceType.RECONSTRUCTION_WORLD
            ),
            routesById = mapOf(
                redRoute.routeId to redRoute,
                blueRoute.routeId to blueRoute
            ),
            routeOwnerById = mapOf(
                redRoute.routeId to TeamId.RED,
                blueRoute.routeId to TeamId.BLUE
            ),
            placementRegions = mapOf(
                TeamId.RED to PlacementRegion(
                    TeamId.RED,
                    setOf(BlockPos(0,0,2))
                ),
                TeamId.BLUE to PlacementRegion(
                    TeamId.BLUE,
                    setOf(BlockPos(0,0,12))
                )
            ),
            explicitTowerSpots = listOf(
                ExplicitTowerSpot(
                    TeamId.RED,
                    BlockPos(0,0,2),
                    FootprintSize.THREE_BY_THREE
                ),
                ExplicitTowerSpot(
                    TeamId.BLUE,
                    BlockPos(0,0,12),
                    FootprintSize.THREE_BY_THREE
                )
            ),
            teamSpawns = mapOf(
                TeamId.RED to TeamSpawnGeometry(
                    TeamId.RED,Vec3(0.0,0.0,0.0)
                ),
                TeamId.BLUE to TeamSpawnGeometry(
                    TeamId.BLUE,Vec3(0.0,0.0,10.0)
                )
            ),
            terminalCrossSections = emptyMap(),
            castleContactCandidates = emptyMap(),
            sourceHash = null
        )
    }
}
