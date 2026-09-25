package dev.cubecrafttd.tower.visual

sealed interface RestoreDecision {
    data object Restore : RestoreDecision
    data class Conflict(
        val currentBlockData: String,
        val expectedInstalledBlockData: String
    ) : RestoreDecision
}

data class LedgerRecord(
    val towerInstanceId: Long,
    val installed: BlockSnapshot,
    val previous: BlockSnapshot,
    val bodyRevision: String,
    val writeTick: Long
)

data class TowerBodyConflict(
    val blockKey: BlockKey,
    val towerInstanceId: Long,
    val currentBlockData: String,
    val expectedInstalledBlockData: String
)

data class TowerBodyConflictReport(
    val conflicts: List<TowerBodyConflict> = emptyList()
) {
    val hasConflicts: Boolean get() = conflicts.isNotEmpty()
}

class TowerBodyLedger {
    private val records = linkedMapOf<BlockKey, LedgerRecord>()

    fun claim(blockKey: BlockKey, record: LedgerRecord) {
        check(blockKey !in records) { "Tower body overlap at $blockKey" }
        records[blockKey] = record
    }

    fun replaceOwned(
        blockKey: BlockKey,
        expectedTowerInstanceId: Long,
        record: LedgerRecord
    ) {
        val existing = records[blockKey]
            ?: error("No existing ledger record at $blockKey")
        check(existing.towerInstanceId == expectedTowerInstanceId) {
            "Block $blockKey is owned by ${existing.towerInstanceId}, " +
                "not $expectedTowerInstanceId"
        }
        records[blockKey] = record
    }

    fun restoreRecords(snapshot: Map<BlockKey, LedgerRecord>) {
        records.clear()
        records.putAll(snapshot)
    }

    fun snapshotRecords(): Map<BlockKey, LedgerRecord> =
        LinkedHashMap(records)

    fun get(blockKey: BlockKey): LedgerRecord? = records[blockKey]

    fun ownedBy(towerInstanceId: Long): Map<BlockKey, LedgerRecord> =
        records.filterValues { it.towerInstanceId == towerInstanceId }

    fun decideRestore(
        blockKey: BlockKey,
        currentBlockData: String
    ): RestoreDecision {
        val record = records[blockKey]
            ?: error("No ledger record for $blockKey")
        return if (currentBlockData == record.installed.blockData) {
            RestoreDecision.Restore
        } else {
            RestoreDecision.Conflict(
                currentBlockData,
                record.installed.blockData
            )
        }
    }

    fun release(blockKey: BlockKey) {
        records.remove(blockKey)
    }

    fun isClaimed(blockKey: BlockKey): Boolean = blockKey in records
    fun isEmpty(): Boolean = records.isEmpty()
    fun size(): Int = records.size
}
