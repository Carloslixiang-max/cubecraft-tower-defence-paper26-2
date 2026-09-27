package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.stats.MatchStatsRecorder
import dev.cubecrafttd.truth.*
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

object MatchEndCostStatsFixture {
    fun run(): List<FixtureResult> {
        val killer=
            UUID.fromString(
                "00000000-0000-0000-0000-000000080001"
            )
        val sender=
            UUID.fromString(
                "00000000-0000-0000-0000-000000080002"
            )
        val context=
            ArenaContext(
                ArenaId("match-end-cost-stats"),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000080100"
                ),
                TestingMapFactory.minimal()
            ).also {
                it.state=
                    ArenaState.RUNNING
                it.redTeam.players +=
                    killer
                it.blueTeam.players +=
                    sender
                it.gameTick=800L
            }

        val ledger=EconomyLedger()
        val stats=MatchStatsRecorder()
        val finalizer=
            MatchMobDeathFinalizer(
                sentExp=
                    PlayerSentMobDeathFinalizer(
                        SentMobExpRewardService(
                            RecommendedMatureMobDefinitions,
                            ledger,
                            PricingMode.NORMAL
                        )
                    ),
                killRewards=
                    MobKillRewardService(
                        ledger,
                        MobKillRewardResolver {
                            _,_ ->
                            ResolvedTruth(
                                3L,
                                ResolutionSource
                                    .ENGINEERING_FALLBACK
                            )
                        },
                        PricingMode.NORMAL
                    ),
                stats=stats,
                nextTransactionId=
                    AtomicLong(80_000L)
            )

        val mob=
            MobRuntimeState(
                identity=
                    MobIdentity(
                        MobInstanceId(80L),
                        UUID.fromString(
                            "00000000-0000-0000-0000-000000080080"
                        ),
                        "zombie",
                        sender,
                        TeamId.RED,
                        1
                    ),
                route=
                    MobRouteState(
                        "red-route",
                        0,0.0,0.0
                    ),
                combat=
                    MobCombatState(
                        health=0.0,
                        maxHealth=40.0,
                        lifecycle=
                            MobLifecycleState.DEAD,
                        lastEligibleDamageSource=
                            DamageSourceIdentity
                                .PlayerTower(
                                    killer,
                                    8001L
                                )
                    )
            )

        val expectedKillCost=
            RecommendedMatureMobDefinitions
                .get("zombie")
                .level(1)
                .sendCoins

        finalizer.finalize(
            context,mob
        )
        // A repeated callback must not double-count the historical end-stat
        // value even though economic settlement is independently idempotent.
        finalizer.finalize(
            context,mob
        )

        val snapshot=
            stats.snapshot()
                .byPlayer
                .getValue(killer)

        return listOf(
            FixtureResult(
                "match-end-kill-stats-use-sent-mob-cost-and-finalize-once",
                snapshot.troopsKilled==1 &&
                    snapshot
                        .troopsKilledCumulativeCost==
                        expectedKillCost
            )
        )
    }
}
