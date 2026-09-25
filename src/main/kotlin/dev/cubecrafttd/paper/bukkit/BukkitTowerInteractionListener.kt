package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.ui.TowerWorldInteractionSemantics
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin

class BukkitTowerInteractionListener(
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
        priority=EventPriority.NORMAL,
        ignoreCancelled=true
    )
    fun onInteract(
        event: PlayerInteractEvent
    ) {
        if(event.action!=Action.RIGHT_CLICK_BLOCK)
            return

        val block=event.clickedBlock
            ?: return
        val player=event.player
        val tower=
            controller.towerAt(
                player.uniqueId,
                BlockPos(
                    block.x,block.y,block.z
                )
            ) ?: return

        event.isCancelled=true
        val semantics=
            TowerWorldInteractionSemantics
                .rightClick(
                    player.isSneaking
                )

        if(semantics.quickUpgrade) {
            runCatching {
                controller
                    .quickUpgradeTower(
                        player.uniqueId,
                        tower
                    )
            }.onSuccess {
                player.sendMessage(
                    "Tower ${tower.value} upgraded."
                )
            }.onFailure {
                player.sendMessage(
                    "Tower upgrade ERROR: " +
                        "${it.javaClass.simpleName}: ${it.message}"
                )
            }
            return
        }

        if(semantics.openManagementMenu) {
            menus.open(
                player.uniqueId,
                controller
                    .towerManagementMenu(
                        player.uniqueId,
                        tower
                    )
                    .toLiveView()
            )
        }
    }
}
