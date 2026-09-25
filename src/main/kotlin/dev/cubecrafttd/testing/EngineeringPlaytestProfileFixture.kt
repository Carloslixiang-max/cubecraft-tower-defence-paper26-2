package dev.cubecrafttd.testing

import dev.cubecrafttd.match.ArmageddonType
import dev.cubecrafttd.truth.*

object EngineeringPlaytestProfileFixture {
    fun run(): List<FixtureResult> {
        val config=
            EngineeringPlaytestProfile.create()

        return listOf(
            FixtureResult(
                "engineering-playtest-normal-complete",
                RuntimeFallbackValidator
                    .validateForNormal(config)
                    .isEmpty()
            ),
            FixtureResult(
                "engineering-playtest-gameplay-complete",
                GameplayFallbackCompletenessValidator
                    .validate(config)
                    .isEmpty()
            ),
            FixtureResult(
                "engineering-playtest-burn-cadence-explicit",
                config.burnIntervalTicks==20L
            ),
            FixtureResult(
                "engineering-playtest-wither-complete",
                ArmageddonFallbackValidator
                    .validate(
                        ArmageddonType.WITHER,
                        config
                    ).isEmpty()
            ),
            FixtureResult(
                "engineering-playtest-lightning-complete",
                ArmageddonFallbackValidator
                    .validate(
                        ArmageddonType.LIGHTNING,
                        config
                    ).isEmpty()
            ),
            FixtureResult(
                "engineering-playtest-horde-complete",
                ArmageddonFallbackValidator
                    .validate(
                        ArmageddonType.HORDE,
                        config
                    ).isEmpty()
            )
        )
    }
}
