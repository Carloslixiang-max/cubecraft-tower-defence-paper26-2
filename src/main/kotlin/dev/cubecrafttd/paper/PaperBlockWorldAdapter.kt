package dev.cubecrafttd.paper

import dev.cubecrafttd.tower.visual.BlockKey
import dev.cubecrafttd.tower.visual.BlockSnapshot

/**
 * Paper implementation is responsible for BlockData parsing, tile-state payloads,
 * physics flags, chunk availability and main-thread safety.
 */
interface PaperBlockWorldAdapter {
    fun snapshot(block: BlockKey): BlockSnapshot
    fun apply(block: BlockKey, snapshot: BlockSnapshot, applyPhysics: Boolean = false)
    fun currentBlockData(block: BlockKey): String
}
