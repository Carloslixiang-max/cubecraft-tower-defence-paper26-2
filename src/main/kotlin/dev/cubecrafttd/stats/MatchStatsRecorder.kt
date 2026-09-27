package dev.cubecrafttd.stats

import dev.cubecrafttd.match.MatchOutcome
import java.util.UUID

data class MatchPlayerStats(
    var coinsEarned: Long = 0,
    var expEarned: Long = 0,
    var troopsSent: Int = 0,
    var troopsSentCumulativeCost: Long = 0,
    var troopsKilled: Int = 0,
    var troopsKilledCumulativeCost: Long = 0,
    var towersBuilt: Int = 0,
    var towersBuiltCumulativeCost: Long = 0,
    var towersSold: Int = 0,
    var castleDamageDone: Double = 0.0,
    var castleDamageTaken: Double = 0.0,
    var win: Int = 0,
    var loss: Int = 0,
    var draw: Int = 0
)

data class MatchStatsSnapshot(
    val byPlayer:
        Map<UUID,MatchPlayerStats>
)

class MatchStatsRecorder {
    private val stats=
        linkedMapOf<UUID,MatchPlayerStats>()

    fun forPlayer(
        playerUuid: UUID
    ): MatchPlayerStats =
        stats.getOrPut(playerUuid) {
            MatchPlayerStats()
        }

    fun recordCoinsEarned(
        playerUuid: UUID,
        amount: Long
    ) {
        require(amount>=0L)
        forPlayer(playerUuid)
            .coinsEarned += amount
    }

    fun recordExpEarned(
        playerUuid: UUID,
        amount: Long
    ) {
        require(amount>=0L)
        forPlayer(playerUuid)
            .expEarned += amount
    }

    fun recordTroopsSent(
        playerUuid: UUID,
        amount: Int,
        cumulativeCost: Long = 0L
    ) {
        require(amount>=0)
        require(cumulativeCost>=0L)
        forPlayer(playerUuid)
            .also {
                it.troopsSent += amount
                it.troopsSentCumulativeCost +=
                    cumulativeCost
            }
    }

    fun recordTroopKill(
        playerUuid: UUID,
        cumulativeCost: Long = 0L
    ) {
        require(cumulativeCost>=0L)
        forPlayer(playerUuid)
            .also {
                it.troopsKilled++
                it.troopsKilledCumulativeCost +=
                    cumulativeCost
            }
    }

    fun recordTowerBuilt(
        playerUuid: UUID,
        cumulativeCost: Long = 0L
    ) {
        require(cumulativeCost>=0L)
        forPlayer(playerUuid)
            .also {
                it.towersBuilt++
                it.towersBuiltCumulativeCost +=
                    cumulativeCost
            }
    }

    fun recordTowerSold(
        playerUuid: UUID
    ) {
        forPlayer(playerUuid)
            .towersSold++
    }

    fun recordCastleDamageDone(
        playerUuid: UUID,
        amount: Double
    ) {
        require(amount>=0.0)
        forPlayer(playerUuid)
            .castleDamageDone += amount
    }

    fun recordCastleDamageTaken(
        playerUuid: UUID,
        amount: Double
    ) {
        require(amount>=0.0)
        forPlayer(playerUuid)
            .castleDamageTaken += amount
    }

    fun recordOutcome(
        playerUuid: UUID,
        outcome: MatchOutcome,
        playerWon: Boolean?
    ) {
        val s=forPlayer(playerUuid)
        when {
            outcome is MatchOutcome.Tie -> s.draw++
            playerWon == true -> s.win++
            playerWon == false -> s.loss++
            else -> Unit
        }
    }

    fun snapshot(): MatchStatsSnapshot =
        MatchStatsSnapshot(
            stats.mapValues {
                (_,v) -> v.copy()
            }
        )
}
