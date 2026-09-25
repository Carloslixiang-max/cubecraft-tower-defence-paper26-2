package dev.cubecrafttd.ui

import dev.cubecrafttd.tower.visual.TowerPath

enum class TowerBuilderFootprint { THREE_BY_THREE, FIVE_BY_FIVE }

object TowerBuilderMenus {
    /**
     * Exact zero-based slots recovered from a 2021 gameplay screenshot.
     * Some icon identities (Ice/Mage/Sorcerer) remain visual inference, so
     * icon hints are not treated as stronger evidence than the slot layout.
     */
    val threeByThree2021 = MenuDefinition(
        title = "Tower builder",
        size = 45,
        evidenceStatus = UiEvidenceStatus.MATURE_DIRECT,
        slots = listOf(
            MenuSlot(2,"tower:archer",UiEvidenceStatus.MATURE_DIRECT,"Archer Tower","bow"),
            MenuSlot(3,"tower:ice",UiEvidenceStatus.MATURE_DIRECT,"Ice Tower","ice-like"),
            MenuSlot(5,"tower:mage",UiEvidenceStatus.MATURE_DIRECT,"Mage Tower","dark-block"),
            MenuSlot(6,"tower:artillery",UiEvidenceStatus.MATURE_DIRECT,"Artillery Tower","tnt"),
            MenuSlot(11,"tower:sorcerer",UiEvidenceStatus.MATURE_DIRECT,"Sorcerer Tower","ender-like"),
            MenuSlot(12,"tower:zeus",UiEvidenceStatus.MATURE_DIRECT,"Zeus Tower","beacon"),
            MenuSlot(14,"tower:quake",UiEvidenceStatus.MATURE_DIRECT,"Quake Tower","dirt"),
            MenuSlot(15,"tower:poison",UiEvidenceStatus.MATURE_DIRECT,"Poison Tower","potion"),
            MenuSlot(40,"builder:book",UiEvidenceStatus.MATURE_DIRECT,"Tower information","book")
        )
    )

    /**
     * Exact Mature 5x5 builder slot indices are not recovered. These positions
     * are a clean engineering fallback and must remain separately tagged.
     */
    val fiveByFiveEngineering = MenuDefinition(
        title = "Tower builder",
        size = 27,
        evidenceStatus = UiEvidenceStatus.ENGINEERING_FALLBACK,
        slots = listOf(
            MenuSlot(11,"tower:necromancer",UiEvidenceStatus.ENGINEERING_FALLBACK,"Necromancer Tower"),
            MenuSlot(13,"tower:turret",UiEvidenceStatus.ENGINEERING_FALLBACK,"Turret Tower"),
            MenuSlot(15,"tower:leach",UiEvidenceStatus.ENGINEERING_FALLBACK,"Leach Tower")
        )
    )

    /**
     * 2017 direct evidence shows all tower sizes in one builder and the 5x5
     * towers on a separate line. 2021 direct evidence recovers the 3x3 slots,
     * but not the exact Mature 5x5 indices. The three 5x5 positions below are
     * therefore deliberately tagged ENGINEERING_FALLBACK.
     */
    val combinedMatureEngineering = MenuDefinition(
        title = "Tower builder",
        size = 45,
        evidenceStatus = UiEvidenceStatus.MATURE_CONTEXT,
        slots = threeByThree2021.slots + listOf(
            MenuSlot(29,"tower:necromancer",UiEvidenceStatus.ENGINEERING_FALLBACK,"Necromancer Tower"),
            MenuSlot(31,"tower:turret",UiEvidenceStatus.ENGINEERING_FALLBACK,"Turret Tower"),
            MenuSlot(33,"tower:leach",UiEvidenceStatus.ENGINEERING_FALLBACK,"Leach Tower")
        )
    )

    val pathSelectorEngineeringSlots = MenuDefinition(
        title = "Select an upgrade path",
        size = 27,
        evidenceStatus = UiEvidenceStatus.MATURE_CONTEXT,
        slots = listOf(
            // Two symmetric middle-row slots are known; exact indices still need
            // direct measurement, so 11/15 are explicitly engineering fallback.
            MenuSlot(11,"path:top",UiEvidenceStatus.ENGINEERING_FALLBACK,"Path 1"),
            MenuSlot(15,"path:bottom",UiEvidenceStatus.ENGINEERING_FALLBACK,"Path 2")
        )
    )
}

data class TowerBuilderSelection(
    val towerId: String,
    val path: TowerPath
)

class TowerBuilderSession {
    var selectedTowerType: String? = null
        private set
    var selectedTowerPath: TowerPath? = null
        private set

    var lastPlacedTowerType: String? = null
        private set
    var lastPlacedTowerPath: TowerPath? = null
        private set

    fun select(
        towerId: String,
        path: TowerPath?
    ): TowerBuilderSelection {
        val resolved=
            path ?: TowerPath.TOP
        selectedTowerType=towerId
        selectedTowerPath=resolved
        return TowerBuilderSelection(
            towerId,resolved
        )
    }

    /**
     * Backward-compatible helper retained for old fixtures/callers.
     * "remember" now means a successful placement was confirmed.
     */
    fun remember(
        towerId: String,
        path: TowerPath?
    ): TowerBuilderSelection {
        val selection=
            TowerBuilderSelection(
                towerId,
                path ?: TowerPath.TOP
            )
        confirmPlaced(selection)
        return selection
    }

    fun selected():
        TowerBuilderSelection? {
        val id=selectedTowerType
            ?: return null
        return TowerBuilderSelection(
            id,
            selectedTowerPath
                ?: TowerPath.TOP
        )
    }

    fun confirmPlaced(
        selection:
            TowerBuilderSelection
    ) {
        selectedTowerType=
            selection.towerId
        selectedTowerPath=
            selection.path
        lastPlacedTowerType=
            selection.towerId
        lastPlacedTowerPath=
            selection.path
    }

    fun quickPlaceSelection():
        TowerBuilderSelection? {
        val id=lastPlacedTowerType
            ?: return null
        val path=
            lastPlacedTowerPath
                ?: TowerPath.TOP
        return TowerBuilderSelection(
            id,path
        )
    }
}

