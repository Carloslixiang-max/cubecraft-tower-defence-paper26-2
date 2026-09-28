package dev.cubecrafttd.testing

import dev.cubecrafttd.ui.*

object LifetimeWinProgressFixture {
    fun run(): List<FixtureResult> {
        val before=
            PlayerMatchSettings(
                lifetimeWins=19
            )
        val after=
            PlayerMatchSettings(
                lifetimeWins=
                    LifetimeWinProgress
                        .nextAfterWin(19)
            )
        val interaction=
            PlayerMatchInteractionState(
                settings=before
            )
        interaction.settings=after

        return listOf(
            FixtureResult(
                "lifetime-win-progress-crosses-auto-centre-threshold-at-20",
                !LifetimeWinProgress
                    .canDisableAutoCentre(
                        19
                    ) &&
                    LifetimeWinProgress
                        .canDisableAutoCentre(
                            20
                        ) &&
                    after.canDisableAutoCentre
            ),
            FixtureResult(
                "match-interaction-can-rehydrate-persisted-lifetime-wins",
                interaction.settings
                    .lifetimeWins==20 &&
                    interaction.settings
                        .canDisableAutoCentre
            )
        )
    }
}
