package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.ArenaContext
import dev.cubecrafttd.tower.TowerAttackCycleReport
import dev.cubecrafttd.tower.TowerAttackFeedbackPort
import dev.cubecrafttd.tower.TowerRuntimeState
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.Server
import kotlin.math.max

/**
 * Engineering-only combat feedback.
 *
 * Core combat remains authoritative. This adapter observes completed attacks
 * and projects a lightweight particle trace so playtesters can see which tower
 * fired at which live mob. Particle style/density are not original truth.
 */
class BukkitEngineeringTowerAttackFeedback(
    private val server: Server
) : TowerAttackFeedbackPort {
    override fun onAttack(
        context: ArenaContext,
        tower: TowerRuntimeState,
        report: TowerAttackCycleReport
    ) {
        val origin=
            tower.geometry.firingOrigins
                .firstOrNull()
                ?: tower.geometry.rangeOrigin

        val targets=
            report.application
                ?.impacts
                ?.asSequence()
                ?.filter {
                    it.damageApplied > 0.0 ||
                        it.effectsApplied.isNotEmpty()
                }
                ?.map { it.targetUuid }
                ?.distinct()
                ?.take(5)
                ?.toList()
                .orEmpty()

        targets.forEach { uuid ->
            val entity=
                server.getEntity(uuid)
                    ?: return@forEach
            val world=entity.world
            val end=entity.location
            val start=
                Location(
                    world,
                    origin.x,
                    origin.y,
                    origin.z
                )
            renderTrace(
                start,
                end
            )
        }
    }

    private fun renderTrace(
        start: Location,
        end: Location
    ) {
        if(start.world.uid != end.world.uid)
            return

        val dx=end.x-start.x
        val dy=end.y-start.y
        val dz=end.z-start.z
        val distance=
            start.distance(end)
        val samples=
            max(
                3,
                (distance*2.0)
                    .toInt()
                    .coerceAtMost(16)
            )

        repeat(samples+1) { index ->
            val t=
                index.toDouble() /
                    samples.toDouble()
            start.world.spawnParticle(
                Particle.END_ROD,
                start.x + dx*t,
                start.y + dy*t,
                start.z + dz*t,
                1,
                0.0,0.0,0.0,
                0.0
            )
        }
    }
}
