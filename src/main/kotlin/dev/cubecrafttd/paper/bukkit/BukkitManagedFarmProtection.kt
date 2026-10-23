package dev.cubecrafttd.paper.bukkit

import org.bukkit.World
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerBucketEmptyEvent
import org.bukkit.event.player.PlayerBucketFillEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.world.StructureGrowEvent
import java.util.UUID

/** Protect only an ownership-verified add-on dimension; no name-based adoption.
 * Direct plugin model/import writes remain possible. Native effects, visitors
 * and temporary match items must not mutate the saved, reusable Farm scene.
 */
class BukkitManagedFarmProtection(private val ownsWorld: (UUID) -> Boolean) : Listener {
    private fun protected(world: World) = ownsWorld(world.uid)

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onBreak(e: BlockBreakEvent) { if (protected(e.block.world)) e.isCancelled=true }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onPlace(e: BlockPlaceEvent) { if (protected(e.block.world)) e.isCancelled=true }

    // Gameplay listeners run at LOWEST/NORMAL/HIGH. Suppress vanilla block UI
    // and state changes afterwards without reviving another plugin's denial or
    // disabling legitimate item use (bow/consumables/owned menus).
    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onInteract(e: PlayerInteractEvent) {
        val block=e.clickedBlock ?: return
        if (protected(block.world)) e.setUseInteractedBlock(Event.Result.DENY)
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onBucketEmpty(e: PlayerBucketEmptyEvent) { if (protected(e.block.world)) e.isCancelled=true }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onBucketFill(e: PlayerBucketFillEvent) { if (protected(e.block.world)) e.isCancelled=true }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onFlow(e: BlockFromToEvent) {
        if (protected(e.block.world) || protected(e.toBlock.world)) e.isCancelled=true
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onPhysics(e: BlockPhysicsEvent) { if (protected(e.block.world)) e.isCancelled=true }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onIgnite(e: BlockIgniteEvent) { if (protected(e.block.world)) e.isCancelled=true }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onBurn(e: BlockBurnEvent) { if (protected(e.block.world)) e.isCancelled=true }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onGrow(e: BlockGrowEvent) { if (protected(e.block.world)) e.isCancelled=true }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onForm(e: BlockFormEvent) { if (protected(e.block.world)) e.isCancelled=true }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onFade(e: BlockFadeEvent) { if (protected(e.block.world)) e.isCancelled=true }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onSpread(e: BlockSpreadEvent) { if (protected(e.block.world)) e.isCancelled=true }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onLeaves(e: LeavesDecayEvent) { if (protected(e.block.world)) e.isCancelled=true }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onFertilize(e: BlockFertilizeEvent) { if (protected(e.block.world)) e.isCancelled=true }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onTree(e: StructureGrowEvent) { if (protected(e.world)) e.isCancelled=true }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onEntityChange(e: EntityChangeBlockEvent) { if (protected(e.block.world)) e.isCancelled=true }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onEntityExplosion(e: EntityExplodeEvent) {
        if (e.location.world?.let(::protected)==true) e.isCancelled=true
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onBlockExplosion(e: BlockExplodeEvent) { if (protected(e.block.world)) e.isCancelled=true }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onPistonExtend(e: BlockPistonExtendEvent) { if (protected(e.block.world)) e.isCancelled=true }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onPistonRetract(e: BlockPistonRetractEvent) { if (protected(e.block.world)) e.isCancelled=true }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=false)
    fun onRedstone(e: BlockRedstoneEvent) { if (protected(e.block.world)) e.newCurrent=e.oldCurrent }
}
