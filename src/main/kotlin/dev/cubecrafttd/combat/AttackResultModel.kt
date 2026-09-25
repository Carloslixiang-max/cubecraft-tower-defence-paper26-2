package dev.cubecrafttd.combat

import dev.cubecrafttd.mob.DamageKind
import dev.cubecrafttd.status.StatusEffectInstance
import java.util.UUID

enum class AttackDeliveryKind {
    DIRECT,
    SPLASH,
    DOT,
    SUMMON,
    CHAIN,
    BOUNCE,
    BEAM
}

data class DamageComponent(
    val kind: DamageKind,
    val amount: Double
) {
    init { require(amount >= 0.0) }
}

data class AttackImpact(
    val targetUuid: UUID,
    val delivery: AttackDeliveryKind,
    val damages: List<DamageComponent>,
    val effects: List<StatusEffectInstance> = emptyList(),
    val sourceTowerInstanceId: Long,
    val sourceOwnerUuid: UUID?
)

data class AttackBatch(
    val primaryTargetUuid: UUID,
    val impacts: List<AttackImpact>,
    val metadata: Map<String, String> = emptyMap()
)

data class MobHitboxPoint(
    val entityUuid: UUID,
    val x: Double,
    val y: Double,
    val z: Double
)

object SphericalAoeResolver {
    fun withinRadius(
        centerX: Double,
        centerY: Double,
        centerZ: Double,
        radius: Double,
        candidates: Collection<MobHitboxPoint>
    ): List<MobHitboxPoint> {
        require(radius >= 0.0)
        val r2 = radius * radius
        return candidates.filter {
            val dx = it.x - centerX
            val dy = it.y - centerY
            val dz = it.z - centerZ
            dx*dx + dy*dy + dz*dz <= r2 + 1e-12
        }.sortedBy { it.entityUuid.toString() }
    }
}
