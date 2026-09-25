package dev.cubecrafttd.testing

import dev.cubecrafttd.tower.TowerChainTargetPolicy
import dev.cubecrafttd.truth.*

object TowerChainPolicyFixture {
    fun run(): List<FixtureResult> {
        val complete=
            CompleteFallbackFixtureFactory
                .create()
        val missingPolicy=
            complete.copy(
                towerChainTargetPolicy=null
            )
        val missing=
            GameplayFallbackCompletenessValidator
                .validate(missingPolicy)

        val resolved=
            NormalGameplayConfigResolverProxy
                .resolvePolicy(complete)

        return listOf(
            FixtureResult(
                "chain-policy-missing-blocks-full-runtime",
                missing.any {
                    it.key ==
                        "tower.chainTargetPolicy" &&
                    it.level ==
                        FallbackRequirementLevel
                            .BLOCKS_MECHANIC
                }
            ),
            FixtureResult(
                "chain-policy-explicit-engineering-source",
                resolved.value ==
                    TowerChainTargetPolicy
                        .ROUTE_PROGRESS_FORWARD_ENGINEERING &&
                    resolved.source ==
                        ResolutionSource
                            .ENGINEERING_FALLBACK
            )
        )
    }

    /**
     * Keeps this fixture focused on the policy binding rather than rebuilding
     * the whole Normal config.
     */
    private object NormalGameplayConfigResolverProxy {
        fun resolvePolicy(
            config: RuntimeFallbackConfigV1
        ) = RuntimeFallbackBindings
            .towerChainTargetPolicy(config)
    }
}
