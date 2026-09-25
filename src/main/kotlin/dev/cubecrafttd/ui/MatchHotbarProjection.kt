package dev.cubecrafttd.ui

import dev.cubecrafttd.match.PlayerMatchSessionState

enum class MatchHotbarVisualKind {
    WOODEN_SWORD,
    STONE_SWORD,
    IRON_SWORD,
    BOW,
    SUMMONER_CHEST,
    CASTLE_BAZAAR_STONE_BRICKS,
    SETTINGS_CRAFTING_TABLE
}

data class MatchHotbarItemProjection(
    val action: HotbarAction,
    val slot: Int,
    val visual: MatchHotbarVisualKind,
    val displayName: String,
    val evidenceStatus: String
)

/**
 * Semantic projection only. Bukkit material/item metadata belongs in the Paper
 * adapter. This keeps the verified action/layout state independent of Bukkit.
 */
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

        return listOf(
            MatchHotbarItemProjection(
                HotbarAction.SWORD,
                layout.slot(
                    HotbarAction.SWORD
                ),
                swordVisual,
                "Sword ${player.bazaar.swordTierIndex+1}",
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
    }
}
