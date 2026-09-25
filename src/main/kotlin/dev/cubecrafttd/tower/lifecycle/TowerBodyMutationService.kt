package dev.cubecrafttd.tower.lifecycle

import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.paper.PaperBlockWorldAdapter
import dev.cubecrafttd.tower.visual.*

data class InstalledBodyResult(
    val blockKeys: Set<BlockKey>,
    val bodyRevision: String
)

data class BodyRemovalResult(
    val conflictReport: TowerBodyConflictReport,
    val releasedBlockCount: Int
)

class TowerBodyMutationService(
    private val world: PaperBlockWorldAdapter,
    private val ledger: TowerBodyLedger,
    private val rotationCache: RotationCache
) {
    fun preflightPlacement(
        base: BlockPos,
        definition: TowerBodyDefinition,
        rotation: QuarterTurn
    ): Map<BlockKey, Pair<BlockSnapshot, BlockSnapshot>> {
        val resolved = absoluteBody(base, definition, rotation)
        check(resolved.keys.size == resolved.size) {
            "Tower body contains duplicate absolute blocks"
        }

        val plan = linkedMapOf<BlockKey, Pair<BlockSnapshot, BlockSnapshot>>()
        resolved.forEach { (key, installed) ->
            check(!ledger.isClaimed(key)) {
                "Tower body overlap at $key"
            }
            val previous = world.snapshot(key)
            plan[key] = previous to installed
        }
        return plan
    }

    fun place(
        towerInstanceId: Long,
        base: BlockPos,
        definition: TowerBodyDefinition,
        rotation: QuarterTurn,
        writeTick: Long
    ): InstalledBodyResult {
        val plan = preflightPlacement(
            base, definition, rotation
        )
        val written = mutableListOf<BlockKey>()
        try {
            plan.forEach { (key, pair) ->
                val (previous, installed) = pair
                world.apply(key, installed, applyPhysics = false)
                ledger.claim(
                    key,
                    LedgerRecord(
                        towerInstanceId,
                        installed,
                        previous,
                        definition.bodyRevision,
                        writeTick
                    )
                )
                written += key
            }
            return InstalledBodyResult(
                plan.keys.toSet(),
                definition.bodyRevision
            )
        } catch (t: Throwable) {
            rollbackWritten(written, plan)
            throw t
        }
    }

    fun preflightUpgrade(
        towerInstanceId: Long,
        base: BlockPos,
        newDefinition: TowerBodyDefinition,
        rotation: QuarterTurn
    ) {
        val oldRecords = ledger.ownedBy(towerInstanceId)
        check(oldRecords.isNotEmpty()) {
            "Tower $towerInstanceId has no installed body"
        }
        val newAbsolute = absoluteBody(base, newDefinition, rotation)

        newAbsolute.keys.forEach { key ->
            val existing = ledger.get(key)
            check(existing == null ||
                existing.towerInstanceId == towerInstanceId) {
                "Upgrade body overlaps another tower at $key"
            }
        }

        // Any external edit to the currently-owned body blocks aborts the
        // upgrade before debit/world mutation. This prevents overwriting
        // admin/other-plugin changes with a new tower body.
        oldRecords.forEach { (key, record) ->
            val current = world.currentBlockData(key)
            check(current == record.installed.blockData) {
                "External edit conflict during upgrade preflight at $key"
            }
        }

        // Snapshot every new-only block now to prove it is readable before commit.
        (newAbsolute.keys - oldRecords.keys).forEach(world::snapshot)
    }

    fun upgrade(
        towerInstanceId: Long,
        base: BlockPos,
        newDefinition: TowerBodyDefinition,
        rotation: QuarterTurn,
        writeTick: Long
    ): InstalledBodyResult {
        val oldRecords = ledger.ownedBy(towerInstanceId)
        check(oldRecords.isNotEmpty()) {
            "Tower $towerInstanceId has no installed body"
        }
        val oldLedgerSnapshot = ledger.snapshotRecords()

        val newAbsolute = absoluteBody(base, newDefinition, rotation)
        newAbsolute.keys.forEach { key ->
            val existing = ledger.get(key)
            check(existing == null || existing.towerInstanceId == towerInstanceId) {
                "Upgrade body overlaps another tower at $key"
            }
        }

        val union = linkedSetOf<BlockKey>().apply {
            addAll(oldRecords.keys)
            addAll(newAbsolute.keys)
        }
        val worldBefore = union.associateWith(world::snapshot)

        try {
            // Restore old-only blocks first.
            (oldRecords.keys - newAbsolute.keys).forEach { key ->
                val record = oldRecords.getValue(key)
                val current = world.currentBlockData(key)
                if (current != record.installed.blockData) {
                    error("External edit conflict during upgrade at $key")
                }
                world.apply(key, record.previous, applyPhysics = false)
                ledger.release(key)
            }

            newAbsolute.forEach { (key, installed) ->
                val existing = oldRecords[key]
                if (existing != null) {
                    world.apply(key, installed, applyPhysics = false)
                    ledger.replaceOwned(
                        key,
                        towerInstanceId,
                        existing.copy(
                            installed = installed,
                            bodyRevision = newDefinition.bodyRevision,
                            writeTick = writeTick
                        )
                    )
                } else {
                    val previous = worldBefore.getValue(key)
                    world.apply(key, installed, applyPhysics = false)
                    ledger.claim(
                        key,
                        LedgerRecord(
                            towerInstanceId,
                            installed,
                            previous,
                            newDefinition.bodyRevision,
                            writeTick
                        )
                    )
                }
            }

            return InstalledBodyResult(
                newAbsolute.keys.toSet(),
                newDefinition.bodyRevision
            )
        } catch (t: Throwable) {
            // Restore exact world state that existed before upgrade attempt,
            // then restore ledger atomically.
            union.toList().asReversed().forEach { key ->
                runCatching {
                    world.apply(
                        key,
                        worldBefore.getValue(key),
                        applyPhysics = false
                    )
                }
            }
            ledger.restoreRecords(oldLedgerSnapshot)
            throw t
        }
    }

    fun remove(towerInstanceId: Long): BodyRemovalResult {
        val owned = ledger.ownedBy(towerInstanceId)
        val ledgerBefore = ledger.snapshotRecords()
        val worldBefore = owned.keys.associateWith(world::snapshot)
        val conflicts = mutableListOf<TowerBodyConflict>()

        try {
            owned.entries.toList().asReversed().forEach { (key, record) ->
                val current = world.currentBlockData(key)
                when (ledger.decideRestore(key, current)) {
                    RestoreDecision.Restore ->
                        world.apply(
                            key,
                            record.previous,
                            applyPhysics = false
                        )
                    is RestoreDecision.Conflict -> {
                        conflicts += TowerBodyConflict(
                            key,
                            towerInstanceId,
                            current,
                            record.installed.blockData
                        )
                    }
                }
                ledger.release(key)
            }
        } catch (t: Throwable) {
            owned.keys.toList().asReversed().forEach { key ->
                runCatching {
                    world.apply(
                        key,
                        worldBefore.getValue(key),
                        applyPhysics = false
                    )
                }
            }
            ledger.restoreRecords(ledgerBefore)
            throw t
        }

        return BodyRemovalResult(
            TowerBodyConflictReport(conflicts),
            owned.size
        )
    }

    private fun absoluteBody(
        base: BlockPos,
        definition: TowerBodyDefinition,
        rotation: QuarterTurn
    ): Map<BlockKey, BlockSnapshot> =
        rotationCache.resolve(definition, rotation).associate { block ->
            val key = BlockKey(
                base.x + block.pos.x,
                base.y + block.pos.y,
                base.z + block.pos.z
            )
            key to BlockSnapshot(block.blockData)
        }

    private fun rollbackWritten(
        written: List<BlockKey>,
        plan: Map<BlockKey, Pair<BlockSnapshot, BlockSnapshot>>
    ) {
        written.asReversed().forEach { key ->
            runCatching {
                world.apply(
                    key,
                    plan.getValue(key).first,
                    applyPhysics = false
                )
            }
            ledger.release(key)
        }
    }
}
