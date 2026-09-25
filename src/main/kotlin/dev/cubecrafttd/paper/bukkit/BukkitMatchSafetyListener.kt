package dev.cubecrafttd.paper.bukkit

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.plugin.Plugin

/**
 * Engineering Playtest safety shell.
 *
 * Match players use a snapshotted/cleared inventory and ADVENTURE mode. These
 * guards keep the temporary match loadout intact and prevent unrelated vanilla
 * survival damage/hunger from invalidating a controlled TD round-trip test.
 * They are not asserted as original CubeCraft rules.
 */
class BukkitMatchSafetyListener(
    private val plugin: Plugin,
    private val controller:
        BukkitNormalArenaController
) : Listener {
    init {
        plugin.server.pluginManager
            .registerEvents(this,plugin)
    }

    @EventHandler(
        priority=EventPriority.LOWEST,
        ignoreCancelled=false
    )
    fun onInventoryClick(
        event: InventoryClickEvent
    ) {
        val player =
            event.whoClicked as? Player
                ?: return
        if(controller.isActivePlayer(player.uniqueId)) {
            event.isCancelled=true
        }
    }

    @EventHandler(
        priority=EventPriority.LOWEST,
        ignoreCancelled=false
    )
    fun onInventoryDrag(
        event: InventoryDragEvent
    ) {
        val player =
            event.whoClicked as? Player
                ?: return
        if(controller.isActivePlayer(player.uniqueId)) {
            event.isCancelled=true
        }
    }

    @EventHandler(
        priority=EventPriority.LOWEST,
        ignoreCancelled=false
    )
    fun onDrop(
        event: PlayerDropItemEvent
    ) {
        if(
            controller.isActivePlayer(
                event.player.uniqueId
            )
        ) {
            event.isCancelled=true
        }
    }

    @EventHandler(
        priority=EventPriority.LOWEST,
        ignoreCancelled=false
    )
    fun onSwapHands(
        event: PlayerSwapHandItemsEvent
    ) {
        if(
            controller.isActivePlayer(
                event.player.uniqueId
            )
        ) {
            event.isCancelled=true
        }
    }

    @EventHandler(
        priority=EventPriority.LOWEST,
        ignoreCancelled=false
    )
    fun onPickup(
        event: EntityPickupItemEvent
    ) {
        val player =
            event.entity as? Player
                ?: return
        if(controller.isActivePlayer(player.uniqueId)) {
            event.isCancelled=true
        }
    }

    @EventHandler(
        priority=EventPriority.HIGHEST,
        ignoreCancelled=true
    )
    fun onPlayerDamage(
        event: EntityDamageEvent
    ) {
        val player =
            event.entity as? Player
                ?: return
        if(controller.isActivePlayer(player.uniqueId)) {
            event.isCancelled=true
        }
    }

    @EventHandler(
        priority=EventPriority.HIGHEST,
        ignoreCancelled=true
    )
    fun onFoodLevelChange(
        event: FoodLevelChangeEvent
    ) {
        val player =
            event.entity as? Player
                ?: return
        if(controller.isActivePlayer(player.uniqueId)) {
            event.isCancelled=true
        }
    }
}
