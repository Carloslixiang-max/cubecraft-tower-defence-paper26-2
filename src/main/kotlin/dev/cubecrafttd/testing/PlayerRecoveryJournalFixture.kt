package dev.cubecrafttd.testing

import dev.cubecrafttd.player.PlayerSnapshot
import dev.cubecrafttd.recovery.FilePlayerRecoveryJournal
import java.nio.file.Files
import java.util.UUID

object PlayerRecoveryJournalFixture {
    fun run(): List<FixtureResult> {
        val dir = Files.createTempDirectory("ctd-recovery-fixture")
        return try {
            val journal = FilePlayerRecoveryJournal(dir)
            val uuid = UUID.fromString("00000000-0000-0000-0000-000000000888")
            val snapshot = PlayerSnapshot(
                uuid, 123L,
                byteArrayOf(1,2,3), "ADVENTURE",
                byteArrayOf(4), byteArrayOf(5), byteArrayOf(6),
                9, 0.25f, 200, 17.5, 1.0, 16, 3.5f, 0.2f,
                4, 250, true, false, 2.0f, byteArrayOf(7), byteArrayOf(8)
            )
            journal.save(snapshot)
            val loaded = journal.loadAll().single()
            val roundTrip = loaded.playerUuid == snapshot.playerUuid &&
                loaded.capturedAtArenaTick == 123L &&
                loaded.gameModeName == "ADVENTURE" &&
                loaded.inventoryPayload.contentEquals(snapshot.inventoryPayload) &&
                loaded.health == 17.5

            journal.delete(uuid)
            val deleted = journal.loadAll().isEmpty()
            listOf(
                FixtureResult("recovery-journal-roundtrip", roundTrip),
                FixtureResult("recovery-journal-delete-after-restore", deleted)
            )
        } finally {
            Files.walk(dir).sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
    }
}
