package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.tower.TowerRuntimeState
import dev.cubecrafttd.tower.visual.TowerBodyDefinitionProvider
import dev.cubecrafttd.tower.visual.TowerPath

/** Inferred visual firing point, not recovered original raytrace coordinates. */
class BukkitTowerVisualOrigins(private val models: TowerBodyDefinitionProvider) {
    private val heights = hashMapOf<String, Double>()

    fun origin(tower: TowerRuntimeState): Vec3 {
        val body = models.definition(tower.identity.towerId, tower.upgrade.level,
            tower.upgrade.path ?: TowerPath.TOP)
        val height = heights.getOrPut(body.bodyRevision) { body.blocks.maxOf { it.pos.y } + 2.25 }
        val base = tower.geometry.baseOrigin
        return Vec3(base.x + 0.5, base.y + height, base.z + 0.5)
    }
}
