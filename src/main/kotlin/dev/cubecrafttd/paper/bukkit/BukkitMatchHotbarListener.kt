package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.ui.HotbarAction
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin

class BukkitMatchHotbarListener(
    private val plugin: Plugin,
    private val controller:
        BukkitNormalArenaController,
    private val menus:
        BukkitMenuBridge
) : Listener {
    init {
        plugin.server.pluginManager
            .registerEvents(this,plugin)
    }

    @EventHandler(
        priority=EventPriority.LOWEST,
        ignoreCancelled=true
    )
    fun onInteract(
        event: PlayerInteractEvent
    ) {
        if(
            event.action!=
                Action.RIGHT_CLICK_AIR &&
            event.action!=
                Action.RIGHT_CLICK_BLOCK
        ) return

        val player=event.player
        val action=
            controller.hotbarActionAt(
                player.uniqueId,
                player.inventory.heldItemSlot
            ) ?: return

        val menuKind=
            when(action) {
                HotbarAction.SUMMONER ->
                    "summoner"
                HotbarAction.CASTLE_BAZAAR ->
                    "bazaar"
                HotbarAction.SETTINGS ->
                    "settings"
                HotbarAction.SWORD,
                HotbarAction.BOW ->
                    return
            }

        val menu=runCatching {
            controller
                .dynamicMenuForPlayer(
                    player.uniqueId,
                    menuKind
                )
        }.getOrElse {
            player.sendMessage(
                "TD menu ERROR: ${it.message}"
            )
            return
        }

        event.isCancelled=true
        menus.open(
            player.uniqueId,
            menu.toLiveView()
        )
    }
}
