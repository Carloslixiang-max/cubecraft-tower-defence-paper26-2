package dev.cubecrafttd.ui

import dev.cubecrafttd.match.PlayerMatchSessionState
import dev.cubecrafttd.mob.MobDefinitionRepository
import dev.cubecrafttd.mob.RecommendedMatureMobDefinitions

/**
 * Functional Stage-4 menu projections.
 *
 * The action semantics and normalized data are real runtime data. Exact Mature
 * slot positions/lore/icons for these menus have not been fully recovered, so
 * every slot below remains ENGINEERING_FALLBACK.
 */
object DynamicMatchMenus {
    fun summoner(
        player:
            PlayerMatchSessionState,
        definitions:
            MobDefinitionRepository =
            RecommendedMatureMobDefinitions
    ): MenuDefinition {
        val slots=
            definitions.all()
                .keys
                .sorted()
                .mapNotNull { mobId ->
                    val level=
                        player.progression
                            .level(mobId)
                    if(level<=0) null
                    else mobId to level
                }
                .mapIndexed {
                    index,(mobId,level) ->
                    MenuSlot(
                        slot=index,
                        actionId=
                            "summoner:mob:$mobId:l$level",
                        evidenceStatus=
                            UiEvidenceStatus
                                .ENGINEERING_FALLBACK,
                        displayName=
                            "$mobId L$level"
                    )
                }
                .toMutableList()

        slots += MenuSlot(
            slot=22,
            actionId="summoner:send",
            evidenceStatus=
                UiEvidenceStatus
                    .ENGINEERING_FALLBACK,
            displayName="Send selected troops"
        )
        slots += MenuSlot(
            slot=26,
            actionId="nav:progression",
            evidenceStatus=
                UiEvidenceStatus
                    .ENGINEERING_FALLBACK,
            displayName="Troop upgrades"
        )

        return MenuDefinition(
            title="Mob Summoner",
            size=27,
            slots=slots,
            evidenceStatus=
                UiEvidenceStatus
                    .ENGINEERING_FALLBACK
        )
    }

    fun progression(
        player:
            PlayerMatchSessionState,
        definitions:
            MobDefinitionRepository =
            RecommendedMatureMobDefinitions
    ): MenuDefinition {
        val mobIds=
            definitions.all()
                .keys
                .sorted()

        val slots=
            buildList {
                mobIds.forEachIndexed {
                    index,mobId ->
                    val current=
                        player.progression
                            .level(mobId)
                    add(
                        MenuSlot(
                            slot=index,
                            actionId=
                                "progression:upgrade:$mobId",
                            evidenceStatus=
                                UiEvidenceStatus
                                    .ENGINEERING_FALLBACK,
                            displayName=
                                "$mobId upgrade (current L$current)"
                        )
                    )
                    add(
                        MenuSlot(
                            slot=18+index,
                            actionId=
                                "progression:rollback:$mobId",
                            evidenceStatus=
                                UiEvidenceStatus
                                    .ENGINEERING_FALLBACK,
                            displayName=
                                "$mobId rollback"
                        )
                    )
                }
                add(
                    MenuSlot(
                        slot=35,
                        actionId="nav:summoner",
                        evidenceStatus=
                            UiEvidenceStatus
                                .ENGINEERING_FALLBACK,
                        displayName=
                            "Back to Mob Summoner"
                    )
                )
            }

        return MenuDefinition(
            title="Troop upgrades",
            size=36,
            slots=slots,
            evidenceStatus=
                UiEvidenceStatus
                    .ENGINEERING_FALLBACK
        )
    }

    fun bazaar(
        player:
            PlayerMatchSessionState
    ): MenuDefinition {
        val slots=
            mutableListOf(
                MenuSlot(
                    10,
                    "bazaar:goldmine:upgrade",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Upgrade Goldmine"
                ),
                MenuSlot(
                    12,
                    "bazaar:sword:upgrade",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Upgrade Sword"
                ),
                MenuSlot(
                    14,
                    "bazaar:bow:upgrade",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Upgrade Bow"
                )
            )

        RecommendedMatureBazaarDefinitions
            .aoePotions
            .forEachIndexed {
                index,potion ->
                val unlocked=
                    potion.potionId in
                        player.bazaar
                            .unlockedAoE
                slots += MenuSlot(
                    slot=27+index,
                    actionId=
                        if(unlocked)
                            "bazaar:potion:use:${potion.potionId}"
                        else
                            "bazaar:potion:unlock:${potion.potionId}",
                    evidenceStatus=
                        UiEvidenceStatus
                            .ENGINEERING_FALLBACK,
                    displayName=
                        if(unlocked)
                            "${potion.potionId} — use ${potion.useCostCoins} Coins"
                        else
                            "${potion.potionId} — unlock ${potion.unlockExp} EXP"
                )
            }

        return MenuDefinition(
            title="Bazaar",
            size=45,
            slots=slots,
            evidenceStatus=
                UiEvidenceStatus
                    .ENGINEERING_FALLBACK
        )
    }

    fun settings(
        player:
            PlayerMatchSessionState
    ): MenuDefinition {
        val model=
            SettingsMenuProjector
                .project(
                    player.interaction
                        .settings
                )

        return MenuDefinition(
            title="Settings",
            size=27,
            evidenceStatus=
                UiEvidenceStatus
                    .ENGINEERING_FALLBACK,
            slots=listOf(
                MenuSlot(
                    11,
                    "settings:auto-centre",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Auto-centre towers: ${model.autoCentreEnabled}"
                ),
                MenuSlot(
                    15,
                    "settings:point-purchases",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "In-game Point purchases: ${model.allowInGamePointPurchases}"
                ),
                MenuSlot(
                    22,
                    "nav:hotbar",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Edit hotbar layout"
                )
            )
        )
    }


    fun hotbarEditor(
        player:
            PlayerMatchSessionState
    ): MenuDefinition {
        val layout=
            player.interaction
                .hotbarLayout
        val slots=
            buildList {
                HotbarAction.entries
                    .forEachIndexed {
                        row,action ->
                        repeat(9) { slot ->
                            val current=
                                layout.slot(action)==
                                    slot
                            add(
                                MenuSlot(
                                    slot=
                                        row*9 +
                                            slot,
                                    actionId=
                                        "hotbar:move:" +
                                            action.name +
                                            ":" +
                                            slot,
                                    evidenceStatus=
                                        UiEvidenceStatus
                                            .ENGINEERING_FALLBACK,
                                    displayName=
                                        action.name
                                            .lowercase()
                                            .replace(
                                                '_',' '
                                            ) +
                                            " -> slot " +
                                            (slot+1) +
                                            if(current)
                                                " (current)"
                                            else ""
                                )
                            )
                        }
                    }
                add(
                    MenuSlot(
                        53,
                        "nav:settings",
                        UiEvidenceStatus
                            .ENGINEERING_FALLBACK,
                        "Back to Settings"
                    )
                )
            }

        return MenuDefinition(
            title="Hotbar editor",
            size=54,
            slots=slots,
            evidenceStatus=
                UiEvidenceStatus
                    .ENGINEERING_FALLBACK
        )
    }

}
