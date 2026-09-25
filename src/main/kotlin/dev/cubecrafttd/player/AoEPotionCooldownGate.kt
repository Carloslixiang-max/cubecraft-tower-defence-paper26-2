package dev.cubecrafttd.player

import dev.cubecrafttd.economy.CooldownKey
import dev.cubecrafttd.economy.DeterministicCooldownTracker
import dev.cubecrafttd.economy.NormalModeConstants
import java.util.UUID

data class AoEPotionCooldownReceipt(
    val playerUuid: UUID,
    val readyAtTick: Long
)

class AoEPotionCooldownGate(
    private val cooldowns: DeterministicCooldownTracker
) {
    fun key(playerUuid: UUID): CooldownKey =
        CooldownKey("aoe-potion:$playerUuid")

    fun remainingTicks(
        playerUuid: UUID,
        gameTick: Long
    ): Long =
        cooldowns.remainingTicks(
            key(playerUuid),
            gameTick
        )

    fun isReady(
        playerUuid: UUID,
        gameTick: Long
    ): Boolean =
        cooldowns.isReady(
            key(playerUuid),
            gameTick
        )

    /**
     * Call this only after the AoE throw has passed targeting/geometry checks
     * and is actually being committed. Failed/cancelled throws must not consume
     * the cooldown.
     */
    fun consumeSuccessfulThrow(
        playerUuid: UUID,
        gameTick: Long
    ): AoEPotionCooldownReceipt {
        check(
            cooldowns.consume(
                key(playerUuid),
                gameTick,
                NormalModeConstants
                    .AOE_POTION_COOLDOWN_TICKS
            )
        ) {
            "AoE potion cooldown active for $playerUuid"
        }

        return AoEPotionCooldownReceipt(
            playerUuid,
            cooldowns.readyAtTick(
                key(playerUuid)
            )
        )
    }
}
