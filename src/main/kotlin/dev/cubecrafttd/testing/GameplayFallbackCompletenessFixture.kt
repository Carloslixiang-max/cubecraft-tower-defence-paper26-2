package dev.cubecrafttd.testing

import dev.cubecrafttd.castle.GuardTargetPriorityPolicy
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.truth.*

object GameplayFallbackCompletenessFixture {
    private fun completeConfig() =
        CompleteFallbackFixtureFactory.create()


    fun run(): List<FixtureResult> {
        val empty =
            GameplayFallbackCompletenessValidator
                .validate(
                    RuntimeFallbackConfigV1()
                )

        val complete =
            completeConfig()
        val completeMissing =
            GameplayFallbackCompletenessValidator
                .validate(complete)

        val speed =
            RuntimeFallbackBindings
                .mobMovementRate(complete)
                .blocksPerTick(
                    "zombie",1,"zombie"
                )

        val reward =
            RuntimeFallbackBindings
                .mobKillReward(complete)
                .rewardCoins(
                    "giant",5
                )

        val archer =
            RecommendedMatureTowerDefinitions
                .get("archer")
                .stage(
                    TowerPathOption.TOP,4
                )
        val interval =
            RuntimeFallbackBindings
                .towerAttackInterval(
                    complete,
                    "archer",
                    archer
                )

        val mage =
            RecommendedMatureTowerDefinitions
                .get("mage")
                .stage(
                    TowerPathOption.NONE,4
                )
        val observedMageFallback =
            RuntimeFallbackBindings
                .towerAttackInterval(
                    complete,
                    "mage",
                    mage
                )

        return listOf(
            FixtureResult(
                "fallback-completeness-expands-all-mob-speed-keys",
                empty.count {
                    it.key.startsWith(
                        "mob.speedBlocksPerTick."
                    )
                } == 50
            ),
            FixtureResult(
                "fallback-completeness-expands-all-mob-kill-coin-keys",
                empty.count {
                    it.key.startsWith(
                        "mob.killCoins."
                    )
                } == 50
            ),
            FixtureResult(
                "fallback-completeness-expands-null-tower-cadences",
                empty.any {
                    it.key ==
                        "tower.attackIntervalSeconds.archer.top.l4"
                } &&
                    empty.any {
                        it.key ==
                            "tower.attackIntervalSeconds.poison.none.l1"
                    }
            ),
            FixtureResult(
                "fallback-completeness-complete-config-has-no-blockers",
                completeMissing.none {
                    it.level ==
                        FallbackRequirementLevel
                            .BLOCKS_MECHANIC
                }
            ),
            FixtureResult(
                "fallback-bindings-resolve-speed-kill-and-tower",
                speed.value==0.1 &&
                    reward.value==1L &&
                    interval?.value==1.0 &&
                    observedMageFallback==null
            )
        )
    }
}
