package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.ui.*
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent

/** Tracked menus are projections, including their visually empty action slots. */
object BukkitMenuInteractionGuard {
    fun click(event: InventoryClickEvent, actions: Map<Int, String>): MenuActionInvocation? {
        val player = event.whoClicked as? Player ?: return null
        // Bottom shift transfers, collection, number-key swaps and outside cursor drops
        // can alter the top inventory too. Cancel the whole tracked view first.
        event.isCancelled = true
        val raw = event.rawSlot
        if (raw !in 0 until event.view.topInventory.size) return null
        val action = actions[raw] ?: return null
        if (action.startsWith("noop:")) return null
        val click = when (event.click) {
            ClickType.LEFT -> ClickKind.LEFT
            ClickType.RIGHT -> ClickKind.RIGHT
            ClickType.SHIFT_LEFT -> ClickKind.SHIFT_LEFT
            ClickType.SHIFT_RIGHT -> ClickKind.SHIFT_RIGHT
            else -> return null
        }
        return MenuActionInvocation(player.uniqueId, action, click)
    }

    fun drag(event: InventoryDragEvent) { event.isCancelled = true }
}
