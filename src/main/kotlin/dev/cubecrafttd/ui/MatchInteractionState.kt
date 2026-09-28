package dev.cubecrafttd.ui

data class PlayerMatchInteractionState(
    val builder: TowerBuilderSession =
        TowerBuilderSession(),
    val summonerDraft:
        SummonerDraftState =
        SummonerDraftState(),
    val settings:
        PlayerMatchSettings =
        PlayerMatchSettings(
            lifetimeWins=0
        ),
    val towerPlacement:
        TowerPlacementFlowState =
        TowerPlacementFlowState(),
    val pinnedRangefinderTowers:
        MutableSet<Long> = linkedSetOf(),
    val aoeInventory:
        AoEPotionInventory =
        AoEPotionInventory(),
    var armedAoEPotion:
        PotionUseToken? = null,
    var hotbarLayout:
        HotbarLayout =
        HotbarLayout.ENGINEERING_RUNTIME_DEFAULT,
    var hotbarEditorSelection:
        HotbarAction? = null
)
