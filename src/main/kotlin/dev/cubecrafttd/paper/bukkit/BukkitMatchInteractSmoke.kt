package dev.cubecrafttd.paper.bukkit

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.EventExecutor
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.RegisteredListener
import java.lang.reflect.Proxy

/** Real Paper events/RegisteredListener delivery; synthetic actors, no client packet claim. */
object BukkitMatchInteractSmoke {
    fun run(plugin: Plugin) {
        fun <T> actor(type: Class<T>): T = type.cast(Proxy.newProxyInstance(
            type.classLoader, arrayOf(type)
        ) { _, method, _ -> error("Unexpected synthetic actor call: ${method.name}") })
        val player = actor(Player::class.java)
        val block = actor(Block::class.java)
        fun input(action: Action, hand: EquipmentSlot? = EquipmentSlot.HAND) = PlayerInteractEvent(
            player, action, ItemStack(Material.BOOK),
            if (action == Action.RIGHT_CLICK_BLOCK || action == Action.LEFT_CLICK_BLOCK) block else null,
            BlockFace.UP, hand
        )
        val hotbarHandler = BukkitMatchHotbarListener::class.java
            .getMethod("onInteract", PlayerInteractEvent::class.java).getAnnotation(EventHandler::class.java)
        val aoeHandler = BukkitAoEPotionTargetListener::class.java
            .getMethod("onInteract", PlayerInteractEvent::class.java).getAnnotation(EventHandler::class.java)
        check(!hotbarHandler.ignoreCancelled && !aoeHandler.ignoreCancelled) {
            "Native predicted air interactions must reach the explicit input policy"
        }
        var accepted = 0
        val delivery = RegisteredListener(object : Listener {}, EventExecutor { _, event ->
            if (BukkitMatchInteractPolicy.acceptsRightClick(event as PlayerInteractEvent)) accepted++
        }, hotbarHandler.priority, plugin, hotbarHandler.ignoreCancelled)

        val air = input(Action.RIGHT_CLICK_AIR)
        check(air.useInteractedBlock() == Event.Result.DENY && air.useItemInHand() == Event.Result.DEFAULT)
        delivery.callEvent(air)
        delivery.callEvent(input(Action.RIGHT_CLICK_AIR, EquipmentSlot.OFF_HAND))
        check(accepted == 1) { "Main/offhand air gesture must dispatch exactly once" }
        check(air.useInteractedBlock() == Event.Result.DENY && air.useItemInHand() == Event.Result.DEFAULT) {
            "Input policy must not re-enable vanilla interaction"
        }
        check(!BukkitMatchInteractPolicy.acceptsBlockRightClick(air))

        val rightBlock = input(Action.RIGHT_CLICK_BLOCK)
        check(BukkitMatchInteractPolicy.acceptsBlockRightClick(rightBlock))
        delivery.callEvent(rightBlock)
        val offhandBlock = input(Action.RIGHT_CLICK_BLOCK, EquipmentSlot.OFF_HAND)
        check(!BukkitMatchInteractPolicy.acceptsBlockRightClick(offhandBlock))
        delivery.callEvent(offhandBlock)
        check(accepted == 2) { "Main/offhand world gesture must dispatch exactly once" }

        val deniedAir = input(Action.RIGHT_CLICK_AIR).apply { setUseItemInHand(Event.Result.DENY) }
        val deniedBlock = input(Action.RIGHT_CLICK_BLOCK).apply { setUseInteractedBlock(Event.Result.DENY) }
        val deniedItem = input(Action.RIGHT_CLICK_BLOCK).apply { setUseItemInHand(Event.Result.DENY) }
        val cancelled = input(Action.RIGHT_CLICK_BLOCK).apply { isCancelled = true }
        listOf(deniedAir, deniedBlock, deniedItem, cancelled,
            input(Action.LEFT_CLICK_AIR), input(Action.LEFT_CLICK_BLOCK),
            input(Action.PHYSICAL, null), input(Action.RIGHT_CLICK_AIR, null)
        ).forEach { event ->
            val before = event.useInteractedBlock() to event.useItemInHand()
            check(!BukkitMatchInteractPolicy.acceptsRightClick(event) &&
                !BukkitMatchInteractPolicy.acceptsBlockRightClick(event)) { "Unexpected accepted input ${event.action}/${event.hand}" }
            delivery.callEvent(event)
            check(before == (event.useInteractedBlock() to event.useItemInHand()))
        }
        check(accepted == 2) { "Denied, left, physical and unknown-hand input must have no dispatch" }
    }
}
