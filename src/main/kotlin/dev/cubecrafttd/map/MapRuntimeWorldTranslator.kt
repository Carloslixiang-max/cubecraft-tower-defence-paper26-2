package dev.cubecrafttd.map

/**
 * Binds schematic-local runtime geometry into a real Paper world coordinate
 * space by translation only. Rotation/mirroring are intentionally not hidden
 * here; a different asset orientation must be an explicit transform later.
 */
data class MapWorldTranslation(
    val dx: Int,
    val dy: Int,
    val dz: Int
) {
    fun block(p: BlockPos): BlockPos =
        BlockPos(p.x+dx,p.y+dy,p.z+dz)

    fun vec(p: Vec3): Vec3 =
        Vec3(p.x+dx,p.y+dy,p.z+dz)
}

object MapRuntimeWorldTranslator {
    fun translate(
        source: MapRuntimeDefinition,
        translation: MapWorldTranslation
    ): MapRuntimeDefinition {
        fun route(route: RouteRuntime): RouteRuntime =
            RouteRuntime.compile(
                route.routeId,
                route.nodes.map(translation::vec)
            )

        fun headingCells(
            cells: Set<BlockPos>
        ) = cells.mapTo(linkedSetOf(),translation::block)

        return source.copy(
            coordinateSpace = CoordinateSpace(
                type = CoordinateSpaceType.RECONSTRUCTION_WORLD,
                dimensions = source.coordinateSpace.dimensions,
                sourceOffset = BlockPos(
                    translation.dx,
                    translation.dy,
                    translation.dz
                ),
                absoluteProductionCoordinatesKnown = false
            ),
            routesById = source.routesById
                .mapValues { (_,v) -> route(v) },
            placementRegions = source.placementRegions
                .mapValues { (_,region) ->
                    region.copy(
                        legalBaseBlocks = region.legalBaseBlocks
                            .mapTo(linkedSetOf(),translation::block)
                    )
                },
            explicitTowerSpots = source.explicitTowerSpots.map {
                it.copy(center=translation.block(it.center))
            },
            teamSpawns = source.teamSpawns.mapValues { (_,spawn) ->
                spawn.copy(
                    mobSpawn=spawn.mobSpawn?.let(translation::vec),
                    playerSpawn=spawn.playerSpawn?.let(translation::vec)
                )
            },
            terminalCrossSections = source.terminalCrossSections.mapValues { (_,terminal) ->
                terminal.copy(
                    centerlineEndpoint=translation.block(terminal.centerlineEndpoint),
                    cells=headingCells(terminal.cells)
                )
            },
            castleContactCandidates = source.castleContactCandidates.mapValues { (_,candidate) ->
                candidate.copy(
                    boundaryCells=headingCells(candidate.boundaryCells)
                )
            },
            castleContactRegionsExact = source.castleContactRegionsExact.mapValues { (_,exact) ->
                exact?.copy(cells=headingCells(exact.cells))
            },
            guardAnchors = source.guardAnchors.mapValues { (_,anchors) ->
                anchors.map { it.copy(position=translation.vec(it.position)) }
            }
        )
    }
}
