package dev.cubecrafttd.mob

import dev.cubecrafttd.arena.TeamId
import java.util.UUID
import dev.cubecrafttd.status.StatusEffectSet
import dev.cubecrafttd.status.RecommendedMatureStatusPolicies

@JvmInline
value class MobInstanceId(val value: Long)

enum class MobLifecycleState {
    QUEUED,
    MOVING,
    ATTACKING_CASTLE,
    /**
     * Boss participates in arena combat/targeting but is moved by its
     * Armageddon runtime instead of MobMovementTickPhase.
     */
    ARMAGEDDON_BOSS,
    DEAD,
    REMOVED
}

data class MobIdentity(
    val instanceId: MobInstanceId,
    val entityUuid: UUID,
    val mobId: String,
    val senderPlayerUuid: UUID,
    val attackedTeam: TeamId,
    val level: Int
)

data class MobRouteState(
    val routeId: String,
    val segmentIndex: Int,
    val segmentProgress: Double,
    val routeProgress: Double
)

data class MobCombatState(
    var health: Double,
    var maxHealth: Double,
    var lifecycle: MobLifecycleState,
    var lastEligibleDamageSource: DamageSourceIdentity? = null
)

sealed interface DamageSourceIdentity {
    data class PlayerTower(val ownerUuid: UUID, val towerInstanceId: Long) : DamageSourceIdentity
    data class PlayerSword(val playerUuid: UUID) : DamageSourceIdentity
    data class PlayerBow(val playerUuid: UUID) : DamageSourceIdentity
    data class PlayerPotion(
        val playerUuid: UUID,
        val potionId: String,
        val awardsPlayerKillCoins: Boolean
    ) : DamageSourceIdentity
    data class CastleGuard(val defendingTeam: TeamId) : DamageSourceIdentity
    data class System(val reason: String) : DamageSourceIdentity
}

data class MobRuntimeState(
    val identity: MobIdentity,
    var route: MobRouteState,
    val combat: MobCombatState,
    var form: MobFormState =
        RecommendedMatureMobFormResolver.initialForm(
            identity.mobId,
            identity.level
        ),
    val statusEffects: StatusEffectSet =
        StatusEffectSet(
            RecommendedMatureStatusPolicies.policies
        )
)
