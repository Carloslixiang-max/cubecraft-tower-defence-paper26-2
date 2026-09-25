package dev.cubecrafttd.testing

import dev.cubecrafttd.tower.TargetPriorityPolicy
import dev.cubecrafttd.truth.*

object TowerDefaultTargetPriorityFallbackFixture {
    fun run():List<FixtureResult> {
        val emptyMissing=
            GameplayFallbackCompletenessValidator
                .validate(
                    RuntimeFallbackConfigV1()
                )

        val complete=
            CompleteFallbackFixtureFactory
                .create()
        val resolved=
            RuntimeFallbackBindings
                .towerDefaultTargetPriority(
                    complete
                )

        return listOf(
            FixtureResult(
                "tower-default-priority-missing-blocks-runtime",
                emptyMissing.any {
                    it.key==
                        "tower.defaultTargetPriority" &&
                    it.level==
                        FallbackRequirementLevel
                            .BLOCKS_MECHANIC
                }
            ),
            FixtureResult(
                "tower-default-priority-explicit-engineering",
                resolved.value==
                    TargetPriorityPolicy.FIRST &&
                    resolved.source==
                        ResolutionSource
                            .ENGINEERING_FALLBACK
            )
        )
    }
}
