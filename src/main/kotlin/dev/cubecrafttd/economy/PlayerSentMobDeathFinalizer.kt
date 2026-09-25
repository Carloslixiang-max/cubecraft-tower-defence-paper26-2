package dev.cubecrafttd.economy

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.mob.*
import java.util.UUID

/**
 * Awards the documented sent-mob EXP only when the sender UUID belongs to the
 * attacking player team for this arena.
 *
 * System-owned Armageddon mobs therefore never receive player progression EXP.
 */
class PlayerSentMobDeathFinalizer(
    private val reward:
        SentMobExpRewardService
) : MobDeathFinalizationPort {
    override fun finalize(
        context: ArenaContext,
        mob: MobRuntimeState
    ) {
        val attackingTeam=
            opposite(
                mob.identity.attackedTeam
            )
        val sender=
            mob.identity.senderPlayerUuid

        val attackingPlayers =
            when(attackingTeam) {
                TeamId.RED ->
                    context.redTeam.players
                TeamId.BLUE ->
                    context.blueTeam.players
            }

        if(!attackingPlayers.contains(sender)) {
            return
        }

        reward.onSentMobDeath(
            mob,
            context.gameTick
        )
    }

    private fun opposite(
        team: TeamId
    ): TeamId =
        when(team) {
            TeamId.RED -> TeamId.BLUE
            TeamId.BLUE -> TeamId.RED
        }
}
