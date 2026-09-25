package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.match.PlayerMatchSessionState
import dev.cubecrafttd.ui.*
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import java.util.UUID

/**
 * Stage-4 live projection of the match hotbar.
 *
 * Semantic action positions come from HotbarLayout. Material choices are only
 * used to make the Engineering Playtest operable and never redefine original
 * CubeCraft truth.
 */
class BukkitMatchLoadoutService(
    private val server: Server
) {
    fun apply(
        playerUuid: UUID,
        state: PlayerMatchSessionState
    ) {
        val player=
            server.getPlayer(playerUuid)
                ?: error(
                    "Player $playerUuid is not online"
                )

        // Reprojection is idempotent: clear the hotbar first so a future
        // layout change cannot leave stale match controls behind.
        repeat(9) { slot ->
            player.inventory.setItem(
                slot,
                ItemStack.empty()
            )
        }

        val projection=
            MatchHotbarProjector
                .project(state)

        projection.forEach { item ->
            player.inventory.setItem(
                item.slot,
                render(item)
            )
        }

        // Engineering playtest support: keep one arrow outside the hotbar and
        // make the bow infinite so a long match cannot become unusable merely
        // because the current recreation has no recovered ammo-management rule.
        val ammoSlot=17
        if(
            player.inventory
                .getItem(ammoSlot)
                ?.type !=
                Material.ARROW
        ) {
            player.inventory.setItem(
                ammoSlot,
                ItemStack(
                    Material.ARROW,1
                )
            )
        }
    }

    private fun render(
        projection:
            MatchHotbarItemProjection
    ): ItemStack {
        val material=
            when(projection.visual) {
                MatchHotbarVisualKind
                    .WOODEN_SWORD ->
                    Material.WOODEN_SWORD
                MatchHotbarVisualKind
                    .STONE_SWORD ->
                    Material.STONE_SWORD
                MatchHotbarVisualKind
                    .IRON_SWORD ->
                    Material.IRON_SWORD
                MatchHotbarVisualKind.BOW ->
                    Material.BOW
                MatchHotbarVisualKind
                    .SUMMONER_CHEST ->
                    Material.CHEST
                MatchHotbarVisualKind
                    .CASTLE_BAZAAR_STONE_BRICKS ->
                    Material.STONE_BRICKS
                MatchHotbarVisualKind
                    .SETTINGS_CRAFTING_TABLE ->
                    Material.CRAFTING_TABLE
            }

        val item=ItemStack(material,1)
        item.editMeta {
            it.displayName(
                Component.text(
                    projection.displayName
                )
            )
            it.isUnbreakable=true
        }

        if(
            projection.visual==
                MatchHotbarVisualKind.BOW
        ) {
            item.addUnsafeEnchantment(
                Enchantment.INFINITY,
                1
            )
        }
        return item
    }
}
