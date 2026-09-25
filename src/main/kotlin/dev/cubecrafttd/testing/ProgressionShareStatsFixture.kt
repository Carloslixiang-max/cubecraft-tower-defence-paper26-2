package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.match.*
import dev.cubecrafttd.progression.*
import dev.cubecrafttd.stats.*
import java.util.UUID

object ProgressionShareStatsFixture {
    fun run(): List<FixtureResult> {
        val p1=UUID.fromString(
            "00000000-0000-0000-0000-000000006001"
        )
        val p2=UUID.fromString(
            "00000000-0000-0000-0000-000000006002"
        )
        val ledger=EconomyLedger()
        val exp=EconomyAccount(
            TeamId.RED,p1,
            EconomyCurrency.MATCH_EXP
        )
        val coins1=EconomyAccount(
            TeamId.RED,p1,
            EconomyCurrency.MATCH_COINS
        )
        val coins2=EconomyAccount(
            TeamId.RED,p2,
            EconomyCurrency.MATCH_COINS
        )
        ledger.apply(
            EconomyTransaction(
                1,0,exp,1000,
                EconomyReason.MATCH_INITIALIZATION,
                "seed-exp"
            )
        )
        ledger.apply(
            EconomyTransaction(
                2,0,coins1,500,
                EconomyReason.MATCH_INITIALIZATION,
                "seed-c1"
            )
        )
        ledger.apply(
            EconomyTransaction(
                3,0,coins2,50,
                EconomyReason.MATCH_INITIALIZATION,
                "seed-c2"
            )
        )

        val state=TroopProgressionState()
        val progression=
            TroopProgressionService(ledger)
        progression.initialize(state)
        val initialZombie=state.level("zombie")
        val initialSpider=state.level("spider")

        val unlock=
            progression.unlockOrUpgrade(
                state,TeamId.RED,p1,
                "spider",100,4,
                "unlock-spider-1"
            )
        val mayAt299=
            progression.mayRollback(
                p1,"spider",299
            )
        val refunded=
            progression.rollbackLast(
                state,TeamId.RED,p1,
                "spider",300,5,
                "rollback-spider-1"
            )

        progression.unlockOrUpgrade(
            state,TeamId.RED,p1,
            "spider",400,6,
            "unlock-spider-2"
        )
        val expired=try {
            progression.rollbackLast(
                state,TeamId.RED,p1,
                "spider",601,7,
                "rollback-expired"
            )
            false
        } catch (_: IllegalStateException) {
            true
        }

        val share=TeamShareService(ledger)
        val receipt=share.shareCoins(
            TeamId.RED,setOf(p1,p2),
            p1,p2,125,
            700,10,"share-1"
        )

        val crossTeamBlocked=try {
            share.shareCoins(
                TeamId.RED,setOf(p1),
                p1,p2,1,
                701,20,"share-bad"
            )
            false
        } catch (_: IllegalStateException) {
            true
        }

        val stats=MatchStatsRecorder()
        stats.recordCoinsEarned(p1,15)
        stats.recordExpEarned(p1,7)
        stats.recordTroopsSent(p1,12)
        stats.recordTroopKill(p1)
        stats.recordTowerBuilt(p1)
        stats.recordTowerSold(p1)
        stats.recordCastleDamageDone(p1,2.5)
        stats.recordOutcome(
            p1,
            MatchOutcome.Winner(
                TeamId.RED,"fixture"
            ),
            playerWon=true
        )
        val snap=stats.snapshot()
            .byPlayer.getValue(p1)

        return listOf(
            FixtureResult(
                "progression-initial-unlocks-from-zero-cost-level1",
                initialZombie==1 &&
                    initialSpider==0
            ),
            FixtureResult(
                "progression-spider-unlock-cost",
                unlock.newLevel==1 &&
                    unlock.expSpent==100L &&
                    ledger.balance(exp)==900L
            ),
            FixtureResult(
                "progression-10s-full-refund-inclusive-deadline",
                mayAt299 &&
                    refunded==100L
            ),
            FixtureResult(
                "progression-rollback-expired-after-200-ticks",
                expired
            ),
            FixtureResult(
                "team-share-free-transfer",
                receipt.amount==125L &&
                    ledger.balance(coins1)==375L &&
                    ledger.balance(coins2)==175L
            ),
            FixtureResult(
                "team-share-requires-both-same-team-members",
                crossTeamBlocked
            ),
            FixtureResult(
                "match-stats-recording",
                snap.coinsEarned==15L &&
                    snap.expEarned==7L &&
                    snap.troopsSent==12 &&
                    snap.troopsKilled==1 &&
                    snap.towersBuilt==1 &&
                    snap.towersSold==1 &&
                    snap.castleDamageDone==2.5 &&
                    snap.win==1
            )
        )
    }
}
