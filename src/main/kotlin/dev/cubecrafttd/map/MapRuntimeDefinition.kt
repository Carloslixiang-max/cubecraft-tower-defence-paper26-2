package dev.cubecrafttd.map

import dev.cubecrafttd.arena.TeamId

data class BlockPos(val x: Int, val y: Int, val z: Int)

enum class MapAuthenticity {
    ORIGINAL,
    OFFICIAL_SCREENSHOT_RECONSTRUCTION,
    COMMUNITY_IMPROVEMENT_BY_ORIGINAL_TRACK_AUTHOR,
    COMMUNITY_RECONSTRUCTION,
    CUSTOM
}

enum class CoordinateSpaceType {
    ORIGINAL_WORLD,
    SCHEMATIC_LOCAL,
    SCHEMATIC_RELATIVE,
    RECONSTRUCTION_WORLD
}

data class CoordinateSpace(
    val type: CoordinateSpaceType,
    val dimensions: Triple<Int, Int, Int>? = null,
    val sourceOffset: BlockPos? = null,
    val absoluteProductionCoordinatesKnown: Boolean = false
)

enum class PlacementRegionSemantics {
    LEGACY_CENTER_ONLY,
    FULL_FOOTPRINT_SURFACE
}

data class PlacementRegion(
    val team: TeamId,
    val legalBaseBlocks: Set<BlockPos>,
    val semantics: PlacementRegionSemantics =
        PlacementRegionSemantics.LEGACY_CENTER_ONLY
)

enum class FootprintSize(val width: Int) {
    THREE_BY_THREE(3),
    FIVE_BY_FIVE(5)
}

data class ExplicitTowerSpot(
    val team: TeamId,
    val center: BlockPos,
    val footprint: FootprintSize
)

data class TeamSpawnGeometry(
    val team: TeamId,
    val mobSpawn: Vec3?,
    val playerSpawn: Vec3? = null
)

enum class CardinalHeading { POS_X, NEG_X, POS_Z, NEG_Z }

data class RouteTerminalCrossSection(
    val team: TeamId,
    val centerlineEndpoint: BlockPos,
    val headingIntoCastle: CardinalHeading,
    val widthBlocks: Int,
    val cells: Set<BlockPos>,
    val evidenceStatus: String
)

data class CastleContactCandidate(
    val team: TeamId,
    val boundaryCells: Set<BlockPos>,
    val headingIntoCastle: CardinalHeading,
    val status: String,
    val originalTriggerRule: String? = null
)

data class ExactCastleContactRegion(
    val team: TeamId,
    val cells: Set<BlockPos>,
    val evidenceRefs: List<String>
)

data class GuardAnchor(
    val team: TeamId,
    val position: Vec3,
    val evidenceRefs: List<String>
)

data class MapRuntimeDefinition(
    val mapId: String,
    val revision: String,
    val authenticity: MapAuthenticity,
    val productionOriginal: Boolean,
    val coordinateSpace: CoordinateSpace,
    val routesById: Map<String, RouteRuntime>,
    val routeOwnerById: Map<String, TeamId>,
    val placementRegions: Map<TeamId, PlacementRegion>,
    val explicitTowerSpots: List<ExplicitTowerSpot>,
    val teamSpawns: Map<TeamId, TeamSpawnGeometry>,
    val terminalCrossSections: Map<TeamId, RouteTerminalCrossSection>,
    val castleContactCandidates: Map<TeamId, CastleContactCandidate>,
    val castleContactRegionsExact: Map<TeamId, ExactCastleContactRegion?> = emptyMap(),
    val guardAnchors: Map<TeamId, List<GuardAnchor>> = emptyMap(),
    val sourceHash: String?,
    val evidenceRefs: List<String> = emptyList()
)
