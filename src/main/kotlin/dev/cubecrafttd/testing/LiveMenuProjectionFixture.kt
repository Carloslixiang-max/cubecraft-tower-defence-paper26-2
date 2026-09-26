package dev.cubecrafttd.testing

import dev.cubecrafttd.match.PlayerMatchSessionState
import dev.cubecrafttd.paper.bukkit.toLiveView
import dev.cubecrafttd.progression.TroopProgressionState
import dev.cubecrafttd.ui.DynamicMatchMenus
import dev.cubecrafttd.ui.TowerBuilderMenus
import java.util.UUID

object LiveMenuProjectionFixture {
    fun run(): List<FixtureResult> {
        val player=
            PlayerMatchSessionState(
                UUID.fromString(
                    "00000000-0000-0000-0000-000000039001"
                ),
                TroopProgressionState(
                    unlockedLevelByMob=
                        linkedMapOf(
                            "zombie" to 1
                        )
                )
            )

        val summoner=
            DynamicMatchMenus
                .summoner(player)
                .toLiveView()
        val progression=
            DynamicMatchMenus
                .progression(player)
                .toLiveView()
        val builder=
            TowerBuilderMenus
                .threeByThree2021
                .toLiveView()

        return listOf(
            FixtureResult(
                "live-menu-projection-preserves-labels-and-navigation",
                summoner.slotActionIds[22] ==
                    "nav:progression" &&
                    summoner.slotDisplayNames[22] ==
                    "Upgrade mobs" &&
                    summoner.slotActionIds[26] ==
                    "summoner:send" &&
                    summoner.slotDisplayNames[26] ==
                    "Send selected troops" &&
                    progression.slotActionIds[35] ==
                    "nav:summoner" &&
                    progression.slotDisplayNames[35] ==
                    "Back to Mob Summoner"
            ),
            FixtureResult(
                "live-menu-projection-preserves-evidence-backed-icon-hints",
                builder.slotIconHints[2]=="bow" &&
                    builder.slotIconHints[6]=="tnt" &&
                    builder.slotIconHints[12]=="beacon" &&
                    builder.slotIconHints[14]=="dirt" &&
                    builder.slotIconHints[15]=="potion" &&
                    builder.slotIconHints[40]=="book"
            ),
            FixtureResult(
                "summoner-historical-bottom-controls-use-nether-star-and-spawner",
                summoner.slotActionIds[22]=="nav:progression" &&
                    summoner.slotIconHints[22]=="nether-star" &&
                    summoner.slotActionIds[26]=="summoner:send" &&
                    summoner.slotIconHints[26]=="spawner"
            )
        )
    }
}
