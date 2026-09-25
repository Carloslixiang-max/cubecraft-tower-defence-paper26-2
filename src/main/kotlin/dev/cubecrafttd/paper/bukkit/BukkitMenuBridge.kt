package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.paper.*
import dev.cubecrafttd.ui.*
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import java.util.UUID

fun interface BukkitMenuItemRenderer {
    fun render(actionId: String): ItemStack
}

fun interface BukkitMenuActionSink {
    fun accept(
        invocation: MenuActionInvocation
    )
}

class BukkitMenuBridge(
    private val plugin: Plugin,
    private val renderer:
        BukkitMenuItemRenderer,
    private val actionSink:
        BukkitMenuActionSink
) : LiveMenuOpenPort, Listener {
    private val openActions =
        linkedMapOf<
            UUID,
            Map<Int,String>
        >()

    init {
        plugin.server.pluginManager
            .registerEvents(
                this,plugin
            )
    }

    override fun open(
        playerUuid: UUID,
        menu: LiveMenuView
    ) {
        val player = plugin.server
            .getPlayer(playerUuid)
            ?: return
        require(
            menu.size > 0 &&
                menu.size % 9 == 0
        )

        val inventory =
            Bukkit.createInventory(
                null,
                menu.size,
                Component.text(
                    menu.title
                )
            )

        menu.slotActionIds.forEach {
            (slot,action) ->
            require(
                slot in 0 until menu.size
            )
            inventory.setItem(
                slot,
                renderer.render(action)
            )
        }

        openActions[playerUuid] =
            LinkedHashMap(
                menu.slotActionIds
            )
        player.openInventory(
            inventory
        )
    }

    @EventHandler
    fun onClick(
        event: InventoryClickEvent
    ) {
        val player=
            event.whoClicked as? Player
                ?: return
        val actions=
            openActions[player.uniqueId]
                ?: return
        val raw=event.rawSlot
        if(
            raw < 0 ||
            raw >=
                event.view
                    .topInventory.size
        ) return

        event.isCancelled=true
        val action=
            actions[raw]
                ?: return

        val click=when(event.click) {
            ClickType.LEFT ->
                ClickKind.LEFT
            ClickType.RIGHT ->
                ClickKind.RIGHT
            ClickType.SHIFT_LEFT ->
                ClickKind.SHIFT_LEFT
            ClickType.SHIFT_RIGHT ->
                ClickKind.SHIFT_RIGHT
            else -> return
        }

        actionSink.accept(
            MenuActionInvocation(
                player.uniqueId,
                action,
                click
            )
        )
    }

    @EventHandler
    fun onClose(
        event: InventoryCloseEvent
    ) {
        openActions.remove(
            event.player.uniqueId
        )
    }
}
