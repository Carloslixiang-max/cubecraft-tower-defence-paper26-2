package dev.cubecrafttd.mob

import java.util.UUID

data class PlayerMobDamageEvent(
    val targetMobUuid: UUID,
    val playerUuid: UUID,
    val amount: Double
) {
    init { require(amount >= 0.0) }
}

fun interface PlayerMobDamageEventPort {
    fun record(event: PlayerMobDamageEvent)
}

object NoOpPlayerMobDamageEventPort : PlayerMobDamageEventPort {
    override fun record(event: PlayerMobDamageEvent) = Unit
}

fun DamageSourceIdentity.playerUuidOrNull(): UUID? =
    when(this) {
        is DamageSourceIdentity.PlayerTower -> ownerUuid
        is DamageSourceIdentity.PlayerSword -> playerUuid
        is DamageSourceIdentity.PlayerBow -> playerUuid
        is DamageSourceIdentity.PlayerPotion -> playerUuid
        is DamageSourceIdentity.CastleGuard,
        is DamageSourceIdentity.System -> null
    }
