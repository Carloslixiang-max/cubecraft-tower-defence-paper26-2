package dev.cubecrafttd.map.schematic

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.map.FootprintSize
import java.util.ArrayDeque

data class TeamMarkerConfig(
    val pathUnderlay: String,
    val towerUnderlay: String,
    val spawnMarkerAbove: String
)

data class SchematicMarkerConfig(
    val teams: Map<TeamId, TeamMarkerConfig>,
    val uniform3x3SurfaceMaterials: Set<String>,
    val checker5x5A: String,
    val checker5x5B: String,
    val centerlineMaterial: String,
    val primarySurfaceY: Set<Int> = emptySet()
) {
    companion object {
        fun farm(): SchematicMarkerConfig = SchematicMarkerConfig(
            teams = mapOf(
                TeamId.RED to TeamMarkerConfig(
                    "minecraft:red_wool",
                    "minecraft:red_terracotta",
                    "minecraft:red_stained_glass"
                ),
                TeamId.BLUE to TeamMarkerConfig(
                    "minecraft:blue_wool",
                    "minecraft:blue_terracotta",
                    "minecraft:blue_stained_glass"
                )
            ),
            uniform3x3SurfaceMaterials = setOf(
                "minecraft:oak_planks",
                "minecraft:dark_oak_planks"
            ),
            checker5x5A = "minecraft:oak_planks",
            checker5x5B = "minecraft:spruce_planks",
            centerlineMaterial = "minecraft:jungle_planks",
            primarySurfaceY = setOf(3, 8)
        )
    }
}

data class SpawnMarkerObservation(
    val team: TeamId,
    val beaconLocal: BlockPos,
    val markerAbove: String
)

data class ExplicitSpotObservation(
    val team: TeamId,
    val footprint: FootprintSize,
    val centerLocal: BlockPos,
    val surfaceDescription: String
)

data class RouteGraphObservation(
    val rawMarkerCount: Int,
    val virtualGapNodes: Set<BlockPos>,
    val allGraphNodeCount: Int,
    val componentSizes: List<Int>,
    val mainComponent: Set<BlockPos>,
    val adjacency: Map<BlockPos, List<BlockPos>>,
    val endpoints: List<BlockPos>,
    val branchNodes: List<BlockPos>,
    val simplePaths: List<List<BlockPos>>
) {
    val simplePathNodeCounts: List<Int> get() = simplePaths.map { it.size }.sorted()
}

data class TerminalCrossSectionObservation(
    val team: TeamId,
    val spawnSideEndpoint: BlockPos,
    val terminalEndpoint: BlockPos,
    val predecessor: BlockPos,
    val heading: String,
    val strip: List<BlockPos>,
    val surfaceMaterials: List<String?>,
    val outsideBorderSurfaceMaterials: List<String?>
)

data class MirrorObservation(
    val xSum: Int,
    val zSum: Int,
    val pathExact: Boolean,
    val mirroredCore3x3PerTeam: Int,
    val mirroredCore5x5PerTeam: Int,
    val redOnly3x3: Set<BlockPos>,
    val redOnly5x5: Set<BlockPos>,
    val blueOnly3x3: Set<BlockPos>,
    val blueOnly5x5: Set<BlockPos>
) {
    fun mirror(point: BlockPos): BlockPos =
        BlockPos(xSum - point.x, point.y, zSum - point.z)
}

data class TeamMarkerObservation(
    val pathUnderlayCells: Set<BlockPos>,
    val towerUnderlayCells: Set<BlockPos>,
    val centerMarkers: Set<BlockPos>,
    val routeGraph: RouteGraphObservation
)

data class SchematicMarkerObservation(
    val teams: Map<TeamId, TeamMarkerObservation>,
    val spawnMarkers: Map<TeamId, SpawnMarkerObservation>,
    val rawSpots3x3: List<ExplicitSpotObservation>,
    val rawSpots5x5: List<ExplicitSpotObservation>,
    val mirror: MirrorObservation?,
    val terminalCrossSections: Map<TeamId, TerminalCrossSectionObservation>
)

class SchematicMarkerClassifier(
    private val config: SchematicMarkerConfig
) {
    private val blockPosComparator =
        compareBy<BlockPos> { it.x }.thenBy { it.y }.thenBy { it.z }

    fun classify(volume: DecodedMapVolume): SchematicMarkerObservation {
        val pathCells = linkedMapOf<TeamId, MutableSet<BlockPos>>()
        val towerCells = linkedMapOf<TeamId, MutableSet<BlockPos>>()
        val centerMarkers = linkedMapOf<TeamId, MutableSet<BlockPos>>()
        TeamId.entries.forEach {
            pathCells[it] = linkedSetOf()
            towerCells[it] = linkedSetOf()
            centerMarkers[it] = linkedSetOf()
        }

        for (y in 0 until volume.dimensions.height) {
            for (z in 0 until volume.dimensions.length) {
                for (x in 0 until volume.dimensions.width) {
                    val material = volume.blockAt(x, y, z) ?: continue
                    config.teams.forEach { (team, teamConfig) ->
                        if (material == teamConfig.pathUnderlay) {
                            val p = BlockPos(x, y, z)
                            pathCells.getValue(team).add(p)
                            if (y + 1 < volume.dimensions.height &&
                                volume.blockAt(x, y + 1, z) == config.centerlineMaterial
                            ) {
                                centerMarkers.getValue(team).add(BlockPos(x, y + 1, z))
                            }
                        }
                        if (material == teamConfig.towerUnderlay) {
                            towerCells.getValue(team).add(BlockPos(x, y, z))
                        }
                    }
                }
            }
        }

        val spawnMarkers = detectSpawnMarkers(volume)
        val spots3 = detect3x3Spots(volume)
        val spots5 = detect5x5Spots(volume)

        val teamObservations = TeamId.entries.associateWith { team ->
            val graph = buildCenterlineGraph(
                volume,
                centerMarkers.getValue(team),
                config.centerlineMaterial
            )
            TeamMarkerObservation(
                pathUnderlayCells = pathCells.getValue(team),
                towerUnderlayCells = towerCells.getValue(team),
                centerMarkers = centerMarkers.getValue(team),
                routeGraph = graph
            )
        }

        val mirror = inferMirror(teamObservations, spawnMarkers, spots3, spots5)
        val terminals = TeamId.entries.mapNotNull { team ->
            inferTerminalCrossSection(
                volume = volume,
                team = team,
                teamConfig = config.teams.getValue(team),
                spawn = spawnMarkers[team],
                graph = teamObservations.getValue(team).routeGraph
            )?.let { team to it }
        }.toMap()

        return SchematicMarkerObservation(
            teams = teamObservations,
            spawnMarkers = spawnMarkers,
            rawSpots3x3 = spots3,
            rawSpots5x5 = spots5,
            mirror = mirror,
            terminalCrossSections = terminals
        )
    }

    private fun detectSpawnMarkers(
        volume: DecodedMapVolume
    ): Map<TeamId, SpawnMarkerObservation> {
        val result = linkedMapOf<TeamId, SpawnMarkerObservation>()
        for (y in 0 until volume.dimensions.height - 1) {
            for (z in 0 until volume.dimensions.length) {
                for (x in 0 until volume.dimensions.width) {
                    if (volume.blockAt(x, y, z) != "minecraft:beacon") continue
                    val above = volume.blockAt(x, y + 1, z) ?: continue
                    val matched = config.teams.entries.firstOrNull {
                        it.value.spawnMarkerAbove == above
                    } ?: continue
                    result[matched.key] = SpawnMarkerObservation(
                        team = matched.key,
                        beaconLocal = BlockPos(x, y, z),
                        markerAbove = above
                    )
                }
            }
        }
        return result
    }

    private fun detect3x3Spots(volume: DecodedMapVolume): List<ExplicitSpotObservation> {
        val out = mutableListOf<ExplicitSpotObservation>()
        config.teams.forEach { (team, teamConfig) ->
            for (y in 0 until volume.dimensions.height - 1) {
                for (z0 in 0 until volume.dimensions.length - 2) {
                    for (x0 in 0 until volume.dimensions.width - 2) {
                        var underOk = true
                        loop@ for (dz in 0..2) {
                            for (dx in 0..2) {
                                if (volume.blockAt(x0 + dx, y, z0 + dz) != teamConfig.towerUnderlay) {
                                    underOk = false
                                    break@loop
                                }
                            }
                        }
                        if (!underOk) continue

                        val first = volume.blockAt(x0, y + 1, z0) ?: continue
                        if (first !in config.uniform3x3SurfaceMaterials) continue
                        var topOk = true
                        loop@ for (dz in 0..2) {
                            for (dx in 0..2) {
                                if (volume.blockAt(x0 + dx, y + 1, z0 + dz) != first) {
                                    topOk = false
                                    break@loop
                                }
                            }
                        }
                        if (topOk) {
                            out += ExplicitSpotObservation(
                                team,
                                FootprintSize.THREE_BY_THREE,
                                BlockPos(x0 + 1, y + 1, z0 + 1),
                                first
                            )
                        }
                    }
                }
            }
        }
        return out
    }

    private fun detect5x5Spots(volume: DecodedMapVolume): List<ExplicitSpotObservation> {
        val out = mutableListOf<ExplicitSpotObservation>()
        config.teams.forEach { (team, teamConfig) ->
            for (y in 0 until volume.dimensions.height - 1) {
                for (z0 in 0 until volume.dimensions.length - 4) {
                    for (x0 in 0 until volume.dimensions.width - 4) {
                        var ok = true
                        loop@ for (dz in 0..4) {
                            for (dx in 0..4) {
                                if (volume.blockAt(x0 + dx, y, z0 + dz) != teamConfig.towerUnderlay) {
                                    ok = false
                                    break@loop
                                }
                                val expected = if ((dx + dz) % 2 == 0) {
                                    config.checker5x5A
                                } else {
                                    config.checker5x5B
                                }
                                if (volume.blockAt(x0 + dx, y + 1, z0 + dz) != expected) {
                                    ok = false
                                    break@loop
                                }
                            }
                        }
                        if (ok) {
                            out += ExplicitSpotObservation(
                                team,
                                FootprintSize.FIVE_BY_FIVE,
                                BlockPos(x0 + 2, y + 1, z0 + 2),
                                "${config.checker5x5A}+${config.checker5x5B}"
                            )
                        }
                    }
                }
            }
        }
        return out
    }

    private fun buildCenterlineGraph(
        volume: DecodedMapVolume,
        rawMarkers: Set<BlockPos>,
        centerMaterial: String
    ): RouteGraphObservation {
        val virtual = linkedSetOf<BlockPos>()
        rawMarkers.forEach { p ->
            listOf(2 to 0, -2 to 0, 0 to 2, 0 to -2).forEach { (dx, dz) ->
                val q = BlockPos(p.x + dx, p.y, p.z + dz)
                val mid = BlockPos(p.x + dx / 2, p.y, p.z + dz / 2)
                if (q in rawMarkers &&
                    mid !in rawMarkers &&
                    volume.blockAt(mid.x, mid.y, mid.z) == centerMaterial
                ) {
                    virtual += mid
                }
            }
        }

        val points = linkedSetOf<BlockPos>().apply {
            addAll(rawMarkers)
            addAll(virtual)
        }

        fun neighbors(p: BlockPos): List<BlockPos> {
            val candidates = mutableListOf(
                BlockPos(p.x + 1, p.y, p.z),
                BlockPos(p.x - 1, p.y, p.z),
                BlockPos(p.x, p.y, p.z + 1),
                BlockPos(p.x, p.y, p.z - 1)
            )
            for (dy in listOf(-1, 1)) {
                candidates += BlockPos(p.x + 1, p.y + dy, p.z)
                candidates += BlockPos(p.x - 1, p.y + dy, p.z)
                candidates += BlockPos(p.x, p.y + dy, p.z + 1)
                candidates += BlockPos(p.x, p.y + dy, p.z - 1)
            }
            return candidates.filter { it in points }.sortedWith(blockPosComparator)
        }

        val remaining = points.toMutableSet()
        val components = mutableListOf<Set<BlockPos>>()
        while (remaining.isNotEmpty()) {
            val seed = remaining.minWith(blockPosComparator)
            remaining.remove(seed)
            val component = linkedSetOf(seed)
            val stack = ArrayDeque<BlockPos>()
            stack.add(seed)
            while (stack.isNotEmpty()) {
                val p = stack.removeLast()
                neighbors(p).forEach { q ->
                    if (remaining.remove(q)) {
                        component += q
                        stack.add(q)
                    }
                }
            }
            components += component
        }
        components.sortByDescending { it.size }

        val main = components.firstOrNull().orEmpty()
        val adjacency = main.associateWith { p ->
            neighbors(p).filter { it in main }
        }
        val endpoints = adjacency.filterValues { it.size == 1 }.keys.sortedWith(blockPosComparator)
        val branchNodes = adjacency.filterValues { it.size > 2 }.keys.sortedWith(blockPosComparator)

        val simplePaths = mutableListOf<List<BlockPos>>()
        if (endpoints.size == 2) {
            val start = endpoints[0]
            val goal = endpoints[1]
            data class Frame(
                val node: BlockPos,
                val path: List<BlockPos>,
                val seen: Set<BlockPos>
            )
            val stack = ArrayDeque<Frame>()
            stack.add(Frame(start, listOf(start), setOf(start)))
            while (stack.isNotEmpty() && simplePaths.size < 8) {
                val frame = stack.removeLast()
                if (frame.node == goal) {
                    simplePaths += frame.path
                    continue
                }
                adjacency.getValue(frame.node).asReversed().forEach { next ->
                    if (next !in frame.seen) {
                        stack.add(
                            Frame(
                                next,
                                frame.path + next,
                                frame.seen + next
                            )
                        )
                    }
                }
            }
        }

        return RouteGraphObservation(
            rawMarkerCount = rawMarkers.size,
            virtualGapNodes = virtual,
            allGraphNodeCount = points.size,
            componentSizes = components.map { it.size },
            mainComponent = main,
            adjacency = adjacency,
            endpoints = endpoints,
            branchNodes = branchNodes,
            simplePaths = simplePaths
        )
    }

    private fun inferMirror(
        teamObservations: Map<TeamId, TeamMarkerObservation>,
        spawns: Map<TeamId, SpawnMarkerObservation>,
        spots3: List<ExplicitSpotObservation>,
        spots5: List<ExplicitSpotObservation>
    ): MirrorObservation? {
        val redSpawn = spawns[TeamId.RED]?.beaconLocal ?: return null
        val blueSpawn = spawns[TeamId.BLUE]?.beaconLocal ?: return null
        val xSum = redSpawn.x + blueSpawn.x
        val zSum = redSpawn.z + blueSpawn.z

        fun mirror(p: BlockPos) = BlockPos(xSum - p.x, p.y, zSum - p.z)

        val redPath = teamObservations.getValue(TeamId.RED).pathUnderlayCells
        val bluePath = teamObservations.getValue(TeamId.BLUE).pathUnderlayCells
        val pathExact = redPath.mapTo(linkedSetOf(), ::mirror) == bluePath

        fun centers(
            source: List<ExplicitSpotObservation>,
            team: TeamId
        ): Set<BlockPos> = source.filter { it.team == team }.mapTo(linkedSetOf()) { it.centerLocal }

        val red3 = centers(spots3, TeamId.RED)
        val blue3 = centers(spots3, TeamId.BLUE)
        val red5 = centers(spots5, TeamId.RED)
        val blue5 = centers(spots5, TeamId.BLUE)

        val red3Mirrored = red3.mapTo(linkedSetOf(), ::mirror)
        val blue3Mirrored = blue3.mapTo(linkedSetOf(), ::mirror)
        val red5Mirrored = red5.mapTo(linkedSetOf(), ::mirror)
        val blue5Mirrored = blue5.mapTo(linkedSetOf(), ::mirror)

        val blueCore3 = red3Mirrored.intersect(blue3)
        val blueCore5 = red5Mirrored.intersect(blue5)

        return MirrorObservation(
            xSum = xSum,
            zSum = zSum,
            pathExact = pathExact,
            mirroredCore3x3PerTeam = blueCore3.size,
            mirroredCore5x5PerTeam = blueCore5.size,
            redOnly3x3 = red3 - blue3Mirrored,
            redOnly5x5 = red5 - blue5Mirrored,
            blueOnly3x3 = blue3 - red3Mirrored,
            blueOnly5x5 = blue5 - red5Mirrored
        )
    }

    private fun inferTerminalCrossSection(
        volume: DecodedMapVolume,
        team: TeamId,
        teamConfig: TeamMarkerConfig,
        spawn: SpawnMarkerObservation?,
        graph: RouteGraphObservation
    ): TerminalCrossSectionObservation? {
        if (spawn == null || graph.endpoints.size != 2) return null
        val beacon = spawn.beaconLocal
        fun distanceSquared(a: BlockPos, b: BlockPos): Long {
            val dx = (a.x - b.x).toLong()
            val dy = (a.y - b.y).toLong()
            val dz = (a.z - b.z).toLong()
            return dx * dx + dy * dy + dz * dz
        }

        val spawnSide = graph.endpoints.minBy { distanceSquared(it, beacon) }
        val terminal = graph.endpoints.maxBy { distanceSquared(it, beacon) }

        var predecessor: BlockPos? = null
        graph.simplePaths.forEach { path ->
            if (predecessor != null || path.size < 2) return@forEach
            if (path.last() == terminal) predecessor = path[path.lastIndex - 1]
            else if (path.first() == terminal) predecessor = path[1]
        }
        val previous = predecessor ?: return null

        val dx = terminal.x - previous.x
        val dy = terminal.y - previous.y
        val dz = terminal.z - previous.z
        if (dy != 0 || kotlin.math.abs(dx) + kotlin.math.abs(dz) != 1) return null

        val y = terminal.y
        val underY = y - 1
        val strip: List<BlockPos>
        val borderA: BlockPos?
        val borderB: BlockPos?

        if (dz != 0) {
            var lo = terminal.x
            while (lo - 1 >= 0 &&
                volume.blockAt(lo - 1, underY, terminal.z) == teamConfig.pathUnderlay
            ) lo--
            var hi = terminal.x
            while (hi + 1 < volume.dimensions.width &&
                volume.blockAt(hi + 1, underY, terminal.z) == teamConfig.pathUnderlay
            ) hi++
            strip = (lo..hi).map { BlockPos(it, y, terminal.z) }
            borderA = if (lo - 1 >= 0) BlockPos(lo - 1, y, terminal.z) else null
            borderB = if (hi + 1 < volume.dimensions.width) BlockPos(hi + 1, y, terminal.z) else null
        } else {
            var lo = terminal.z
            while (lo - 1 >= 0 &&
                volume.blockAt(terminal.x, underY, lo - 1) == teamConfig.pathUnderlay
            ) lo--
            var hi = terminal.z
            while (hi + 1 < volume.dimensions.length &&
                volume.blockAt(terminal.x, underY, hi + 1) == teamConfig.pathUnderlay
            ) hi++
            strip = (lo..hi).map { BlockPos(terminal.x, y, it) }
            borderA = if (lo - 1 >= 0) BlockPos(terminal.x, y, lo - 1) else null
            borderB = if (hi + 1 < volume.dimensions.length) BlockPos(terminal.x, y, hi + 1) else null
        }

        val heading = when {
            dx == 1 -> "+x"
            dx == -1 -> "-x"
            dz == 1 -> "+z"
            else -> "-z"
        }

        fun surface(p: BlockPos?): String? =
            p?.let { volume.blockAt(it.x, it.y, it.z) }

        return TerminalCrossSectionObservation(
            team = team,
            spawnSideEndpoint = spawnSide,
            terminalEndpoint = terminal,
            predecessor = previous,
            heading = heading,
            strip = strip,
            surfaceMaterials = strip.map(::surface),
            outsideBorderSurfaceMaterials = listOf(surface(borderA), surface(borderB))
        )
    }
}
