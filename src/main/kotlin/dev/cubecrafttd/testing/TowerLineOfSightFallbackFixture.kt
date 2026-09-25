package dev.cubecrafttd.testing

import dev.cubecrafttd.tower.RecommendedMatureTowerDefinitions
import dev.cubecrafttd.truth.*

object TowerLineOfSightFallbackFixture {
    fun run():List<FixtureResult> {
        val empty=
            GameplayFallbackCompletenessValidator
                .validate(
                    RuntimeFallbackConfigV1()
                )
        val losMissing=
            empty.filter {
                it.key.startsWith(
                    "tower.requiresLineOfSight."
                )
            }

        val complete=
            CompleteFallbackFixtureFactory
                .create()
        val archer=
            RuntimeFallbackBindings
                .towerRequiresLineOfSight(
                    complete,"archer"
                )

        return listOf(
            FixtureResult(
                "tower-los-one-explicit-key-per-family",
                losMissing.size ==
                    RecommendedMatureTowerDefinitions
                        .all().size
            ),
            FixtureResult(
                "tower-los-explicit-engineering-binding",
                archer.value &&
                    archer.source==
                        ResolutionSource
                            .ENGINEERING_FALLBACK
            )
        )
    }
}
