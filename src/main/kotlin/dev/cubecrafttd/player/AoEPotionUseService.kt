package dev.cubecrafttd.player

import dev.cubecrafttd.arena.ArenaEntityIndex
import dev.cubecrafttd.ui.AoEPotionDefinition
import java.util.UUID

data class AoEPotionUseReceipt(
    val actionPlan: AoEPotionActionPlan,
    val cooldownReadyAtTick: Long
)

class AoEPotionUseService(
    private val cooldown:
        AoEPotionCooldownGate
) {
    fun commit(
        definition: AoEPotionDefinition,
        config: AoEPotionRuntimeConfig,
        ownerUuid: UUID,
        gameTick: Long,
        candidates:
            Collection<AoEPotionTargetCandidate>,
        index: ArenaEntityIndex
    ): AoEPotionUseReceipt {
        check(
            cooldown.isReady(
                ownerUuid,
                gameTick
            )
        ) {
            "AoE potion cooldown active for $ownerUuid"
        }

        // Planning performs all type/target/runtime-config validation first.
        // If planning throws, the 15-second cooldown is NOT consumed.
        val plan =
            AoEPotionRuntimePlanner.plan(
                definition,
                config,
                ownerUuid,
                gameTick,
                candidates,
                index
            )

        val cd =
            cooldown.consumeSuccessfulThrow(
                ownerUuid,
                gameTick
            )

        return AoEPotionUseReceipt(
            plan,
            cd.readyAtTick
        )
    }
}
