package dev.cubecrafttd.economy

import dev.cubecrafttd.mob.MobDefinitionRepository
import dev.cubecrafttd.mob.MobRuntimeState
import java.util.UUID

enum class PricingMode {
    NORMAL,
    DOUBLE_INCOME,
    QUICK_START
}

object NormalModeConstants {
    /**
     * Mature community guide evidence (2020-2021 era):
     * both sending troops and throwing AoE potions use a 15-second cooldown.
     *
     * The evidence does not prove that both actions share one timer, so the
     * core intentionally keeps them as separate cooldown scopes.
     */
    const val SEND_COOLDOWN_TICKS: Long = 15L * 20L
    const val AOE_POTION_COOLDOWN_TICKS: Long = 15L * 20L
    const val QUEUE_LIMIT: Int = 12
}

@JvmInline
value class CooldownKey(val value: String)

class DeterministicCooldownTracker {
    private val readyAt = linkedMapOf<CooldownKey, Long>()

    fun readyAtTick(key: CooldownKey): Long = readyAt[key] ?: 0L
    fun remainingTicks(key: CooldownKey, gameTick: Long): Long =
        (readyAtTick(key) - gameTick).coerceAtLeast(0L)

    fun isReady(key: CooldownKey, gameTick: Long): Boolean =
        gameTick >= readyAtTick(key)

    fun consume(key: CooldownKey, gameTick: Long, cooldownTicks: Long): Boolean {
        require(cooldownTicks >= 0)
        if (!isReady(key, gameTick)) return false
        readyAt[key] = gameTick + cooldownTicks
        return true
    }
}

class SentMobExpRewardService(
    private val definitions: MobDefinitionRepository,
    private val ledger: EconomyLedger,
    private val pricingMode: PricingMode
) {
    fun onSentMobDeath(mob: MobRuntimeState, arenaTick: Long): Long {
        val def = definitions.get(mob.identity.mobId).level(mob.identity.level)
        val multiplier = if (pricingMode == PricingMode.DOUBLE_INCOME) 2L else 1L
        val amount = def.expRewardOnDeath * multiplier
        val sender = mob.identity.senderPlayerUuid

        ledger.apply(
            EconomyTransaction(
                transactionId = mob.identity.instanceId.value xor arenaTick,
                arenaTick = arenaTick,
                account = EconomyAccount(
                    team = opposite(mob.identity.attackedTeam),
                    playerUuid = sender,
                    currency = EconomyCurrency.MATCH_EXP
                ),
                delta = amount,
                reason = EconomyReason.TROOP_SEND_REWARD,
                correlationId = "sent-mob-exp:${mob.identity.instanceId.value}",
                truthFieldKey = "troops.${mob.identity.mobId}.level${mob.identity.level}.exp_reward"
            )
        )
        return amount
    }

    private fun opposite(team: dev.cubecrafttd.arena.TeamId): dev.cubecrafttd.arena.TeamId =
        when (team) {
            dev.cubecrafttd.arena.TeamId.RED -> dev.cubecrafttd.arena.TeamId.BLUE
            dev.cubecrafttd.arena.TeamId.BLUE -> dev.cubecrafttd.arena.TeamId.RED
        }
}
