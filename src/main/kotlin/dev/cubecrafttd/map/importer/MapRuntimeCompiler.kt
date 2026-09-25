package dev.cubecrafttd.map.importer

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.*

object MapRuntimeCompiler {
    fun compile(candidate: MapImportCandidate): MapRuntimeDefinition {
        require(candidate.routes.map { it.routeId }.distinct().size == candidate.routes.size) {
            "Duplicate routeId in map import candidate"
        }

        val routes = candidate.routes.associate { input ->
            input.routeId to RouteRuntime.compile(input.routeId, input.waypoints)
        }
        val routeOwners = candidate.routes.associate { it.routeId to it.team }

        val spawns = TeamId.entries.associateWith { team ->
            val geometry = candidate.teamGeometry[team]
            TeamSpawnGeometry(team = team, mobSpawn = geometry?.mobSpawn)
        }

        val terminals = candidate.teamGeometry.mapNotNull { (team, geometry) ->
            geometry.terminalCrossSection?.let { team to it }
        }.toMap()

        val contactCandidates = candidate.teamGeometry.mapNotNull { (team, geometry) ->
            geometry.castleContactCandidate?.let { team to it }
        }.toMap()

        val exactRegions = TeamId.entries.associateWith { team ->
            candidate.teamGeometry[team]?.exactCastleContactRegion
        }

        val guards = TeamId.entries.associateWith { team ->
            candidate.teamGeometry[team]?.guardAnchors.orEmpty()
        }

        return MapRuntimeDefinition(
            mapId = candidate.mapId,
            revision = candidate.revision,
            authenticity = candidate.authenticity,
            productionOriginal = candidate.productionOriginal,
            coordinateSpace = candidate.coordinateSpace,
            routesById = routes,
            routeOwnerById = routeOwners,
            placementRegions = candidate.placementRegions,
            explicitTowerSpots = candidate.explicitTowerSpots,
            teamSpawns = spawns,
            terminalCrossSections = terminals,
            castleContactCandidates = contactCandidates,
            castleContactRegionsExact = exactRegions,
            guardAnchors = guards,
            sourceHash = candidate.sourceHash,
            evidenceRefs = candidate.evidenceRefs
        )
    }
}
