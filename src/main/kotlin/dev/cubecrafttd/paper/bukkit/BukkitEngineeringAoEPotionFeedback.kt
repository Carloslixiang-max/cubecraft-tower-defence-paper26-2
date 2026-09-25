package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.ArenaContext
import dev.cubecrafttd.player.*
import org.bukkit.Particle
import org.bukkit.Server

/**
 * Engineering-only visual projection for committed AoE pulses.
 * Particle style is not claimed as original CubeCraft fidelity.
 */
class BukkitEngineeringAoEPotionFeedback(
    private val server: Server
) : AoEPotionFeedbackPort {
    override fun onPulse(
        context: ArenaContext,
        scheduled:
            ScheduledAoEPotionPulse,
        report:
            AoEPotionPulseApplicationReport
    ) {
        scheduled.pulse
            .targetMobUuids
            .distinct()
            .take(12)
            .forEach { uuid ->
                val entity=
                    server.getEntity(uuid)
                        ?: return@forEach
                if(entity.world.uid!=context.worldUid)
                    return@forEach

                entity.world.spawnParticle(
                    Particle.END_ROD,
                    entity.location
                        .clone()
                        .add(0.0,1.0,0.0),
                    3,
                    0.25,0.25,0.25,
                    0.0
                )
            }
    }
}
