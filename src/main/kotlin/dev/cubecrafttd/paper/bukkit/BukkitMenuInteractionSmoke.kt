package dev.cubecrafttd.paper.bukkit

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.*
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack
import java.lang.reflect.Proxy
import java.util.UUID

/** Real Paper event objects/inventories with synthetic viewers; no human click certification. */
object BukkitMenuInteractionSmoke {
    fun run() {
        val uuid = UUID(110, 1)
        val top = Bukkit.createInventory(null, 36)
        val bottom = Bukkit.createInventory(null, 36)
        bottom.setItem(0, ItemStack(Material.DIAMOND, 3))
        fun <T> proxy(type: Class<T>, handler: (String, Array<out Any?>?) -> Any?): T = type.cast(
            Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { _, method, args ->
                when (method.name) {
                    "toString" -> "synthetic-menu-smoke"
                    "hashCode" -> 110
                    "equals" -> false
                    else -> handler(method.name, args)
                }
            })
        val player = proxy(Player::class.java) { name, _ ->
            when (name) { "getUniqueId" -> uuid; else -> error("Unexpected Player method $name") }
        }
        val view = proxy(InventoryView::class.java) { name, args ->
            when (name) {
                "getPlayer" -> player
                "getTopInventory" -> top
                "getBottomInventory" -> bottom
                "getType" -> InventoryType.CHEST
                "countSlots" -> 72
                "convertSlot" -> (args!![0] as Int).let { if (it >= 36) it - 36 else it }
                "getInventory" -> (args!![0] as Int).let { if (it < 0) null else if (it < 36) top else bottom }
                else -> error("Unexpected InventoryView method $name")
            }
        }
        val actions = mapOf(8 to "noop:hotbar:2021-arrow-unresolved", 27 to "hotbar:select:SWORD", 30 to "hotbar:place:3")
        fun checkClick(raw: Int, click: ClickType, action: InventoryAction, expected: String?) {
            val event = InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, raw, click, action, 0)
            val invocation = BukkitMenuInteractionGuard.click(event, actions)
            check(event.isCancelled && invocation?.actionId == expected) { "Unsafe menu click: $raw/$click" }
            check(top.getItem(30) == null && bottom.getItem(0) == ItemStack(Material.DIAMOND, 3))
        }
        checkClick(30, ClickType.LEFT, InventoryAction.PICKUP_ALL, "hotbar:place:3")
        checkClick(27, ClickType.RIGHT, InventoryAction.PICKUP_HALF, "hotbar:select:SWORD")
        checkClick(8, ClickType.LEFT, InventoryAction.PICKUP_ALL, null)
        checkClick(5, ClickType.LEFT, InventoryAction.NOTHING, null)
        checkClick(36, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY, null)
        checkClick(36, ClickType.DOUBLE_CLICK, InventoryAction.COLLECT_TO_CURSOR, null)
        checkClick(36, ClickType.NUMBER_KEY, InventoryAction.HOTBAR_SWAP, null)
        checkClick(-999, ClickType.LEFT, InventoryAction.DROP_ALL_CURSOR, null)
        checkClick(27, ClickType.DROP, InventoryAction.DROP_ONE_SLOT, null)
        checkClick(27, ClickType.SWAP_OFFHAND, InventoryAction.HOTBAR_SWAP, null)
        listOf(setOf(0, 36), setOf(36)).forEach { slots ->
            val drag = InventoryDragEvent(view, ItemStack(Material.AIR), ItemStack(Material.DIAMOND, 3),
                false, slots.associateWith { ItemStack(Material.DIAMOND) })
            BukkitMenuInteractionGuard.drag(drag)
            check(drag.isCancelled)
        }
        check(top.contents.all { it == null } && bottom.getItem(0) == ItemStack(Material.DIAMOND, 3))
        val empty = BukkitEngineeringMenuRenderer().render("hotbar:place:3", "empty-slot")
        check(empty.type == Material.AIR)
        top.setItem(30, empty)
        check(top.getItem(30) == null)
        checkClick(30, ClickType.LEFT, InventoryAction.NOTHING, "hotbar:place:3")
    }
}
