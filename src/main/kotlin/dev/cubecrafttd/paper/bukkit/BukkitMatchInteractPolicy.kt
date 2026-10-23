package dev.cubecrafttd.paper.bukkit

import org.bukkit.event.Event
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

/**
 * Paper fires interactions per hand and predicts no-op air clicks as block DENY.
 * Accept that air prediction without reviving an explicitly denied item action.
 * Scope/ownership checks still belong to each active-match controller operation.
 */
object BukkitMatchInteractPolicy {
    fun matchesArenaWorld(expected: java.util.UUID?, actual: java.util.UUID): Boolean =
        expected != null && expected == actual

    fun acceptsRightClick(event: PlayerInteractEvent): Boolean {
        if (event.hand != EquipmentSlot.HAND) return false
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return false
        if (event.useItemInHand() == Event.Result.DENY) return false
        return event.action == Action.RIGHT_CLICK_AIR || event.useInteractedBlock() != Event.Result.DENY
    }

    fun acceptsBlockRightClick(event: PlayerInteractEvent): Boolean =
        event.action == Action.RIGHT_CLICK_BLOCK && acceptsRightClick(event)
}
