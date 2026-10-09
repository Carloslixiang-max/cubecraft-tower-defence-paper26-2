package dev.cubecrafttd.paper

import dev.cubecrafttd.tower.visual.BlockKey
import dev.cubecrafttd.tower.visual.BlockSnapshot

/**
 * Paper implementation is responsible for BlockData parsing, tile-state payloads,
 * physics flags, chunk availability and main-thread safety.
 */
interface PaperBlockWorldAdapter {
    fun snapshot(block: BlockKey): BlockSnapshot
    /** Only for positions already claimed in this arena's tower body ledger. */
    fun snapshotOwned(block: BlockKey): BlockSnapshot = snapshot(block)
    fun releaseSnapshot(snapshot: BlockSnapshot) {}
    fun apply(block: BlockKey, snapshot: BlockSnapshot, applyPhysics: Boolean = false)
    fun currentBlockData(block: BlockKey): String
}
