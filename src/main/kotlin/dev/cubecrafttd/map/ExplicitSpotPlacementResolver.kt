package dev.cubecrafttd.map

import dev.cubecrafttd.arena.TeamId

data class PlacementRequest(
    val team: TeamId,
    val clickedBlock: BlockPos,
    val footprint: FootprintSize,
    val autoCentre: Boolean
)

enum class PlacementResolutionSource {
    EXPLICIT_ANCHOR,
    ADVANCED_REGION
}

sealed interface PlacementResolution {
    data class ExplicitSpot(val spot: ExplicitTowerSpot) :
        PlacementResolution

    data class AdvancedCenter(
        val center: BlockPos
    ) : PlacementResolution

    data class Rejected(val reason: String) :
        PlacementResolution

    fun centerOrNull(): BlockPos? = when (this) {
        is ExplicitSpot -> spot.center
        is AdvancedCenter -> center
        is Rejected -> null
    }
}

fun interface TowerPlacementResolver {
    fun resolve(
        map: MapRuntimeDefinition,
        request: PlacementRequest
    ): PlacementResolution
}

object ExplicitSpotPlacementResolver : TowerPlacementResolver {
    override fun resolve(
        map: MapRuntimeDefinition,
        request: PlacementRequest
    ): PlacementResolution {
        val candidates = map.explicitTowerSpots.asSequence()
            .filter {
                it.team == request.team &&
                    it.footprint == request.footprint
            }
            .filter { spot ->
                if (request.autoCentre) {
                    request.clickedBlock in footprintCells(spot)
                } else {
                    request.clickedBlock == spot.center
                }
            }
            .toList()

        if (candidates.isEmpty()) {
            return PlacementResolution.Rejected(
                "No explicit ${request.footprint} spot contains requested block"
            )
        }
        if (candidates.size > 1) {
            return PlacementResolution.Rejected(
                "Ambiguous explicit spot overlap; map import must be reconciled"
            )
        }
        return PlacementResolution.ExplicitSpot(
            candidates.single()
        )
    }

    fun footprintCells(
        spot: ExplicitTowerSpot
    ): Set<BlockPos> =
        AdvancedPlacementResolver.footprintCells(
            spot.center,
            spot.footprint
        )
}

object AdvancedPlacementResolver : TowerPlacementResolver {
    override fun resolve(
        map: MapRuntimeDefinition,
        request: PlacementRequest
    ): PlacementResolution {
        val region = map.placementRegions[request.team]
            ?: return PlacementResolution.Rejected(
                "Missing placement region for ${request.team}"
            )
        if (region.semantics !=
            PlacementRegionSemantics.FULL_FOOTPRINT_SURFACE
        ) {
            return PlacementResolution.Rejected(
                "Map placement region is not a full footprint surface mask"
            )
        }

        return if (
            isFootprintLegal(
                region,
                request.clickedBlock,
                request.footprint
            )
        ) {
            PlacementResolution.AdvancedCenter(
                request.clickedBlock
            )
        } else {
            PlacementResolution.Rejected(
                "Entire ${request.footprint} footprint is not inside legal tower region"
            )
        }
    }

    fun isFootprintLegal(
        region: PlacementRegion,
        center: BlockPos,
        footprint: FootprintSize
    ): Boolean {
        if (region.semantics !=
            PlacementRegionSemantics.FULL_FOOTPRINT_SURFACE
        ) return false
        return footprintCells(center,footprint)
            .all { it in region.legalBaseBlocks }
    }

    fun footprintCells(
        center: BlockPos,
        footprint: FootprintSize
    ): Set<BlockPos> {
        val radius = (footprint.width - 1) / 2
        return buildSet {
            for (
                x in center.x - radius..
                    center.x + radius
            ) {
                for (
                    z in center.z - radius..
                        center.z + radius
                ) {
                    add(
                        BlockPos(
                            x,center.y,z
                        )
                    )
                }
            }
        }
    }

    fun allLegalCenters(
        region: PlacementRegion,
        footprint: FootprintSize
    ): Set<BlockPos> {
        if (region.semantics !=
            PlacementRegionSemantics.FULL_FOOTPRINT_SURFACE
        ) return emptySet()

        return region.legalBaseBlocks
            .asSequence()
            .filter {
                isFootprintLegal(
                    region,it,footprint
                )
            }
            .toCollection(linkedSetOf())
    }
}

/**
 * 2021 auto-centre ON:
 * use evidence-backed suggested anchors only.
 *
 * auto-centre OFF:
 * use the full placement region footprint rule.
 *
 * This deliberately does not invent a nearest-anchor snap radius.
 */
object RecommendedPlacementResolver :
    TowerPlacementResolver {
    override fun resolve(
        map: MapRuntimeDefinition,
        request: PlacementRequest
    ): PlacementResolution =
        if (request.autoCentre) {
            ExplicitSpotPlacementResolver.resolve(
                map,request
            )
        } else {
            val region =
                map.placementRegions[request.team]
            if (
                region?.semantics ==
                    PlacementRegionSemantics
                        .FULL_FOOTPRINT_SURFACE
            ) {
                AdvancedPlacementResolver.resolve(
                    map,request
                )
            } else {
                // Legacy fixtures/maps that only know centers retain
                // exact-center placement instead of being misread as
                // a full placement mask.
                ExplicitSpotPlacementResolver.resolve(
                    map,request
                )
            }
        }
}
