package dev.cubecrafttd.player

import dev.cubecrafttd.truth.ResolvedTruth
import dev.cubecrafttd.ui.WeaponKind
import java.util.UUID

data class WeaponAbilityKey(
    val playerUuid: UUID,
    val weapon: WeaponKind,
    val abilityId: String
)

data class WeaponAbilityChargeState(
    var validHits: Int = 0
)

data class WeaponAbilityChargeResult(
    val currentHits: Int,
    val requiredHits: Int,
    val triggered: Boolean
)

class WeaponAbilityChargeTracker {
    private val states =
        linkedMapOf<
            WeaponAbilityKey,
            WeaponAbilityChargeState
        >()

    fun recordValidHit(
        key: WeaponAbilityKey,
        requiredHits:
            ResolvedTruth<Int>
    ): WeaponAbilityChargeResult {
        require(requiredHits.value > 0)
        val state = states.getOrPut(key) {
            WeaponAbilityChargeState()
        }
        state.validHits++
        val triggered =
            state.validHits >=
                requiredHits.value
        if (triggered) {
            state.validHits = 0
        }
        return WeaponAbilityChargeResult(
            currentHits=state.validHits,
            requiredHits=requiredHits.value,
            triggered=triggered
        )
    }

    fun current(
        key: WeaponAbilityKey
    ): Int = states[key]?.validHits ?: 0

    fun clearPlayer(
        playerUuid: UUID
    ) {
        states.keys.removeIf {
            it.playerUuid == playerUuid
        }
    }
}
