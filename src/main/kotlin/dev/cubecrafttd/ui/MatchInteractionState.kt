package dev.cubecrafttd.ui

sealed interface HotbarEditorSelection {
    data class Action(
        val action: HotbarAction
    ) : HotbarEditorSelection

    data class AoE(
        val potionId: String
    ) : HotbarEditorSelection {
        init {
            require(potionId.isNotBlank())
        }
    }
}

data class PlayerMatchInteractionState(
    val builder: TowerBuilderSession =
        TowerBuilderSession(),
    val summonerDraft:
        SummonerDraftState =
        SummonerDraftState(),
    var settings:
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
        HotbarEditorSelection? = null
)
