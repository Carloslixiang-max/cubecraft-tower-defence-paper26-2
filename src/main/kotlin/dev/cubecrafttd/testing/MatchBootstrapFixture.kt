package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.match.*
import dev.cubecrafttd.player.*
import dev.cubecrafttd.progression.*
import dev.cubecrafttd.truth.*
import java.util.UUID

object MatchBootstrapFixture {
    private fun snapshot(
        uuid: UUID
    )=PlayerSnapshot(
        uuid,0,
        byteArrayOf(1),
        "SURVIVAL",
        byteArrayOf(2),
        byteArrayOf(3),
        byteArrayOf(4),
        0,0f,0,
        20.0,0.0,
        20,5f,0f,
        0,300,
        false,false,0f,
        byteArrayOf(5),
        byteArrayOf(6)
    )

    fun run(): List<FixtureResult> {
        val red=UUID.fromString(
            "00000000-0000-0000-0000-000000012001"
        )
        val blue=UUID.fromString(
            "00000000-0000-0000-0000-000000012002"
        )
        val prepared=
            linkedSetOf<UUID>()
        val restored=
            linkedSetOf<UUID>()
        val adapter=
            object: PlayerStateAdapter {
                override fun capture(
                    playerUuid: UUID,
                    arenaTick: Long
                )=snapshot(playerUuid)

                override fun prepareForMatch(
                    playerUuid: UUID
                ) {
                    prepared += playerUuid
                }

                override fun restore(
                    snapshot: PlayerSnapshot
                ) {
                    restored +=
                        snapshot.playerUuid
                }

                override fun isOnline(
                    playerUuid: UUID
                )=true
            }
        val recovery=
            PlayerRecoveryOrchestrator(
                adapter,
                PlayerSnapshotStore()
            )
        val ledger=EconomyLedger()
        val progression=
            TroopProgressionService(ledger)
        val context=ArenaContext(
            ArenaId("bootstrap"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000012100"
            ),
            TestingMapFactory.minimal()
        )
        val runtime=
            NormalArenaRuntimeState
                .withCadence(
                    dev.cubecrafttd.troop
                        .TroopSpawnCadence(
                            20,
                            "fixture"
                        )
                )
        val preset=
            MatchRulePresetFactory
                .recommendedMature(
                    MatchMode.QUICK_START,
                    ResolvedTruth(
                        500L,
                        ResolutionSource
                            .ENGINEERING_FALLBACK
                    ),
                    ResolvedTruth(
                        0L,
                        ResolutionSource
                            .OBSERVED_ORIGINAL
                    ),
                    ProgressionMode
                        .CLASSIC_PROGRESSION
                )

        val service=
            MatchBootstrapService(
                recovery,ledger,progression
            )
        val report=service.prepare(
            context,runtime,
            MatchBootstrapRequest(
                setOf(red),setOf(blue),
                preset,
                firstGoldmineIncomeTick=20,
                economyTransactionBaseId=100
            )
        )

        val redCoins=ledger.balance(
            EconomyAccount(
                TeamId.RED,red,
                EconomyCurrency.MATCH_COINS
            )
        )
        val redExp=ledger.balance(
            EconomyAccount(
                TeamId.RED,red,
                EconomyCurrency.MATCH_EXP
            )
        )
        val blueCoins=ledger.balance(
            EconomyAccount(
                TeamId.BLUE,blue,
                EconomyCurrency.MATCH_COINS
            )
        )

        val beforeStart=
            context.state==
                ArenaState.PREPARED
        service.start(context)

        return listOf(
            FixtureResult(
                "bootstrap-captures-and-prepares-all-players",
                report.capturedPlayers==
                    setOf(red,blue) &&
                    prepared==setOf(red,blue)
            ),
            FixtureResult(
                "bootstrap-assigns-teams",
                context.redTeam.players==
                    setOf(red) &&
                    context.blueTeam.players==
                    setOf(blue)
            ),
            FixtureResult(
                "bootstrap-quick-start-seeds-economy",
                redCoins==1500L &&
                    blueCoins==1500L &&
                    redExp==100L
            ),
            FixtureResult(
                "bootstrap-creates-goldmine-runtime",
                runtime.goldmines.size==2 &&
                    runtime.goldmines
                        .getValue(red)
                        .level==1 &&
                    runtime.goldmines
                        .getValue(red)
                        .nextIncomeTick==20L
            ),
            FixtureResult(
                "bootstrap-initializes-classic-progression",
                report.session.players
                    .getValue(red)
                    .progression
                    .level("zombie")==1 &&
                    report.session.players
                    .getValue(red)
                    .progression
                    .level("giant")==0
            ),
            FixtureResult(
                "bootstrap-prepared-then-running",
                beforeStart &&
                    context.state==
                        ArenaState.RUNNING
            )
        )
    }
}
