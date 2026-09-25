package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.*
import dev.cubecrafttd.map.importer.*

object FarmMapImportFixture {
    const val EXPECTED_SHA256 = "28d24136afe80358b89556d0fbe3b08d00e518af38c7c518819fa7fc225e613e"

    fun candidate(): MapImportCandidate {
        val redTerminalCells = (59..65).map { BlockPos(it, 3, 122) }.toSet()
        val blueTerminalCells = (59..65).map { BlockPos(it, 3, 65) }.toSet()

        val redRoute = listOf(
            Vec3(26.0, 8.0, 71.0),
            Vec3(26.0, 8.0, 90.0),
            Vec3(62.0, 3.0, 123.0),
            Vec3(62.0, 3.0, 122.0)
        )
        val blueRoute = listOf(
            Vec3(98.0, 8.0, 116.0),
            Vec3(98.0, 8.0, 99.0),
            Vec3(62.0, 3.0, 64.0),
            Vec3(62.0, 3.0, 65.0)
        )

        // This fixture intentionally uses reduced waypoint lists for core contract tests.
        // Full 308-node/30-waypoint branch data remains in Farm runtime candidate research files.
        return MapImportCandidate(
            mapId = "farm_improved_proposal",
            revision = "candidate-v2-contract-fixture",
            authenticity = MapAuthenticity.COMMUNITY_IMPROVEMENT_BY_ORIGINAL_TRACK_AUTHOR,
            productionOriginal = false,
            coordinateSpace = CoordinateSpace(
                type = CoordinateSpaceType.SCHEMATIC_LOCAL,
                dimensions = Triple(125, 18, 191),
                sourceOffset = BlockPos(-61, 54, -95),
                absoluteProductionCoordinatesKnown = false
            ),
            sourceHash = EXPECTED_SHA256,
            routes = listOf(
                CandidateRoute("red_fixture_branch", TeamId.RED, redRoute),
                CandidateRoute("blue_fixture_branch", TeamId.BLUE, blueRoute)
            ),
            placementRegions = mapOf(
                TeamId.RED to PlacementRegion(TeamId.RED, setOf(BlockPos(26, 8, 70))),
                TeamId.BLUE to PlacementRegion(TeamId.BLUE, setOf(BlockPos(98, 8, 117)))
            ),
            explicitTowerSpots = emptyList(),
            teamGeometry = mapOf(
                TeamId.RED to CandidateTeamGeometry(
                    team = TeamId.RED,
                    mobSpawn = Vec3(26.0, 8.0, 71.0),
                    terminalCrossSection = RouteTerminalCrossSection(
                        TeamId.RED, BlockPos(62, 3, 122), CardinalHeading.NEG_Z, 7,
                        redTerminalCells, "MEASURED_FROM_SCHEMATIC"
                    ),
                    castleContactCandidate = CastleContactCandidate(
                        TeamId.RED, redTerminalCells, CardinalHeading.NEG_Z,
                        "INFERRED_SCHEMATIC_BOUNDARY_CANDIDATE", originalTriggerRule = null
                    )
                ),
                TeamId.BLUE to CandidateTeamGeometry(
                    team = TeamId.BLUE,
                    mobSpawn = Vec3(98.0, 8.0, 116.0),
                    terminalCrossSection = RouteTerminalCrossSection(
                        TeamId.BLUE, BlockPos(62, 3, 65), CardinalHeading.POS_Z, 7,
                        blueTerminalCells, "MEASURED_FROM_SCHEMATIC"
                    ),
                    castleContactCandidate = CastleContactCandidate(
                        TeamId.BLUE, blueTerminalCells, CardinalHeading.POS_Z,
                        "INFERRED_SCHEMATIC_BOUNDARY_CANDIDATE", originalTriggerRule = null
                    )
                )
            ),
            evidenceRefs = listOf(
                "CubeCraft_TD_Farm_integrity_fixture_v1.json",
                "CubeCraft_TD_Farm_runtime_candidate_v2.json"
            )
        )
    }

    fun run(): FixtureResult {
        val runtime = MapRuntimeCompiler.compile(candidate())
        val report = MapRuntimeValidator.validate(runtime)

        val assertions = listOf(
            runtime.sourceHash == EXPECTED_SHA256,
            runtime.coordinateSpace.dimensions == Triple(125, 18, 191),
            runtime.productionOriginal == false,
            runtime.castleContactRegionsExact.values.all { it == null },
            runtime.guardAnchors.values.all { it.isEmpty() },
            runtime.terminalCrossSections[TeamId.RED]?.widthBlocks == 7,
            runtime.terminalCrossSections[TeamId.BLUE]?.widthBlocks == 7,
            runtime.castleContactCandidates[TeamId.RED]?.originalTriggerRule == null,
            runtime.castleContactCandidates[TeamId.BLUE]?.originalTriggerRule == null,
            report.isValid
        )

        return FixtureResult(
            id = "farm-map-import-contract",
            passed = assertions.all { it },
            details = report.issues.map { "${it.code}: fatal=${it.fatal}: ${it.message}" }
        )
    }
}
