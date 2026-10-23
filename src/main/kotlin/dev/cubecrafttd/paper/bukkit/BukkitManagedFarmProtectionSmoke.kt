package dev.cubecrafttd.paper.bukkit

import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import java.lang.reflect.Proxy

/** Real event dispatcher, protected dimension versus the host world. No map writes. */
object BukkitManagedFarmProtectionSmoke {
    fun run(plugin: Plugin, owned: World) {
        val host=plugin.server.worlds.first { it.uid != owned.uid }
        val listener=BukkitManagedFarmProtection { it==owned.uid }
        plugin.server.pluginManager.registerEvents(listener,plugin)
        try {
            listOf(owned to true,host to false).forEach { (world,blocked) ->
                val block=world.getBlockAt(8,world.maxHeight-12,8)
                val actor=Proxy.newProxyInstance(Player::class.java.classLoader,arrayOf(Player::class.java)) { _,method,_ ->
                    when (method.name) {
                        "getWorld" -> world
                        "getUniqueId" -> java.util.UUID(0,124)
                        "toString" -> "FarmProtectionSmokePlayer"
                        else -> error("Unexpected smoke actor call: ${method.name}")
                    }
                } as Player
                val broken=BlockBreakEvent(block,actor)
                plugin.server.pluginManager.callEvent(broken)
                check(broken.isCancelled==blocked) { "Farm/host break isolation failed" }
                val placed=BlockPlaceEvent(block,block.state,block,ItemStack(Material.STONE),actor,true,EquipmentSlot.HAND)
                plugin.server.pluginManager.callEvent(placed)
                check(placed.isCancelled==blocked) { "Farm/host place isolation failed" }
                val click=PlayerInteractEvent(actor,Action.RIGHT_CLICK_BLOCK,ItemStack(Material.WOODEN_SWORD),block,BlockFace.UP,EquipmentSlot.HAND)
                plugin.server.pluginManager.callEvent(click)
                check((click.useInteractedBlock()==Event.Result.DENY)==blocked) { "Native container/lever/enchant interaction unprotected" }
                check(click.useItemInHand()==Event.Result.DEFAULT) { "Farm guard swallowed normal item use" }
                val denied=PlayerInteractEvent(actor,Action.RIGHT_CLICK_BLOCK,ItemStack(Material.BOW),block,BlockFace.UP,EquipmentSlot.HAND)
                denied.setUseItemInHand(Event.Result.DENY)
                plugin.server.pluginManager.callEvent(denied)
                check(denied.useItemInHand()==Event.Result.DENY) { "Farm guard revived an external item denial" }
                val flow=BlockFromToEvent(block,block.getRelative(BlockFace.EAST))
                plugin.server.pluginManager.callEvent(flow)
                check(flow.isCancelled==blocked) { "Fluid isolation failed" }
                val physics=BlockPhysicsEvent(block,Material.STONE.createBlockData())
                plugin.server.pluginManager.callEvent(physics)
                check(physics.isCancelled==blocked) { "Physics isolation failed" }
                val burn=BlockBurnEvent(block,block)
                plugin.server.pluginManager.callEvent(burn)
                check(burn.isCancelled==blocked) { "Burn isolation failed" }
                val piston=BlockPistonExtendEvent(block,listOf(block.getRelative(BlockFace.EAST)),BlockFace.EAST)
                plugin.server.pluginManager.callEvent(piston)
                check(piston.isCancelled==blocked) { "Piston isolation failed" }
                val redstone=BlockRedstoneEvent(block,0,15)
                plugin.server.pluginManager.callEvent(redstone)
                check(redstone.newCurrent==if(blocked) 0 else 15) { "Redstone isolation failed" }
            }
        } finally { HandlerList.unregisterAll(listener) }
        plugin.logger.info("Managed Farm protection PASS: break/place/native interactions, fluid/physics/fire/piston/redstone blocked only in owned dimension; item permissions and host-world events preserved")
    }
}
