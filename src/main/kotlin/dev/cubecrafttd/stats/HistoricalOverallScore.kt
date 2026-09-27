package dev.cubecrafttd.stats

import dev.cubecrafttd.economy.*
import java.util.UUID

/**
 * Historical CubeCraft Tower Defence end-of-match score.
 *
 * Community evidence from the live Java game identifies Overall score as the
 * amount of Coins spent during the round. Keep the eligible purchase reasons
 * explicit so transfers/admin/fallback transactions cannot silently become
 * leaderboard score when the economy grows.
 */
object HistoricalOverallScoreCalculator {
    val qualifyingReasons: Set<EconomyReason> =
        setOf(
            EconomyReason.TROOP_PURCHASE,
            EconomyReason.TOWER_PURCHASE,
            EconomyReason.TOWER_UPGRADE,
            EconomyReason.BAZAAR
        )

    fun byPlayer(
        ledger: EconomyLedger
    ): Map<UUID,Long> {
        val score=
            linkedMapOf<UUID,Long>()

        ledger.history()
            .forEach { tx ->
                val player=
                    tx.account.playerUuid
                        ?: return@forEach
                if(
                    tx.account.currency !=
                        EconomyCurrency.MATCH_COINS ||
                    tx.delta >= 0L ||
                    tx.reason !in
                        qualifyingReasons
                ) {
                    return@forEach
                }

                score[player]=
                    Math.addExact(
                        score[player] ?: 0L,
                        Math.negateExact(
                            tx.delta
                        )
                    )
            }

        return score
    }

    fun forPlayer(
        ledger: EconomyLedger,
        playerUuid: UUID
    ): Long =
        byPlayer(ledger)[playerUuid]
            ?: 0L
}
