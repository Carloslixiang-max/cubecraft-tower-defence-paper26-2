package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.TrackedEntityRemovalPort
import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.mob.MobPositionUpdatePort
import dev.cubecrafttd.paper.*
import org.bukkit.Location
import org.bukkit.Server
import java.util.UUID

class BukkitEntityRuntimeAdapter(
    private val server: Server
) : PaperEntityAdapter,
    LiveEntityPositionPort,
    LiveEntityTeleportPort,
    MobPositionUpdatePort,
    TrackedEntityRemovalPort,
    TrackedEntityPresencePort {

    override fun remove(uuid: UUID): Boolean {
        val entity = server.getEntity(uuid) ?: return true
        entity.remove()
        return server.getEntity(uuid) == null
    }

    override fun exists(
        entityUuid: UUID
    ): Boolean =
        server.getEntity(entityUuid) != null

    override fun isAlive(uuid: UUID): Boolean {
        val entity = server.getEntity(uuid) ?: return false
        return !entity.isDead && entity.isValid
    }

    override fun currentPosition(
        uuid: UUID
    ): EntityPosition? =
        server.getEntity(uuid)?.location?.let {
            EntityPosition(it.x,it.y,it.z)
        }

    override fun position(
        entityUuid: UUID
    ): Vec3? = server.getEntity(entityUuid)
        ?.location
        ?.let { Vec3(it.x,it.y,it.z) }

    override fun move(
        entityUuid: UUID,
        position: Vec3
    ) {
        val entity = server.getEntity(entityUuid)
            ?: error("Entity $entityUuid is not live")
        val current = entity.location
        val yaw=
            BukkitMovementFacingResolver
                .yawDegrees(
                    current.x,
                    current.z,
                    position.x,
                    position.z,
                    current.yaw
                )
        check(
            entity.teleport(
                Location(
                    current.world,
                    position.x,
                    position.y,
                    position.z,
                    yaw,
                    current.pitch
                )
            )
        ) { "Teleport failed for $entityUuid" }
    }
}
