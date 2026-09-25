package dev.cubecrafttd.combat

import dev.cubecrafttd.mob.DamageSourceIdentity
import java.util.UUID

enum class MatureKillCreditKind {
    PLAYER_TOWER,
    PLAYER_SWORD,
    PLAYER_BOW,
    PLAYER_POTION,
    CASTLE_GUARD,
    SYSTEM_OR_UNATTRIBUTED
}

data class KillAttributionDecision(
    val kind: MatureKillCreditKind,
    val creditedPlayerUuid: UUID?,
    val awardsPlayerKillCoins: Boolean
)

object KillAttributionService {
    /**
     * RECOMMENDED_MATURE policy: final finishing blow gets credit.
     * The exact Coins amount remains a separate TruthGate field.
     */
    fun matureFinalBlow(source: DamageSourceIdentity?): KillAttributionDecision = when (source) {
        is DamageSourceIdentity.PlayerTower -> KillAttributionDecision(
            MatureKillCreditKind.PLAYER_TOWER, source.ownerUuid, true
        )
        is DamageSourceIdentity.PlayerSword -> KillAttributionDecision(
            MatureKillCreditKind.PLAYER_SWORD, source.playerUuid, true
        )
        is DamageSourceIdentity.PlayerBow -> KillAttributionDecision(
            MatureKillCreditKind.PLAYER_BOW, source.playerUuid, true
        )
        is DamageSourceIdentity.PlayerPotion -> KillAttributionDecision(
            MatureKillCreditKind.PLAYER_POTION,
            source.playerUuid,
            source.awardsPlayerKillCoins
        )
        is DamageSourceIdentity.CastleGuard -> KillAttributionDecision(
            MatureKillCreditKind.CASTLE_GUARD, null, false
        )
        is DamageSourceIdentity.System, null -> KillAttributionDecision(
            MatureKillCreditKind.SYSTEM_OR_UNATTRIBUTED, null, false
        )
    }
}
