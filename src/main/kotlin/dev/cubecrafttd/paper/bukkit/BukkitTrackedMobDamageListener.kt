package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.paper.*
import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.plugin.Plugin
import java.util.UUID

fun interface TrackedMobPredicate {
    fun contains(entityUuid: UUID): Boolean
}

class BukkitTrackedMobDamageListener(
    plugin: Plugin,
    private val trackedMobs: TrackedMobPredicate,
    private val sink: LivePlayerWeaponHitPort
) : Listener {
    init {
        plugin.server.pluginManager.registerEvents(this,plugin)
    }

    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    fun onEntityDamage(event: EntityDamageEvent) {
        if (event is EntityDamageByEntityEvent) return
        if (trackedMobs.contains(event.entity.uniqueId)) {
            event.isCancelled = true
        }
    }

    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    fun onDamageByEntity(event: EntityDamageByEntityEvent) {
        if (!trackedMobs.contains(event.entity.uniqueId)) return

        // Live entity HP is never authoritative for TD mobs.
        event.isCancelled = true

        val player = event.damageSource.causingEntity as? Player
            ?: return
        val direct = event.damageSource.directEntity
        val kind = when {
            direct is AbstractArrow -> "BOW"
            direct?.uniqueId == player.uniqueId &&
                player.inventory.itemInMainHand.type.name
                    .endsWith("_SWORD") -> "SWORD"
            else -> return
        }

        sink.accept(
            LivePlayerWeaponHit(
                player.uniqueId,
                event.entity.uniqueId,
                kind,
                event.finalDamage
            )
        )
    }
}
