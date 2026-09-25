package dev.cubecrafttd.testing

import dev.cubecrafttd.match.PlayerMatchSessionState
import dev.cubecrafttd.progression.TroopProgressionState
import dev.cubecrafttd.ui.*
import java.util.UUID

object MatchHotbarProjectionFixture {
    fun run(): List<FixtureResult> {
        val player=
            PlayerMatchSessionState(
                UUID.fromString(
                    "00000000-0000-0000-0000-000000035001"
                ),
                TroopProgressionState()
            )

        val initial=
            MatchHotbarProjector
                .project(player)

        player.bazaar
            .swordTierIndex=2
        player.bazaar
            .bowTierIndex=2

        val upgraded=
            MatchHotbarProjector
                .project(player)

        return listOf(
            FixtureResult(
                "hotbar-projection-all-five-actions",
                initial.map {
                    it.action
                }.toSet()==
                    HotbarAction.entries
                        .toSet()
            ),
            FixtureResult(
                "hotbar-projection-engineering-layout-slots",
                initial.associate {
                    it.action to it.slot
                }==
                    HotbarLayout
                        .ENGINEERING_RUNTIME_DEFAULT
                        .slots
            ),
            FixtureResult(
                "hotbar-projection-reflects-weapon-tiers",
                upgraded.first {
                    it.action==
                        HotbarAction.SWORD
                }.visual==
                    MatchHotbarVisualKind
                        .IRON_SWORD &&
                    upgraded.first {
                        it.action==
                            HotbarAction.BOW
                    }.displayName==
                        "Bow 3"
            )
        )
    }
}
