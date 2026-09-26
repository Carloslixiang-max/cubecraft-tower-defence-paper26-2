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
    var failNextDelete=false

    override fun save(
        snapshot: PlayerSnapshot
    ) {
        saved[snapshot.playerUuid] =
            snapshot
    }

    override fun delete(
        playerUuid: UUID
    ) {
        if(failNextDelete) {
            failNextDelete=false
            error(
                "synthetic journal delete failure"
            )
        }
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

        val restartJournal=
            RecordingRecoveryJournal()
        restartJournal.save(
            snapshot(player)
        )
        var restartCurrent=
            snapshot(player)
        val restartAdapter=
            object: PlayerStateAdapter {
                override fun capture(
                    playerUuid: UUID,
                    arenaTick: Long
                ): PlayerSnapshot =
                    restartCurrent.copy(
                        capturedAtArenaTick=
                            arenaTick
                    )

                override fun prepareForMatch(
                    playerUuid: UUID
                ) = Unit

                override fun restore(
                    snapshot: PlayerSnapshot
                ) {
                    restartCurrent=snapshot
                }

                override fun isOnline(
                    playerUuid: UUID
                )=true
            }
        val restartRecovery=
            JournaledPlayerRecoveryOrchestrator(
                restartAdapter,
                PlayerSnapshotStore(),
                restartJournal
            )
        val loadedFromJournal=
            restartRecovery
                .recoverJournalIntoMemory()

        var verifierSawSnapshot=false
        val rejectedByVerifier=
            !restartRecovery
                .restoreIfPossible(
                    player
                ) { expected ->
                    verifierSawSnapshot=
                        expected.playerUuid==
                            player
                    false
                }
        val journalRetained=
            player in
                restartJournal.saved &&
                player in
                    restartRecovery
                        .pending()

        val acceptedByVerifier=
            restartRecovery
                .restoreIfPossible(
                    player
                ) { expected ->
                    expected.playerUuid==
                        player
                }

        val prepareFailurePlayer=
            UUID.fromString(
                "00000000-0000-0000-0000-000000013002"
            )
        val prepareFailureJournal=
            RecordingRecoveryJournal()
        var prepareFailureRestoreCount=0
        val prepareFailureRecovery=
            JournaledPlayerRecoveryOrchestrator(
                object: PlayerStateAdapter {
                    override fun capture(
                        playerUuid: UUID,
                        arenaTick: Long
                    )=snapshot(playerUuid)

                    override fun prepareForMatch(
                        playerUuid: UUID
                    ) {
                        error(
                            "synthetic prepare failure"
                        )
                    }

                    override fun restore(
                        snapshot: PlayerSnapshot
                    ) {
                        prepareFailureRestoreCount++
                    }

                    override fun isOnline(
                        playerUuid: UUID
                    )=true
                },
                PlayerSnapshotStore(),
                prepareFailureJournal
            )
        val prepareFailureThrown=
            runCatching {
                prepareFailureRecovery
                    .captureBeforeMatch(
                        prepareFailurePlayer,
                        0
                    )
            }.isFailure
        val prepareFailureRolledBack=
            prepareFailureThrown &&
            prepareFailureRestoreCount==1 &&
            prepareFailurePlayer !in
                prepareFailureRecovery
                    .pending() &&
            prepareFailurePlayer !in
                prepareFailureJournal
                    .saved

        val rollbackFailurePlayer=
            UUID.fromString(
                "00000000-0000-0000-0000-000000013003"
            )
        val rollbackFailureJournal=
            RecordingRecoveryJournal()
        val rollbackFailureRecovery=
            JournaledPlayerRecoveryOrchestrator(
                object: PlayerStateAdapter {
                    override fun capture(
                        playerUuid: UUID,
                        arenaTick: Long
                    )=snapshot(playerUuid)

                    override fun prepareForMatch(
                        playerUuid: UUID
                    ) {
                        error(
                            "synthetic prepare failure"
                        )
                    }

                    override fun restore(
                        snapshot: PlayerSnapshot
                    ) {
                        error(
                            "synthetic rollback failure"
                        )
                    }

                    override fun isOnline(
                        playerUuid: UUID
                    )=true
                },
                PlayerSnapshotStore(),
                rollbackFailureJournal
            )
        val rollbackFailureThrown=
            runCatching {
                rollbackFailureRecovery
                    .captureBeforeMatch(
                        rollbackFailurePlayer,
                        0
                    )
            }.isFailure
        val rollbackFailureRetained=
            rollbackFailureThrown &&
            rollbackFailurePlayer in
                rollbackFailureRecovery
                    .pending() &&
            rollbackFailurePlayer in
                rollbackFailureJournal
                    .saved

        val deleteFailurePlayer=
            UUID.fromString(
                "00000000-0000-0000-0000-000000013004"
            )
        val deleteFailureJournal=
            RecordingRecoveryJournal()
        deleteFailureJournal.save(
            snapshot(
                deleteFailurePlayer
            )
        )
        var deleteFailureRestoreCount=0
        val deleteFailureRecovery=
            JournaledPlayerRecoveryOrchestrator(
                object: PlayerStateAdapter {
                    override fun capture(
                        playerUuid: UUID,
                        arenaTick: Long
                    )=snapshot(playerUuid)

                    override fun prepareForMatch(
                        playerUuid: UUID
                    )=Unit

                    override fun restore(
                        snapshot: PlayerSnapshot
                    ) {
                        deleteFailureRestoreCount++
                    }

                    override fun isOnline(
                        playerUuid: UUID
                    )=true
                },
                PlayerSnapshotStore(),
                deleteFailureJournal
            )
        deleteFailureRecovery
            .recoverJournalIntoMemory()
        deleteFailureJournal
            .failNextDelete=true
        val deleteFailed=
            !deleteFailureRecovery
                .restoreIfPossible(
                    deleteFailurePlayer
                ) { true }
        val deleteFailureStillPending=
            deleteFailed &&
            deleteFailurePlayer in
                deleteFailureRecovery
                    .pending() &&
            deleteFailurePlayer in
                deleteFailureJournal
                    .saved
        val deleteRetrySucceeded=
            deleteFailureRecovery
                .restoreIfPossible(
                    deleteFailurePlayer
                ) { true } &&
            deleteFailurePlayer !in
                deleteFailureRecovery
                    .pending() &&
            deleteFailurePlayer !in
                deleteFailureJournal
                    .saved &&
            deleteFailureRestoreCount>=2

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
            ),
            FixtureResult(
                "restart-recovery-load-reports-previous-process-snapshot",
                loadedFromJournal==
                    setOf(player)
            ),
            FixtureResult(
                "restart-recovery-failed-verification-keeps-journal",
                verifierSawSnapshot &&
                    rejectedByVerifier &&
                    journalRetained
            ),
            FixtureResult(
                "restart-recovery-success-verification-deletes-journal",
                acceptedByVerifier &&
                    player !in
                        restartJournal.saved &&
                    restartRecovery
                        .pending()
                        .isEmpty()
            ),
            FixtureResult(
                "durable-prepare-failure-immediately-restores-and-deletes-journal",
                prepareFailureRolledBack
            ),
            FixtureResult(
                "durable-prepare-rollback-failure-keeps-pending-journal",
                rollbackFailureRetained
            ),
            FixtureResult(
                "durable-journal-delete-failure-keeps-snapshot-pending",
                deleteFailureStillPending
            ),
            FixtureResult(
                "durable-journal-delete-failure-can-retry-safely",
                deleteRetrySucceeded
            )
        )
    }
}
