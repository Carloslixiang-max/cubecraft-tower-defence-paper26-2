package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.combat.*
import dev.cubecrafttd.mob.DamageSourceIdentity
import java.util.UUID

object KillAttributionFixture {
    fun run(): List<FixtureResult> {
        val player = UUID.fromString("00000000-0000-0000-0000-000000000999")
        val tower = KillAttributionService.matureFinalBlow(DamageSourceIdentity.PlayerTower(player, 77))
        val sword = KillAttributionService.matureFinalBlow(DamageSourceIdentity.PlayerSword(player))
        val bow = KillAttributionService.matureFinalBlow(DamageSourceIdentity.PlayerBow(player))
        val guard = KillAttributionService.matureFinalBlow(DamageSourceIdentity.CastleGuard(TeamId.RED))
        return listOf(
            FixtureResult("kill-credit-tower-final-blow", tower.awardsPlayerKillCoins && tower.creditedPlayerUuid == player),
            FixtureResult("kill-credit-sword-final-blow", sword.awardsPlayerKillCoins),
            FixtureResult("kill-credit-bow-final-blow", bow.awardsPlayerKillCoins),
            FixtureResult("kill-credit-guard-zero-player-coins", !guard.awardsPlayerKillCoins && guard.creditedPlayerUuid == null)
        )
    }
}
