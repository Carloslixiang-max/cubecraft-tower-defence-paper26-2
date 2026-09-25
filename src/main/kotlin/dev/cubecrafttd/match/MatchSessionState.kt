package dev.cubecrafttd.match

import dev.cubecrafttd.progression.TroopProgressionState
import dev.cubecrafttd.ui.MatchBazaarState
import dev.cubecrafttd.ui.PlayerMatchInteractionState
import java.util.UUID

data class PlayerMatchSessionState(
    val playerUuid: UUID,
    val progression:
        TroopProgressionState,
    val bazaar:
        MatchBazaarState =
        MatchBazaarState(),
    val interaction:
        PlayerMatchInteractionState =
        PlayerMatchInteractionState()
)

data class MatchSessionState(
    val preset: MatchRulePreset,
    val players:
        MutableMap<UUID,PlayerMatchSessionState> =
        linkedMapOf()
)
