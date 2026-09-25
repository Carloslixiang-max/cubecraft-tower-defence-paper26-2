package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.tower.TowerInstanceId
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.plugin.Plugin
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class BukkitTowerRangefinderView(
    val towerInstanceId: TowerInstanceId,
    val centre: Vec3,
    val radiusBlocks: Double
) {
    init {
        require(radiusBlocks >= 0.0)
    }
}

/**
 * Engineering adapter for the evidence-backed rangefinder semantics.
 *
 * Core decides WHICH tower ranges are visible. This class only projects those
 * ranges into Paper as a lightweight particle ring. The particle choice and
 * sampling density are Engineering Playtest presentation, not original truth.
 */
class BukkitEngineeringRangefinderService(
    private val plugin: Plugin,
    private val controller:
        BukkitNormalArenaController
) {
    init {
        plugin.server.scheduler
            .runTaskTimer(
                plugin,
                Runnable(::renderActivePlayers),
                10L,
                10L
            )
    }

    private fun renderActivePlayers() {
        plugin.server.onlinePlayers
            .forEach { player ->
                if(
                    !controller.isActivePlayer(
                        player.uniqueId
                    )
                ) return@forEach

                val target=
                    player.getTargetBlockExact(8)
                val hovered=
                    target?.let {
                        controller.towerAt(
                            player.uniqueId,
                            BlockPos(
                                it.x,it.y,it.z
                            )
                        )
                    }

                val p=player.location
                val views=
                    controller
                        .rangefinderViewsForPlayer(
                            player.uniqueId,
                            hovered,
                            player.isSneaking,
                            p.x,p.y,p.z
                        )

                views.forEach { view ->
                    renderRing(
                        player,
                        view
                    )
                }
            }
    }

    private fun renderRing(
        player: org.bukkit.entity.Player,
        view: BukkitTowerRangefinderView
    ) {
        val samples=32
        repeat(samples) { index ->
            val angle=
                2.0 * PI *
                    index.toDouble() /
                    samples.toDouble()
            val x=
                view.centre.x +
                    cos(angle) *
                    view.radiusBlocks
            val z=
                view.centre.z +
                    sin(angle) *
                    view.radiusBlocks
            val location=
                Location(
                    player.world,
                    x,
                    view.centre.y + 0.15,
                    z
                )
            player.spawnParticle(
                Particle.END_ROD,
                location,
                1
            )
        }
    }
}
