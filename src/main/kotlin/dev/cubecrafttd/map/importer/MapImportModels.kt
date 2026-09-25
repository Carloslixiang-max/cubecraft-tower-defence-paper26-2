package dev.cubecrafttd.map.importer

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.*

data class CandidateRoute(
    val routeId: String,
    val team: TeamId,
    val waypoints: List<Vec3>
)

data class CandidateTeamGeometry(
    val team: TeamId,
    val mobSpawn: Vec3?,
    val terminalCrossSection: RouteTerminalCrossSection?,
    val castleContactCandidate: CastleContactCandidate?,
    val exactCastleContactRegion: ExactCastleContactRegion? = null,
    val guardAnchors: List<GuardAnchor> = emptyList()
)

data class MapImportCandidate(
    val mapId: String,
    val revision: String,
    val authenticity: MapAuthenticity,
    val productionOriginal: Boolean,
    val coordinateSpace: CoordinateSpace,
    val sourceHash: String?,
    val routes: List<CandidateRoute>,
    val placementRegions: Map<TeamId, PlacementRegion>,
    val explicitTowerSpots: List<ExplicitTowerSpot>,
    val teamGeometry: Map<TeamId, CandidateTeamGeometry>,
    val evidenceRefs: List<String> = emptyList()
)
