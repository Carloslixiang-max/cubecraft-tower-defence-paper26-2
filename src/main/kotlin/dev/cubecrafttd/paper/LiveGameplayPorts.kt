package dev.cubecrafttd.paper

import dev.cubecrafttd.map.Vec3
import java.util.UUID

/**
 * Narrow ports required from the eventual Paper 26.2 binding.
 * No Bukkit types leak into the core domain.
 */
fun interface LiveEntityPositionPort {
    fun position(
        entityUuid: UUID
    ): Vec3?
}

fun interface LiveLineOfSightPort {
    fun hasLineOfSight(
        origin: Vec3,
        targetEntityUuid: UUID
    ): Boolean
}

fun interface LiveEntityTeleportPort {
    fun move(
        entityUuid: UUID,
        position: Vec3
    )
}

data class LivePlayerWeaponHit(
    val playerUuid: UUID,
    val targetEntityUuid: UUID,
    val weaponKind: String,
    val finalDamage: Double
)

fun interface LivePlayerWeaponHitPort {
    fun accept(
        hit: LivePlayerWeaponHit
    )
}

data class LiveMenuView(
    val title: String,
    val size: Int,
    val slotActionIds:
        Map<Int,String>,
    val slotDisplayNames:
        Map<Int,String> =
        emptyMap()
)

fun interface LiveMenuOpenPort {
    fun open(
        playerUuid: UUID,
        menu: LiveMenuView
    )
}

fun interface LiveActionBarPort {
    fun send(
        playerUuid: UUID,
        text: String
    )
}

data class PaperIntegrationPortSet(
    val positions: LiveEntityPositionPort,
    val lineOfSight: LiveLineOfSightPort,
    val movement: LiveEntityTeleportPort,
    val weaponHits: LivePlayerWeaponHitPort,
    val menus: LiveMenuOpenPort,
    val actionBar: LiveActionBarPort
)
