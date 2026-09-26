package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.match.*
import dev.cubecrafttd.player.*
import dev.cubecrafttd.progression.*
import dev.cubecrafttd.truth.*
import java.util.UUID

object TeamMatchBootstrapFixture {
    private fun snapshot(
        uuid: UUID,
        tick: Long
    )=PlayerSnapshot(
        uuid,tick,
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
        val red1=UUID.fromString(
            "00000000-0000-0000-0000-000000075001"
        )
        val red2=UUID.fromString(
            "00000000-0000-0000-0000-000000075002"
        )
        val blue1=UUID.fromString(
            "00000000-0000-0000-0000-000000075003"
        )
        val blue2=UUID.fromString(
            "00000000-0000-0000-0000-000000075004"
        )
        val reds=
            linkedSetOf(red1,red2)
        val blues=
            linkedSetOf(blue1,blue2)
        val all=
            reds + blues

        val captured=
            linkedSetOf<UUID>()
        val prepared=
            linkedSetOf<UUID>()
        val restored=
            linkedSetOf<UUID>()
        val adapter=
            object:PlayerStateAdapter {
                override fun capture(
                    playerUuid: UUID,
                    arenaTick: Long
                ):PlayerSnapshot {
                    captured += playerUuid
                    return snapshot(
                        playerUuid,
                        arenaTick
                    )
                }

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
        val context=ArenaContext(
            ArenaId("team-bootstrap"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000075100"
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
                    MatchMode.NORMAL,
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

        val report=
            MatchBootstrapService(
                recovery,
                ledger,
                TroopProgressionService(
                    ledger
                )
            ).prepare(
                context,
                runtime,
                MatchBootstrapRequest(
                    redPlayers=reds,
                    bluePlayers=blues,
                    preset=preset,
                    firstGoldmineIncomeTick=20L,
                    economyTransactionBaseId=7500L
                )
            )

        val allCapturedPrepared=
            captured==all &&
                prepared==all &&
                recovery.pending()==all

        val teamsAndSession=
            context.redTeam.players==
                reds &&
            context.blueTeam.players==
                blues &&
            report.session.players.keys==
                all

        val departure=
            MatchParticipantDepartureService(
                context,
                report.session,
                linkedSetOf()
            ).depart(red1)
        val teammateRemainsActive=
            departure?.newlyDeparted==true &&
                red1 !in
                    report.session.players &&
                red2 in
                    report.session.players &&
                blue1 in
                    report.session.players &&
                blue2 in
                    report.session.players

        all.forEach {
            recovery.restoreIfPossible(it)
        }
        val allRestored=
            restored==all &&
                recovery.pending()
                    .isEmpty()

        return listOf(
            FixtureResult(
                "team-bootstrap-captures-and-prepares-all-2v2-players",
                allCapturedPrepared
            ),
            FixtureResult(
                "team-bootstrap-populates-both-teams-and-four-player-session",
                teamsAndSession
            ),
            FixtureResult(
                "team-bootstrap-departure-keeps-teammates-active-and-restores-all",
                teammateRemainsActive &&
                    allRestored
            )
        )
    }
}
