package dev.cubecrafttd.testing

import dev.cubecrafttd.player.*
import java.util.UUID

private class FakePlayerStateAdapter : PlayerStateAdapter {
    private val online = linkedSetOf<UUID>()
    val restores = mutableMapOf<UUID, Int>()
    var failNextRestore = false
    var failNextPrepare = false

    fun setOnline(uuid: UUID, value: Boolean) {
        if (value) online += uuid else online -= uuid
    }

    override fun capture(playerUuid: UUID, arenaTick: Long): PlayerSnapshot =
        PlayerSnapshot(
            playerUuid, arenaTick,
            byteArrayOf(1), "SURVIVAL", byteArrayOf(2), byteArrayOf(3), byteArrayOf(4),
            7, 0.5f, 99, 20.0, 0.0, 20, 5.0f, 0.0f,
            0, 300, false, false, 0f, byteArrayOf(5), byteArrayOf(6)
        )

    override fun prepareForMatch(
        playerUuid: UUID
    ) {
        if(failNextPrepare) {
            failNextPrepare=false
            error(
                "synthetic prepare failure"
            )
        }
    }

    override fun restore(snapshot: PlayerSnapshot) {
        if (failNextRestore) {
            failNextRestore = false
            error("synthetic restore failure")
        }
        restores[snapshot.playerUuid] = (restores[snapshot.playerUuid] ?: 0) + 1
    }

    override fun isOnline(playerUuid: UUID): Boolean = playerUuid in online
}

object PlayerRecoveryFixture {
    fun run(): List<FixtureResult> {
        val uuid = UUID.fromString("00000000-0000-0000-0000-000000000777")
        val adapter = FakePlayerStateAdapter()
        val store = PlayerSnapshotStore()
        val recovery = PlayerRecoveryOrchestrator(adapter, store)

        adapter.setOnline(uuid, true)
        recovery.captureBeforeMatch(uuid, 42)

        adapter.failNextRestore = true
        val failed = recovery.restoreIfPossible(uuid)
        val retainedAfterFailure = uuid in recovery.pending()

        val success = recovery.restoreIfPossible(uuid)
        val secondAttempt = recovery.restoreIfPossible(uuid)
        val exactlyOnce = adapter.restores[uuid] == 1

        val secondMatchCapture=
            runCatching {
                recovery.captureBeforeMatch(
                    uuid,43
                )
            }.isSuccess
        val secondMatchPending=
            uuid in recovery.pending()
        val secondMatchRestore=
            recovery.restoreIfPossible(
                uuid
            )

        val prepareFailureUuid=
            UUID.fromString(
                "00000000-0000-0000-0000-000000000778"
            )
        adapter.setOnline(
            prepareFailureUuid,
            true
        )
        adapter.failNextPrepare=true
        val prepareFailure=
            runCatching {
                recovery.captureBeforeMatch(
                    prepareFailureUuid,
                    44
                )
            }.isFailure
        val immediatePrepareRollback=
            adapter.restores[
                prepareFailureUuid
            ]==1 &&
            prepareFailureUuid !in
                recovery.pending()

        return listOf(
            FixtureResult("player-restore-failure-retains-snapshot", !failed && retainedAfterFailure),
            FixtureResult("player-restore-retry-success", success),
            FixtureResult("player-restore-exactly-once", !secondAttempt && exactlyOnce),
            FixtureResult(
                "player-recovery-allows-next-match-after-restored-tombstone",
                secondMatchCapture &&
                    secondMatchPending &&
                    secondMatchRestore &&
                    adapter.restores[uuid]==2
            ),
            FixtureResult(
                "player-prepare-failure-immediately-rolls-back",
                prepareFailure &&
                    immediatePrepareRollback
            )
        )
    }
}
