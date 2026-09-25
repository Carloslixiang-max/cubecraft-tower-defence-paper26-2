package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.map.schematic.*

object FarmSchematicClassifierFixture {
    fun run(bytes: ByteArray): List<FixtureResult> {
        val volume = SpongeSchematicV2Decoder().decode(bytes)
        val observation = SchematicMarkerClassifier(
            SchematicMarkerConfig.farm()
        ).classify(volume)

        val red = observation.teams.getValue(TeamId.RED)
        val blue = observation.teams.getValue(TeamId.BLUE)
        val mirror = observation.mirror
        val redTerminal = observation.terminalCrossSections[TeamId.RED]
        val blueTerminal = observation.terminalCrossSections[TeamId.BLUE]

        val red3 = observation.rawSpots3x3.count { it.team == TeamId.RED }
        val blue3 = observation.rawSpots3x3.count { it.team == TeamId.BLUE }
        val red5 = observation.rawSpots5x5.count { it.team == TeamId.RED }
        val blue5 = observation.rawSpots5x5.count { it.team == TeamId.BLUE }

        val expectedTerminalSurface = listOf(
            "minecraft:birch_planks",
            "minecraft:birch_planks",
            "minecraft:birch_planks",
            "minecraft:jungle_planks",
            "minecraft:birch_planks",
            "minecraft:birch_planks",
            "minecraft:birch_planks"
        )

        return listOf(
            FixtureResult(
                "classifier-spawn-markers",
                observation.spawnMarkers[TeamId.RED]?.beaconLocal == BlockPos(26, 7, 70) &&
                    observation.spawnMarkers[TeamId.BLUE]?.beaconLocal == BlockPos(98, 7, 117)
            ),
            FixtureResult(
                "classifier-path-underlay-counts",
                red.pathUnderlayCells.size == 2535 && blue.pathUnderlayCells.size == 2535
            ),
            FixtureResult(
                "classifier-tower-underlay-counts",
                red.towerUnderlayCells.size == 2244 && blue.towerUnderlayCells.size == 2217
            ),
            FixtureResult(
                "classifier-center-marker-counts",
                red.centerMarkers.size == 367 && blue.centerMarkers.size == 367
            ),
            FixtureResult(
                "classifier-raw-explicit-spot-counts",
                red3 == 191 && blue3 == 188 && red5 == 9 && blue5 == 8
            ),
            FixtureResult(
                "classifier-mirror-transform",
                mirror?.xSum == 124 && mirror.zSum == 187 && mirror.pathExact
            ),
            FixtureResult(
                "classifier-mirrored-core-counts",
                mirror?.mirroredCore3x3PerTeam == 188 &&
                    mirror.mirroredCore5x5PerTeam == 8
            ),
            FixtureResult(
                "classifier-red-asymmetric-extras",
                mirror?.redOnly3x3 == setOf(
                    BlockPos(47, 8, 171),
                    BlockPos(47, 8, 174),
                    BlockPos(47, 8, 177)
                ) &&
                    mirror.redOnly5x5 == setOf(BlockPos(21, 8, 164)) &&
                    mirror.blueOnly3x3.isEmpty() &&
                    mirror.blueOnly5x5.isEmpty()
            ),
            FixtureResult(
                "classifier-red-route-graph-shape",
                red.routeGraph.rawMarkerCount == 367 &&
                    red.routeGraph.virtualGapNodes == setOf(BlockPos(29, 8, 126)) &&
                    red.routeGraph.allGraphNodeCount == 368 &&
                    red.routeGraph.componentSizes.take(2) == listOf(365, 3) &&
                    red.routeGraph.mainComponent.size == 365 &&
                    red.routeGraph.endpoints == listOf(
                        BlockPos(26, 8, 71),
                        BlockPos(62, 3, 122)
                    ) &&
                    red.routeGraph.branchNodes == listOf(
                        BlockPos(26, 8, 88),
                        BlockPos(26, 8, 126)
                    ) &&
                    red.routeGraph.simplePathNodeCounts == listOf(308, 308)
            ),
            FixtureResult(
                "classifier-blue-route-graph-shape",
                blue.routeGraph.rawMarkerCount == 367 &&
                    blue.routeGraph.virtualGapNodes == setOf(BlockPos(95, 8, 61)) &&
                    blue.routeGraph.allGraphNodeCount == 368 &&
                    blue.routeGraph.componentSizes.take(2) == listOf(365, 3) &&
                    blue.routeGraph.mainComponent.size == 365 &&
                    blue.routeGraph.endpoints == listOf(
                        BlockPos(62, 3, 65),
                        BlockPos(98, 8, 116)
                    ) &&
                    blue.routeGraph.branchNodes == listOf(
                        BlockPos(98, 8, 61),
                        BlockPos(98, 8, 99)
                    ) &&
                    blue.routeGraph.simplePathNodeCounts == listOf(308, 308)
            ),
            FixtureResult(
                "classifier-red-terminal",
                redTerminal?.terminalEndpoint == BlockPos(62, 3, 122) &&
                    redTerminal.predecessor == BlockPos(62, 3, 123) &&
                    redTerminal.heading == "-z" &&
                    redTerminal.strip.size == 7 &&
                    redTerminal.surfaceMaterials == expectedTerminalSurface &&
                    redTerminal.outsideBorderSurfaceMaterials ==
                        listOf("minecraft:polished_andesite", "minecraft:polished_andesite")
            ),
            FixtureResult(
                "classifier-blue-terminal",
                blueTerminal?.terminalEndpoint == BlockPos(62, 3, 65) &&
                    blueTerminal.predecessor == BlockPos(62, 3, 64) &&
                    blueTerminal.heading == "+z" &&
                    blueTerminal.strip.size == 7 &&
                    blueTerminal.surfaceMaterials == expectedTerminalSurface &&
                    blueTerminal.outsideBorderSurfaceMaterials ==
                        listOf("minecraft:polished_andesite", "minecraft:polished_andesite")
            )
        )
    }
}
