package dev.cubecrafttd.ui

import dev.cubecrafttd.match.PlayerMatchSessionState

enum class MatchHotbarVisualKind {
    WOODEN_SWORD,
    STONE_SWORD,
    IRON_SWORD,
    BOW,
    SUMMONER_CHEST,
    CASTLE_BAZAAR_STONE_BRICKS,
    SETTINGS_CRAFTING_TABLE,
    AOE_POTION
}

data class MatchHotbarItemProjection(
    val action: HotbarAction?,
    val slot: Int,
    val visual: MatchHotbarVisualKind,
    val displayName: String,
    val evidenceStatus: String,
    val potionId: String? = null,
    val amount: Int = 1
) {
    init {
        require(slot in 0..8)
        require(amount>0)
        check(
            (action==null) !=
                (potionId==null)
        ) {
            "Projection must represent exactly one fixed action or AoE potion"
        }
    }
}

object MatchHotbarProjector {
    fun project(
        player: PlayerMatchSessionState
    ): List<MatchHotbarItemProjection> {
        val layout=
            player.interaction.hotbarLayout

        val swordVisual=
            when(
                player.bazaar.swordTierIndex
            ) {
                0 ->
                    MatchHotbarVisualKind
                        .WOODEN_SWORD
                1 ->
                    MatchHotbarVisualKind
                        .STONE_SWORD
                2 ->
                    MatchHotbarVisualKind
                        .IRON_SWORD
                else ->
                    error(
                        "Unsupported sword tier " +
                            player.bazaar
                                .swordTierIndex
                    )
            }

        val bowTier=
            player.bazaar.bowTierIndex+1

        val controls=
            listOf(
                MatchHotbarItemProjection(
                    HotbarAction.SWORD,
                    layout.slot(
                        HotbarAction.SWORD
                    ),
                    swordVisual,
                    "Sword " +
                        (player.bazaar
                            .swordTierIndex+1),
                    "MATURE_WEAPON_TIER_MATERIAL"
                ),
                MatchHotbarItemProjection(
                    HotbarAction.BOW,
                    layout.slot(
                        HotbarAction.BOW
                    ),
                    MatchHotbarVisualKind.BOW,
                    "Bow $bowTier",
                    "MATURE_BOW_ITEM"
                ),
                MatchHotbarItemProjection(
                    HotbarAction.SUMMONER,
                    layout.slot(
                        HotbarAction.SUMMONER
                    ),
                    MatchHotbarVisualKind
                        .SUMMONER_CHEST,
                    "Mob Summoner",
                    "MATURE_GUIDE_CHEST"
                ),
                MatchHotbarItemProjection(
                    HotbarAction.CASTLE_BAZAAR,
                    layout.slot(
                        HotbarAction
                            .CASTLE_BAZAAR
                    ),
                    MatchHotbarVisualKind
                        .CASTLE_BAZAAR_STONE_BRICKS,
                    "Castle Bazaar",
                    "MATURE_GUIDE_STONE_BRICKS"
                ),
                MatchHotbarItemProjection(
                    HotbarAction.SETTINGS,
                    layout.slot(
                        HotbarAction.SETTINGS
                    ),
                    MatchHotbarVisualKind
                        .SETTINGS_CRAFTING_TABLE,
                    "Settings",
                    "OFFICIAL_2021_SCREENSHOT_EXAMPLE"
                )
            )

        val aoes=
            layout.aoeSlots
                .mapNotNull {
                    (potionId,slot) ->
                    val quantity=
                        player.interaction
                            .aoeInventory
                            .quantity(
                                potionId
                            )
                    if(quantity<=0) {
                        null
                    } else {
                        MatchHotbarItemProjection(
                            action=null,
                            slot=slot,
                            visual=
                                MatchHotbarVisualKind
                                    .AOE_POTION,
                            displayName=
                                potionId+" AoE",
                            evidenceStatus=
                                "OFFICIAL_2021_MULTI_AOE_HOTBAR_CONTEXT",
                            potionId=potionId,
                            amount=quantity
                        )
                    }
                }

        return (
            controls+
                aoes
        ).sortedBy {
            it.slot
        }
    }
}
