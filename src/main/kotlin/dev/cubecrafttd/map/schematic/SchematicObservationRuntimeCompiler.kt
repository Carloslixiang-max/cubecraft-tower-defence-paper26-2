package dev.cubecrafttd.map.schematic

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.*
import dev.cubecrafttd.map.importer.*

data class SchematicImportMetadata(
    val mapId: String,
    val revision: String,
    val authenticity: MapAuthenticity,
    val productionOriginal: Boolean,
    val evidenceRefs: List<String> = emptyList()
)

object RouteWaypointCompressor {
    fun orientAndCompress(
        rawPath: List<BlockPos>,
        spawnSideEndpoint: BlockPos
    ): List<BlockPos> {
        require(rawPath.size >= 2)
        val oriented = when {
            rawPath.first() == spawnSideEndpoint -> rawPath
            rawPath.last() == spawnSideEndpoint -> rawPath.asReversed()
            else -> error("Route does not contain expected spawn-side endpoint at either end")
        }

        val result = mutableListOf(oriented.first())
        var previousDirection: Triple<Int, Int, Int>? = null

        for (index in 0 until oriented.lastIndex) {
            val a = oriented[index]
            val b = oriented[index + 1]
            val direction = Triple(
                b.x - a.x,
                b.y - a.y,
                b.z - a.z
            )
            if (previousDirection == null) {
                previousDirection = direction
            } else if (direction != previousDirection) {
                result += a
                previousDirection = direction
            }
        }
        result += oriented.last()
        return result
    }
}

class SchematicObservationRuntimeCompiler {
    fun compile(
        volume: DecodedMapVolume,
        observation: SchematicMarkerObservation,
        metadata: SchematicImportMetadata
    ): MapRuntimeDefinition {
        val mirror = observation.mirror
            ?: error("This compiler currently requires a verified two-team mirror transform")

        val candidateRoutes = mutableListOf<CandidateRoute>()
        TeamId.entries.forEach { team ->
            val graph = observation.teams.getValue(team).routeGraph
            val terminal = observation.terminalCrossSections[team]
                ?: error("No terminal observation for $team")

            val compressed = graph.simplePaths.map { path ->
                RouteWaypointCompressor.orientAndCompress(
                    path,
                    terminal.spawnSideEndpoint
                )
            }.sortedWith(routeComparator)

            compressed.forEachIndexed { index, waypoints ->
                candidateRoutes += CandidateRoute(
                    routeId = "${team.name.lowercase()}_branch_${index + 1}",
                    team = team,
                    waypoints = waypoints.map {
                        Vec3(it.x.toDouble(), it.y.toDouble(), it.z.toDouble())
                    }
                )
            }
        }

        val core3 = mirroredCoreSpots(
            observation.rawSpots3x3,
            FootprintSize.THREE_BY_THREE,
            mirror
        )
        val core5 = mirroredCoreSpots(
            observation.rawSpots5x5,
            FootprintSize.FIVE_BY_FIVE,
            mirror
        )
        val explicit = core3 + core5

        val placementRegions = TeamId.entries.associateWith { team ->
            val surfaceCells = observation.teams
                .getValue(team)
                .towerUnderlayCells
                .mapTo(linkedSetOf()) {
                    BlockPos(it.x,it.y + 1,it.z)
                }
            PlacementRegion(
                team = team,
                legalBaseBlocks = surfaceCells,
                semantics =
                    PlacementRegionSemantics
                        .FULL_FOOTPRINT_SURFACE
            )
        }

        val teamGeometry = TeamId.entries.associateWith { team ->
            val terminal = observation.terminalCrossSections.getValue(team)
            val heading = when (terminal.heading) {
                "+x" -> CardinalHeading.POS_X
                "-x" -> CardinalHeading.NEG_X
                "+z" -> CardinalHeading.POS_Z
                "-z" -> CardinalHeading.NEG_Z
                else -> error("Unsupported terminal heading ${terminal.heading}")
            }
            CandidateTeamGeometry(
                team = team,
                mobSpawn = Vec3(
                    terminal.spawnSideEndpoint.x.toDouble(),
                    terminal.spawnSideEndpoint.y.toDouble(),
                    terminal.spawnSideEndpoint.z.toDouble()
                ),
                terminalCrossSection = RouteTerminalCrossSection(
                    team = team,
                    centerlineEndpoint = terminal.terminalEndpoint,
                    headingIntoCastle = heading,
                    widthBlocks = terminal.strip.size,
                    cells = terminal.strip.toSet(),
                    evidenceStatus = "MEASURED_FROM_SCHEMATIC"
                ),
                castleContactCandidate = CastleContactCandidate(
                    team = team,
                    boundaryCells = terminal.strip.toSet(),
                    headingIntoCastle = heading,
                    status = "INFERRED_SCHEMATIC_BOUNDARY_CANDIDATE",
                    originalTriggerRule = null
                ),
                exactCastleContactRegion = null,
                guardAnchors = emptyList()
            )
        }

        val candidate = MapImportCandidate(
            mapId = metadata.mapId,
            revision = metadata.revision,
            authenticity = metadata.authenticity,
            productionOriginal = metadata.productionOriginal,
            coordinateSpace = CoordinateSpace(
                type = CoordinateSpaceType.SCHEMATIC_LOCAL,
                dimensions = Triple(
                    volume.dimensions.width,
                    volume.dimensions.height,
                    volume.dimensions.length
                ),
                sourceOffset = BlockPos(
                    volume.offset.x,
                    volume.offset.y,
                    volume.offset.z
                ),
                absoluteProductionCoordinatesKnown = false
            ),
            sourceHash = volume.sha256,
            routes = candidateRoutes,
            placementRegions = placementRegions,
            explicitTowerSpots = explicit,
            teamGeometry = teamGeometry,
            evidenceRefs = metadata.evidenceRefs
        )

        return MapRuntimeCompiler.compile(candidate)
    }

    private fun mirroredCoreSpots(
        raw: List<ExplicitSpotObservation>,
        footprint: FootprintSize,
        mirror: MirrorObservation
    ): List<ExplicitTowerSpot> {
        val red = raw.filter { it.team == TeamId.RED }
            .mapTo(linkedSetOf()) { it.centerLocal }
        val blue = raw.filter { it.team == TeamId.BLUE }
            .mapTo(linkedSetOf()) { it.centerLocal }

        val redCore = red.filterTo(linkedSetOf()) { center ->
            mirror.mirror(center) in blue
        }
        val blueCore = blue.filterTo(linkedSetOf()) { center ->
            mirror.mirror(center) in red
        }

        check(redCore.size == blueCore.size) {
            "Mirrored core mismatch for $footprint: red=${redCore.size}, blue=${blueCore.size}"
        }

        return buildList {
            redCore.sortedWith(blockComparator).forEach {
                add(ExplicitTowerSpot(TeamId.RED, it, footprint))
            }
            blueCore.sortedWith(blockComparator).forEach {
                add(ExplicitTowerSpot(TeamId.BLUE, it, footprint))
            }
        }
    }

    companion object {
        private val blockComparator =
            compareBy<BlockPos> { it.x }.thenBy { it.y }.thenBy { it.z }

        private val routeComparator = Comparator<List<BlockPos>> { a, b ->
            val limit = minOf(a.size, b.size)
            for (i in 0 until limit) {
                val pa = a[i]
                val pb = b[i]
                val cmpX = pa.x.compareTo(pb.x)
                if (cmpX != 0) return@Comparator cmpX
                val cmpY = pa.y.compareTo(pb.y)
                if (cmpY != 0) return@Comparator cmpY
                val cmpZ = pa.z.compareTo(pb.z)
                if (cmpZ != 0) return@Comparator cmpZ
            }
            a.size.compareTo(b.size)
        }
    }
}
