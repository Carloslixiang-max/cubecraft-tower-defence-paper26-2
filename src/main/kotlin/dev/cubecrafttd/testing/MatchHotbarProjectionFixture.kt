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

        player.interaction
            .aoeInventory
            .add(
                "meteor",
                3
            )
        player.interaction
            .hotbarLayout=
            player.interaction
                .hotbarLayout
                .placeAoE(
                    "meteor",
                    4
                )
        val withAoE=
            MatchHotbarProjector
                .project(player)

        player.interaction
            .aoeInventory
            .consume(
                "meteor",
                3
            )
        val withoutStock=
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
            ),
            FixtureResult(
                "hotbar-projection-includes-owned-aoe-stack",
                withAoE
                    .firstOrNull {
                        it.potionId==
                            "meteor"
                    }
                    ?.let {
                        it.action==null &&
                        it.slot==4 &&
                        it.visual==
                            MatchHotbarVisualKind
                                .AOE_POTION &&
                        it.amount==3
                    } == true
            ),
            FixtureResult(
                "hotbar-projection-hides-empty-aoe-stack-without-forgetting-layout",
                withoutStock.none {
                    it.potionId==
                        "meteor"
                } &&
                    player.interaction
                        .hotbarLayout
                        .aoeSlot(
                            "meteor"
                        )==4
            )
        )
    }
}
