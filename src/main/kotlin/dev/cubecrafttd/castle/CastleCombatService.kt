package dev.cubecrafttd.castle

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.mob.MobLifecycleState
import dev.cubecrafttd.mob.MobRuntimeState

/**
 * Pure state transition. No default interval is embedded here.
 */
class CastleCombatService(
    private val castles: MutableMap<TeamId, CastleRuntime>
) {
    fun enterCastleAttackState(mob: MobRuntimeState) {
        check(mob.combat.lifecycle == MobLifecycleState.MOVING) {
            "Only MOVING mobs may enter ATTACKING_CASTLE"
        }
        mob.combat.lifecycle = MobLifecycleState.ATTACKING_CASTLE
    }

    fun applyCastleHit(attackingMob: MobRuntimeState, castleDamage: Double): CastleRuntime {
        check(attackingMob.combat.lifecycle == MobLifecycleState.ATTACKING_CASTLE)
        require(castleDamage >= 0.0)
        val castle = castles[attackingMob.identity.attackedTeam]
            ?: error("No castle runtime for ${attackingMob.identity.attackedTeam}")
        castle.damage(castleDamage)
        return castle
    }
}
