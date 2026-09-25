package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.tower.lifecycle.TowerPlacementInteractionResult
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin

/**
 * Evidence-backed world gesture:
 * regular right-click on a legal checked placement surface opens the builder;
 * shift-right-click uses the remembered quick-placement selection.
 *
 * This listener deliberately does not guess an exact required held-item gate.
 */
class BukkitTowerPlacementListener(
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
        priority=EventPriority.HIGH,
        ignoreCancelled=true
    )
    fun onInteract(
        event: PlayerInteractEvent
    ) {
        if(event.action!=Action.RIGHT_CLICK_BLOCK)
            return
        val block=event.clickedBlock ?: return
        val player=event.player
        val pos=BlockPos(block.x,block.y,block.z)

        // Existing tower interaction is handled earlier by the tower listener.
        if(controller.towerAt(player.uniqueId,pos)!=null)
            return
        if(!controller.canBeginTowerPlacement(player.uniqueId,pos))
            return

        event.isCancelled=true
        runCatching {
            if(player.isSneaking) {
                controller.quickPlaceTower(
                    player.uniqueId,pos
                )
            } else {
                controller.beginTowerPlacement(
                    player.uniqueId,pos
                )
            }
        }.onSuccess { result ->
            when(result) {
                is TowerPlacementInteractionResult.OpenBuilder ->
                    menus.open(
                        player.uniqueId,
                        result.menu.toLiveView()
                    )
                is TowerPlacementInteractionResult.Placed ->
                    player.sendMessage(
                        "Tower placed: ${result.result.selection.towerId}"
                    )
                is TowerPlacementInteractionResult.OpenPathSelector ->
                    menus.open(
                        player.uniqueId,
                        result.menu.toLiveView()
                    )
            }
        }.onFailure {
            player.sendMessage(
                "Tower placement ERROR: ${it.message}"
            )
        }
    }
}
