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
            else -> Material.PAPER
        }
}
