package dev.cubecrafttd.paper.bukkit

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class BukkitEngineeringMenuRenderer :
    BukkitMenuItemRenderer {
    override fun render(
        actionId: String,
        iconHint: String?
    ): ItemStack {
        val item=ItemStack(
            materialFor(
                actionId,
                iconHint
            ),
            1
        )
        item.editMeta {
            it.displayName(
                Component.text(
                    actionId
                )
            )
            if(
                iconHint==
                    "spawner-glow"
            ) {
                it.setEnchantmentGlintOverride(
                    true
                )
            }
        }
        return item
    }

    private fun materialFor(
        actionId: String,
        iconHint: String?
    ): Material =
        when(iconHint) {
            "bow" -> Material.BOW
            "wooden-sword" ->
                Material.WOODEN_SWORD
            "chest" -> Material.CHEST
            "stone-bricks" ->
                Material.STONE_BRICKS
            "crafting-table" ->
                Material.CRAFTING_TABLE
            "item-frame" ->
                Material.ITEM_FRAME
            "empty-slot" ->
                Material.GRAY_STAINED_GLASS_PANE
            "tnt" -> Material.TNT
            "beacon" -> Material.BEACON
            "dirt" -> Material.DIRT
            "potion" -> Material.POTION
            "book" -> Material.BOOK
            "anvil" -> Material.ANVIL
            "stick" -> Material.STICK
            "barrier" -> Material.BARRIER
            "flint" -> Material.FLINT
            "sugar" -> Material.SUGAR
            "nether-star" -> Material.NETHER_STAR
            "spawner" -> Material.SPAWNER
            "spawner-glow" ->
                Material.SPAWNER
            "glass-pane" -> Material.GLASS_PANE
            "glass-pane-orange" ->
                Material.ORANGE_STAINED_GLASS_PANE

            // These three 2021 builder identities are visual inference rather
            // than direct item-name evidence. They stay weaker evidence in the
            // MenuSlot model even though the renderer can now honor the hint.
            "ice-like" -> Material.PACKED_ICE
            "coal" -> Material.COAL
            "ender-eye" -> Material.ENDER_EYE

            // Compatibility aliases retained for older menu snapshots.
            "dark-block" -> Material.OBSIDIAN
            "ender-like" -> Material.ENDER_PEARL

            else -> when {
            actionId.startsWith(
                "tower:"
            ) -> Material.STONE
            actionId.startsWith(
                "summoner:"
            ) -> Material.CHEST
            actionId.startsWith(
                "progression:"
            ) -> Material.NETHER_STAR
            actionId.startsWith(
                "bazaar:"
            ) -> Material.EMERALD
            actionId.startsWith(
                "path:"
            ) -> Material.PAPER
            actionId=="armageddon:vote:wither" ->
                Material.WITHER_SKELETON_SKULL
            actionId=="armageddon:vote:lightning" ->
                Material.LIGHTNING_ROD
            actionId=="armageddon:vote:horde" ->
                Material.ZOMBIE_HEAD
            actionId=="pregame:armageddon:random" ->
                Material.END_CRYSTAL
            actionId=="pregame:armageddon:wither" ->
                Material.WITHER_SKELETON_SKULL
            actionId=="pregame:armageddon:lightning" ->
                Material.LIGHTNING_ROD
            actionId=="pregame:armageddon:horde" ->
                Material.ZOMBIE_HEAD
            actionId=="pregame:pricing:normal" ->
                Material.GOLD_NUGGET
            actionId=="pregame:pricing:double_income" ->
                Material.GOLD_INGOT
            actionId=="pregame:pricing:quick_start" ->
                Material.EMERALD
            actionId.startsWith(
                "nav:"
            ) -> Material.NETHER_STAR
            actionId.startsWith(
                "hotbar:"
            ) -> Material.ITEM_FRAME
            else -> Material.PAPER
            }
        }
}
