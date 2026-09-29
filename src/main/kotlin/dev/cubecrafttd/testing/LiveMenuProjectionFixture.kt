package dev.cubecrafttd.testing

import dev.cubecrafttd.match.PlayerMatchSessionState
import dev.cubecrafttd.paper.bukkit.toLiveView
import dev.cubecrafttd.progression.TroopProgressionState
import dev.cubecrafttd.ui.DynamicMatchMenus
import dev.cubecrafttd.ui.TowerBuilderMenus
import dev.cubecrafttd.ui.TowerManagementMenus
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
        val zeusPath=
            TowerBuilderMenus
                .pathSelector2021(
                    "zeus"
                )
                .toLiveView()
        val archerTowerMenu=
            TowerManagementMenus
                .mature2021(
                    7,
                    "Archer Tower",
                    1,
                    false
                )
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
            ),
            FixtureResult(
                "live-menu-projection-preserves-2021-zeus-path2-context",
                zeusPath.slotActionIds[15]==
                    "path:bottom" &&
                    zeusPath.slotDisplayNames[15]==
                        "Zeus Tower path 2" &&
                    zeusPath.slotIconHints[15]==
                        "sugar" &&
                    zeusPath.slotLore[15]==
                        listOf(
                            "Summons baby Zeus.",
                            "Click to select path!"
                        )
            ),
            FixtureResult(
                "live-menu-projection-preserves-2021-tower-menu-bottom-row-icons",
                archerTowerMenu.size==45 &&
                    archerTowerMenu.slotIconHints[36]=="stick" &&
                    archerTowerMenu.slotIconHints[40]=="book" &&
                    archerTowerMenu.slotIconHints[44]=="barrier"
            )
        )
    }
}
