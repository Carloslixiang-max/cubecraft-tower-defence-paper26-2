package dev.cubecrafttd.tower

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.tower.visual.TowerPath
import java.util.UUID

@JvmInline
value class TowerInstanceId(val value: Long)

data class TowerIdentity(
    val instanceId: TowerInstanceId,
    val towerId: String,
    val ownerUuid: UUID,
    val team: TeamId
)

data class TowerGeometryState(
    val baseOrigin: Vec3,
    val rangeOrigin: Vec3,
    val firingOrigins: List<Vec3>,
    val bodyRevision: String
)

data class TowerUpgradeState(
    var level: Int,
    var path: TowerPath?
)

data class TowerCooldownState(
    var nextAttackTick: Long = 0L
)

enum class TowerLifecycleState {
    ACTIVE,
    MUTATING,
    REMOVING
}

data class TowerInvestmentState(
    var currentGameCoinsInvested: Long
) {
    init { require(currentGameCoinsInvested >= 0L) }
}

data class TowerRuntimeState(
    val identity: TowerIdentity,
    val upgrade: TowerUpgradeState,
    var geometry: TowerGeometryState,
    val cooldown: TowerCooldownState = TowerCooldownState(),
    val investment: TowerInvestmentState = TowerInvestmentState(0L),
    var lifecycle: TowerLifecycleState = TowerLifecycleState.ACTIVE
)
