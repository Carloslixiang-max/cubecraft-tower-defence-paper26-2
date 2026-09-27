package dev.cubecrafttd.testing

import dev.cubecrafttd.ui.MobCombatVisualProjection
import dev.cubecrafttd.ui.PlayerMatchSettings

object MobCombatVisualProjectionFixture {
    fun run(): List<FixtureResult> {
        val enabled=
            PlayerMatchSettings(
                lifetimeWins=20,
                digitalMobHealth=true,
                damageIndicators=true
            )
        val disabled=
            PlayerMatchSettings(
                lifetimeWins=20
            )

        return listOf(
            FixtureResult(
                "mob-visual-health-integer-format",
                MobCombatVisualProjection
                    .healthText(
                        12.0,
                        20.0
                    )=="12 / 20"
            ),
            FixtureResult(
                "mob-visual-health-decimal-format",
                MobCombatVisualProjection
                    .healthText(
                        12.25,
                        20.0
                    )=="12.3 / 20"
            ),
            FixtureResult(
                "mob-visual-damage-integer-format",
                MobCombatVisualProjection
                    .damageText(
                        4.0
                    )=="-4"
            ),
            FixtureResult(
                "mob-visual-damage-decimal-format",
                MobCombatVisualProjection
                    .damageText(
                        4.25
                    )=="-4.3"
            ),
            FixtureResult(
                "mob-visual-damage-delta-clamps-healing",
                MobCombatVisualProjection
                    .damageAmount(
                        10.0,
                        14.0
                    )==0.0 &&
                    MobCombatVisualProjection
                        .damageAmount(
                            14.0,
                            9.5
                        )==4.5
            ),
            FixtureResult(
                "mob-visual-settings-gate-projection",
                MobCombatVisualProjection
                    .digitalHealthEnabled(
                        enabled
                    ) &&
                    MobCombatVisualProjection
                        .damageIndicatorsEnabled(
                            enabled
                        ) &&
                    !MobCombatVisualProjection
                        .digitalHealthEnabled(
                            disabled
                        ) &&
                    !MobCombatVisualProjection
                        .damageIndicatorsEnabled(
                            disabled
                        )
            )
        )
    }
}
