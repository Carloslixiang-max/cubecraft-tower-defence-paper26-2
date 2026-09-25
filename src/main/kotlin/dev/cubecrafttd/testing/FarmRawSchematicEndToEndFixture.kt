package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.*
import dev.cubecrafttd.map.schematic.*
import kotlin.math.abs

object FarmRawSchematicEndToEndFixture {
    private val expectedBlueLow = listOf(
        BlockPos(98,8,116), BlockPos(98,8,99), BlockPos(90,8,99),
        BlockPos(90,8,92), BlockPos(88,8,92), BlockPos(88,8,68),
        BlockPos(90,8,68), BlockPos(90,8,61), BlockPos(98,8,61),
        BlockPos(98,8,40), BlockPos(110,8,40), BlockPos(110,8,16),
        BlockPos(86,8,16), BlockPos(86,8,28), BlockPos(56,8,28),
        BlockPos(56,8,13), BlockPos(52,8,13), BlockPos(51,7,13),
        BlockPos(50,7,13), BlockPos(49,6,13), BlockPos(48,6,13),
        BlockPos(47,5,13), BlockPos(46,5,13), BlockPos(45,4,13),
        BlockPos(44,4,13), BlockPos(43,3,13), BlockPos(38,3,13),
        BlockPos(38,3,45), BlockPos(62,3,45), BlockPos(62,3,65)
    )

    private val expectedBlueHigh = listOf(
        BlockPos(98,8,116), BlockPos(98,8,99), BlockPos(106,8,99),
        BlockPos(106,8,92), BlockPos(108,8,92), BlockPos(108,8,68),
        BlockPos(106,8,68), BlockPos(106,8,61), BlockPos(98,8,61),
        BlockPos(98,8,40), BlockPos(110,8,40), BlockPos(110,8,16),
        BlockPos(86,8,16), BlockPos(86,8,28), BlockPos(56,8,28),
        BlockPos(56,8,13), BlockPos(52,8,13), BlockPos(51,7,13),
        BlockPos(50,7,13), BlockPos(49,6,13), BlockPos(48,6,13),
        BlockPos(47,5,13), BlockPos(46,5,13), BlockPos(45,4,13),
        BlockPos(44,4,13), BlockPos(43,3,13), BlockPos(38,3,13),
        BlockPos(38,3,45), BlockPos(62,3,45), BlockPos(62,3,65)
    )

    fun run(bytes: ByteArray): List<FixtureResult> {
        val volume = SpongeSchematicV2Decoder().decode(bytes)
        val observation = SchematicMarkerClassifier(
            SchematicMarkerConfig.farm()
        ).classify(volume)

        val blueTerminal = observation.terminalCrossSections.getValue(TeamId.BLUE)
        val compressedBlue = observation.teams.getValue(TeamId.BLUE)
            .routeGraph.simplePaths
            .map { RouteWaypointCompressor.orientAndCompress(it, blueTerminal.spawnSideEndpoint) }
            .toSet()

        val runtime = SchematicObservationRuntimeCompiler().compile(
            volume,
            observation,
            SchematicImportMetadata(
                mapId = "farm_improved_2022_candidate",
                revision = "raw-schematic-e2e-v1",
                authenticity = MapAuthenticity.COMMUNITY_IMPROVEMENT_BY_ORIGINAL_TRACK_AUTHOR,
                productionOriginal = false,
                evidenceRefs = listOf("ImprovedFarm.schem")
            )
        )

        val red3 = runtime.explicitTowerSpots.count {
            it.team == TeamId.RED && it.footprint == FootprintSize.THREE_BY_THREE
        }
        val blue3 = runtime.explicitTowerSpots.count {
            it.team == TeamId.BLUE && it.footprint == FootprintSize.THREE_BY_THREE
        }
        val red5 = runtime.explicitTowerSpots.count {
            it.team == TeamId.RED && it.footprint == FootprintSize.FIVE_BY_FIVE
        }
        val blue5 = runtime.explicitTowerSpots.count {
            it.team == TeamId.BLUE && it.footprint == FootprintSize.FIVE_BY_FIVE
        }

        val redLengths = runtime.routesById
            .filterKeys { it.startsWith("red_") }
            .values.map { it.totalLength }
        val blueLengths = runtime.routesById
            .filterKeys { it.startsWith("blue_") }
            .values.map { it.totalLength }

        val redGraph = observation.teams.getValue(TeamId.RED).routeGraph
        val blueGraph = observation.teams.getValue(TeamId.BLUE).routeGraph

        fun splitBranchNodeCounts(graph: RouteGraphObservation): List<Int> {
            check(graph.branchNodes.size == 2)
            val a = graph.branchNodes[0]
            val b = graph.branchNodes[1]
            return graph.simplePaths.map { path ->
                val ia = path.indexOf(a)
                val ib = path.indexOf(b)
                if (ia < 0 || ib < 0) 0 else kotlin.math.abs(ia - ib) + 1
            }.sorted()
        }

        return listOf(
            FixtureResult(
                "raw-e2e-blue-waypoint-compression-exact",
                compressedBlue == setOf(expectedBlueLow, expectedBlueHigh)
            ),
            FixtureResult(
                "raw-e2e-four-runtime-routes",
                runtime.routesById.size == 4 &&
                    runtime.routesById.values.all { it.nodes.size == 30 }
            ),
            FixtureResult(
                "raw-e2e-mirrored-core-spots",
                red3 == 188 && blue3 == 188 && red5 == 8 && blue5 == 8
            ),
            FixtureResult(
                "raw-e2e-branch-segment-node-count",
                splitBranchNodeCounts(redGraph) == listOf(59, 59) &&
                    splitBranchNodeCounts(blueGraph) == listOf(59, 59)
            ),
            FixtureResult(
                "raw-e2e-equal-split-route-lengths",
                abs(redLengths[0] - redLengths[1]) < 1e-9 &&
                    abs(blueLengths[0] - blueLengths[1]) < 1e-9
            ),
            FixtureResult(
                "raw-e2e-runtime-truth-guards",
                !runtime.productionOriginal &&
                    runtime.castleContactRegionsExact.values.all { it == null } &&
                    runtime.guardAnchors.values.all { it.isEmpty() }
            ),
            FixtureResult(
                "raw-e2e-runtime-source-hash",
                runtime.sourceHash ==
                    "28d24136afe80358b89556d0fbe3b08d00e518af38c7c518819fa7fc225e613e"
            )
        )
    }
}
