package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.castle.*
import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.tower.*
import org.bukkit.Server
import kotlin.math.sqrt

class BukkitTowerMobGeometryProvider(
    private val server: Server,
    private val lineOfSight: BukkitLineOfSightPort
) : MobGeometryProvider {
    override fun geometry(
        tower: TowerRuntimeState,
        mobUuid: java.util.UUID
    ): MobGeometryView? {
        val entity = server.getEntity(mobUuid)
            ?: return null
        val location = entity.location
        val origin = tower.geometry.rangeOrigin
        val dx = location.x-origin.x
        val dy = location.y-origin.y
        val dz = location.z-origin.z
        return MobGeometryView(
            sqrt(dx*dx+dy*dy+dz*dz),
            lineOfSight.hasLineOfSight(
                tower.geometry.firingOrigins
                    .firstOrNull() ?: origin,
                mobUuid
            )
        )
    }
}

class BukkitGuardGeometryProvider(
    private val server: Server,
    private val lineOfSight: BukkitLineOfSightPort
) : GuardGeometryProvider {
    override fun geometry(
        guard: GuardRuntime,
        mobUuid: java.util.UUID
    ): GuardGeometryView? {
        val guardEntity = server.getEntity(
            guard.identity.entityUuid
        ) ?: return null
        val target = server.getEntity(mobUuid)
            ?: return null
        check(guardEntity.world.uid == target.world.uid) {
            "Guard and target are in different worlds"
        }
        val g = guardEntity.location
        val t = target.location
        val dx=t.x-g.x
        val dy=t.y-g.y
        val dz=t.z-g.z
        return GuardGeometryView(
            sqrt(dx*dx+dy*dy+dz*dz),
            lineOfSight.hasLineOfSight(
                Vec3(g.x,g.y,g.z),
                mobUuid
            )
        )
    }
}
