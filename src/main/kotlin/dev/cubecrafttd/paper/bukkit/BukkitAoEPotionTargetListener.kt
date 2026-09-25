package dev.cubecrafttd.paper.bukkit

import org.bukkit.Particle
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.plugin.Plugin
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class BukkitAoEPotionTargetListener(
    private val plugin: Plugin,
    private val controller:
        BukkitNormalArenaController
) : Listener {
    init {
        plugin.server.pluginManager
            .registerEvents(this,plugin)
    }

    @EventHandler(
        priority=EventPriority.NORMAL,
        ignoreCancelled=false
    )
    fun onInteract(
        event: PlayerInteractEvent
    ) {
        val player=event.player
        if(
            !controller.hasArmedAoEPotion(
                player.uniqueId
            )
        ) return

        if(
            event.action!=
                Action.RIGHT_CLICK_AIR &&
            event.action!=
                Action.RIGHT_CLICK_BLOCK
        ) return

        event.isCancelled=true

        val target=
            event.clickedBlock
                ?.location
                ?.clone()
                ?.add(0.5,1.0,0.5)
                ?: player.getTargetBlockExact(
                    30
                )?.location
                    ?.clone()
                    ?.add(0.5,1.0,0.5)
                ?: player.eyeLocation
                    .clone()
                    .add(
                        player.eyeLocation
                            .direction
                            .multiply(12.0)
                    )

        runCatching {
            controller.commitArmedAoEPotion(
                player.uniqueId,
                target.x,
                target.y,
                target.z
            )
        }.onSuccess { report ->
            renderTargetRing(
                target,
                report.radiusBlocks
            )
            player.sendMessage(
                "AoE " + report.potionId +
                    " committed: targets=" +
                    report.candidateCount +
                    ", pulses=" +
                    report.scheduledPulseCount +
                    ", cooldownReadyAt=" +
                    report.cooldownReadyAtTick
            )
        }.onFailure { error ->
            player.sendMessage(
                "AoE target ERROR: " +
                    error.javaClass.simpleName +
                    ": " +
                    error.message
            )
        }
    }

    private fun renderTargetRing(
        centre: org.bukkit.Location,
        radius: Double
    ) {
        val samples=32
        repeat(samples) { index ->
            val angle=
                2.0 * PI *
                    index.toDouble() /
                    samples.toDouble()
            centre.world.spawnParticle(
                Particle.END_ROD,
                centre.x +
                    cos(angle)*radius,
                centre.y + 0.15,
                centre.z +
                    sin(angle)*radius,
                1,
                0.0,0.0,0.0,
                0.0
            )
        }
    }
}
