package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.castle.*
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.ArmorStand
import org.bukkit.event.entity.CreatureSpawnEvent
import java.util.UUID

/**
 * Geometry-only live anchor for a Castle Guard.
 *
 * The visible Guard build belongs to the map. This invisible marker is not
 * presented as an original CubeCraft entity/model.
 */
class BukkitGuardAnchorSpawnPort(
    private val world: World
) : GuardEntitySpawnPort {
    override fun spawn(
        request: GuardEntitySpawnRequest
    ): UUID {
        val p=request.position
        val stand=world.spawn(
            Location(
                world,p.x,p.y,p.z
            ),
            ArmorStand::class.java,
            CreatureSpawnEvent
                .SpawnReason.CUSTOM
        ) {
            BukkitTrackedEntityTag.mark(
                it
            )
            it.setVisible(false)
            it.setGravity(false)
            it.setMarker(true)
            it.isInvulnerable=true
            it.isSilent=true
            it.setRemoveWhenFarAway(false)
        }
        return stand.uniqueId
    }
}
