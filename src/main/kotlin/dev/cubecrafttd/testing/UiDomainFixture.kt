package dev.cubecrafttd.testing

import dev.cubecrafttd.ui.*
import dev.cubecrafttd.tower.visual.TowerPath

object UiDomainFixture {
    fun run(): List<FixtureResult> {
        val builder = TowerBuilderMenus.threeByThree2021
        val session = TowerBuilderSession()
        val selected = session.remember("zeus",null)

        val left = SummonerClickSemantics.resolve(
            ClickKind.LEFT,
            QueueEditContext(3,5,20)
        )
        val right = SummonerClickSemantics.resolve(
            ClickKind.RIGHT,
            QueueEditContext(3,5,20)
        )
        val max = SummonerClickSemantics.resolve(
            ClickKind.SHIFT_LEFT,
            QueueEditContext(3,5,4)
        )
        val clear = SummonerClickSemantics.resolve(
            ClickKind.SHIFT_RIGHT,
            QueueEditContext(3,5,20)
        )

        val hotbar = HotbarLayout.DEFAULT_2021
        val swapped = hotbar.move(
            HotbarAction.SUMMONER,8
        )

        return listOf(
            FixtureResult(
                "ui-2021-builder-exact-slots",
                builder.actionAt(2) == "tower:archer" &&
                    builder.actionAt(3) == "tower:ice" &&
                    builder.actionAt(5) == "tower:mage" &&
                    builder.actionAt(6) == "tower:artillery" &&
                    builder.actionAt(11) == "tower:sorcerer" &&
                    builder.actionAt(12) == "tower:zeus" &&
                    builder.actionAt(14) == "tower:quake" &&
                    builder.actionAt(15) == "tower:poison" &&
                    builder.actionAt(40) == "builder:book"
            ),
            FixtureResult(
                "ui-path-default-top-and-quick-place-remembers-path",
                selected.path == TowerPath.TOP &&
                    session.quickPlaceSelection()
                        ?.path == TowerPath.TOP
            ),
            FixtureResult(
                "ui-summoner-click-semantics",
                left.quantity == 4 &&
                    right.quantity == 2 &&
                    max.quantity == 7 &&
                    clear.quantity == 0
            ),
            FixtureResult(
                "ui-hotbar-defaults",
                hotbar.slot(HotbarAction.SWORD) == 0 &&
                    hotbar.slot(HotbarAction.BOW) == 1 &&
                    hotbar.slot(HotbarAction.SUMMONER) == 2 &&
                    hotbar.slot(HotbarAction.CASTLE_BAZAAR) == 3 &&
                    hotbar.slot(HotbarAction.SETTINGS) == 8
            ),
            FixtureResult(
                "ui-hotbar-customization-swaps-actions",
                swapped.slot(HotbarAction.SUMMONER) == 8 &&
                    swapped.slot(HotbarAction.SETTINGS) == 2
            ),
            FixtureResult(
                "ui-path-selector-exact-slot-gap-remains-labelled",
                TowerBuilderMenus.pathSelectorEngineeringSlots
                    .slots.all {
                        it.evidenceStatus ==
                            UiEvidenceStatus.ENGINEERING_FALLBACK
                    }
            )
        )
    }
}
