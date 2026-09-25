package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.*
import dev.cubecrafttd.map.schematic.*

object FarmAdvancedPlacementFixture {
    fun run(bytes: ByteArray): List<FixtureResult> {
        val volume =
            SpongeSchematicV2Decoder().decode(bytes)
        val observation =
            SchematicMarkerClassifier(
                SchematicMarkerConfig.farm()
            ).classify(volume)
        val runtime =
            SchematicObservationRuntimeCompiler()
                .compile(
                    volume,
                    observation,
                    SchematicImportMetadata(
                        "farm_improved_2022_candidate",
                        "advanced-placement-v1",
                        MapAuthenticity
                            .COMMUNITY_IMPROVEMENT_BY_ORIGINAL_TRACK_AUTHOR,
                        productionOriginal=false
                    )
                )

        val blueRegion =
            runtime.placementRegions
                .getValue(TeamId.BLUE)
        val redRegion =
            runtime.placementRegions
                .getValue(TeamId.RED)

        val allExplicitLegal =
            runtime.explicitTowerSpots.all { spot ->
                AdvancedPlacementResolver
                    .isFootprintLegal(
                        runtime.placementRegions
                            .getValue(spot.team),
                        spot.center,
                        spot.footprint
                    )
            }

        val blueExplicit3 =
            runtime.explicitTowerSpots
                .filter {
                    it.team == TeamId.BLUE &&
                        it.footprint ==
                            FootprintSize.THREE_BY_THREE
                }
                .mapTo(linkedSetOf()) { it.center }

        val blueAdvanced3 =
            AdvancedPlacementResolver
                .allLegalCenters(
                    blueRegion,
                    FootprintSize.THREE_BY_THREE
                )
        val explicitCoverage =
            runtime.explicitTowerSpots
                .filter {
                    it.team == TeamId.BLUE &&
                        it.footprint ==
                            FootprintSize.THREE_BY_THREE
                }
                .flatMap {
                    ExplicitSpotPlacementResolver
                        .footprintCells(it)
                }
                .toSet()

        val advancedOnly =
            blueAdvanced3
                .firstOrNull {
                    it !in explicitCoverage
                }

        val advancedOffResult =
            advancedOnly?.let {
                RecommendedPlacementResolver
                    .resolve(
                        runtime,
                        PlacementRequest(
                            TeamId.BLUE,
                            it,
                            FootprintSize.THREE_BY_THREE,
                            autoCentre=false
                        )
                    )
            }

        val advancedOnResult =
            advancedOnly?.let {
                RecommendedPlacementResolver
                    .resolve(
                        runtime,
                        PlacementRequest(
                            TeamId.BLUE,
                            it,
                            FootprintSize.THREE_BY_THREE,
                            autoCentre=true
                        )
                    )
            }

        // This is one of the Red proposal-specific extra centers recovered
        // from the community improvement asset. It is part of that asset's
        // legal region, but not promoted into the mirrored explicit-core list.
        val redProposalExtra =
            BlockPos(47,8,171)
        val redExtraAdvancedLegal =
            AdvancedPlacementResolver
                .isFootprintLegal(
                    redRegion,
                    redProposalExtra,
                    FootprintSize.THREE_BY_THREE
                )
        val redExtraIsExplicitCore =
            runtime.explicitTowerSpots.any {
                it.team == TeamId.RED &&
                    it.footprint ==
                        FootprintSize.THREE_BY_THREE &&
                    it.center == redProposalExtra
            }

        return listOf(
            FixtureResult(
                "advanced-placement-region-semantics",
                blueRegion.semantics ==
                    PlacementRegionSemantics
                        .FULL_FOOTPRINT_SURFACE &&
                    redRegion.semantics ==
                        PlacementRegionSemantics
                            .FULL_FOOTPRINT_SURFACE
            ),
            FixtureResult(
                "advanced-placement-all-explicit-spots-legal",
                allExplicitLegal
            ),
            FixtureResult(
                "advanced-placement-has-non-anchor-centers",
                advancedOnly != null &&
                    blueAdvanced3.size >
                        blueExplicit3.size
            ),
            FixtureResult(
                "advanced-placement-auto-centre-off-allows-region-center",
                advancedOffResult is
                    PlacementResolution.AdvancedCenter
            ),
            FixtureResult(
                "advanced-placement-auto-centre-on-does-not-invent-snap",
                advancedOnResult is
                    PlacementResolution.Rejected
            ),
            FixtureResult(
                "advanced-placement-community-red-extra-keeps-provenance-separation",
                redExtraAdvancedLegal &&
                    !redExtraIsExplicitCore
            )
        )
    }
}
