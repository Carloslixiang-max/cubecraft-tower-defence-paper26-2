package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.match.*
import dev.cubecrafttd.player.*
import dev.cubecrafttd.progression.*
import dev.cubecrafttd.recovery.*
import dev.cubecrafttd.truth.*
import java.util.UUID

private class RecordingRecoveryJournal :
    PlayerRecoveryJournal {
    val saved =
        linkedMapOf<UUID,PlayerSnapshot>()
    val deleted =
        linkedSetOf<UUID>()

    override fun save(
        snapshot: PlayerSnapshot
    ) {
        saved[snapshot.playerUuid] =
            snapshot
    }

    override fun delete(
        playerUuid: UUID
    ) {
        deleted += playerUuid
        saved.remove(playerUuid)
    }

    override fun loadAll():
        List<PlayerSnapshot> =
        saved.values.toList()
}

object MatchBootstrapDurableRecoveryFixture {
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
        val player=
            UUID.fromString(
                "00000000-0000-0000-0000-000000013001"
            )
        val events=
            mutableListOf<String>()
        val adapter=
            object: PlayerStateAdapter {
                override fun capture(
                    playerUuid: UUID,
                    arenaTick: Long
                ): PlayerSnapshot {
                    events += "capture"
                    return snapshot(playerUuid)
                }

                override fun prepareForMatch(
                    playerUuid: UUID
                ) {
                    events += "prepare"
                }

                override fun restore(
                    snapshot: PlayerSnapshot
                ) {
                    events += "restore"
                }

                override fun isOnline(
                    playerUuid: UUID
                )=true
            }
        val journal=
            object: PlayerRecoveryJournal {
                val delegate=
                    RecordingRecoveryJournal()

                override fun save(
                    snapshot: PlayerSnapshot
                ) {
                    events += "journal-save"
                    delegate.save(snapshot)
                }

                override fun delete(
                    playerUuid: UUID
                ) {
                    events += "journal-delete"
                    delegate.delete(playerUuid)
                }

                override fun loadAll()=
                    delegate.loadAll()
            }

        val store=
            PlayerSnapshotStore()
        val recovery=
            JournaledPlayerRecoveryOrchestrator(
                adapter,store,journal
            )
        val ledger=EconomyLedger()
        val context=ArenaContext(
            ArenaId("durable-bootstrap"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000013100"
            ),
            TestingMapFactory.minimal()
        )
        val runtime=
            NormalArenaRuntimeState
                .withCadence(
                    dev.cubecrafttd.troop
                        .TroopSpawnCadence(
                            20,"fixture"
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

        MatchBootstrapService(
            recovery,
            ledger,
            TroopProgressionService(
                ledger
            )
        ).prepare(
            context,runtime,
            MatchBootstrapRequest(
                setOf(player),
                emptySet(),
                preset,
                firstGoldmineIncomeTick=20,
                economyTransactionBaseId=1000
            )
        )

        val durableBeforeMutation =
            events.indexOf("journal-save") >= 0 &&
            events.indexOf("prepare") >
                events.indexOf("journal-save")

        val pendingAfterPrepare =
            recovery.pending()==
                setOf(player)

        val restored=
            recovery.restoreIfPossible(
                player
            )
        val deleteAfterRestore =
            events.indexOf("journal-delete") >
                events.indexOf("restore")

        return listOf(
            FixtureResult(
                "bootstrap-durable-save-before-player-mutation",
                durableBeforeMutation
            ),
            FixtureResult(
                "bootstrap-durable-pending-after-prepare",
                pendingAfterPrepare
            ),
            FixtureResult(
                "bootstrap-durable-delete-after-successful-restore",
                restored &&
                    deleteAfterRestore &&
                    recovery.pending()
                        .isEmpty()
            )
        )
    }
}
