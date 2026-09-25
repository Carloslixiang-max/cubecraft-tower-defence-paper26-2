package dev.cubecrafttd.paper.bukkit

import org.bukkit.entity.AbstractArrow
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.plugin.Plugin

/**
 * Engineering live-adapter shielding only.
 *
 * TD mob HP/pathing remain core-authoritative. These handlers suppress vanilla
 * visual/physics side effects that otherwise make AI-disabled tracked mobs look
 * wrong in a real Paper world.
 */
class BukkitTrackedMobEnvironmentListener(
    private val plugin: Plugin,
    private val trackedMobs:
        TrackedMobPredicate,
    private val activePlayer:
        (java.util.UUID)->Boolean
) : Listener {
    init {
        plugin.server.pluginManager
            .registerEvents(
                this,
                plugin
            )
    }

    @EventHandler(
        priority=EventPriority.HIGHEST,
        ignoreCancelled=false
    )
    fun onCombust(
        event: EntityCombustEvent
    ) {
        if(
            !trackedMobs.contains(
                event.entity.uniqueId
            )
        ) return

        event.isCancelled=true
        event.entity.fireTicks=0
    }

    @EventHandler(
        priority=EventPriority.MONITOR,
        ignoreCancelled=false
    )
    fun onProjectileHit(
        event: ProjectileHitEvent
    ) {
        val arrow=
            event.entity as?
                AbstractArrow
                ?: return
        val shooter=
            arrow.shooter as?
                Player
                ?: return
        if(
            !activePlayer(
                shooter.uniqueId
            )
        ) return

        // Defer removal until the current event chain completes so a tracked
        // mob EntityDamageByEntityEvent can still project the hit into core.
        plugin.server.scheduler
            .runTask(
                plugin,
                Runnable {
                    if(arrow.isValid) {
                        arrow.remove()
                    }
                }
            )
    }
}
