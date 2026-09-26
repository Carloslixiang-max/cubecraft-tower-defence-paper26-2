package dev.cubecrafttd.paper.bukkit

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class BukkitEngineeringMenuRenderer :
    BukkitMenuItemRenderer {
    override fun render(
        actionId: String
    ): ItemStack {
        val item=ItemStack(
            materialFor(actionId),
            1
        )
        item.editMeta {
            it.displayName(
                Component.text(
                    actionId
                )
            )
        }
        return item
    }

    private fun materialFor(
        actionId: String
    ): Material =
        when {
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
