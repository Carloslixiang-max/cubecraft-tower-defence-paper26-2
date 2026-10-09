package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.paper.PaperBlockWorldAdapter
import dev.cubecrafttd.tower.visual.*
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.TileState
import org.bukkit.block.BlockState
import java.util.UUID

class BukkitBlockWorldAdapter(
    private val world: World
) : PaperBlockWorldAdapter {
    // Arena-lifetime handles only. Player crash journals and Farm reset remain separate.
    private val ownedStateSnapshots = linkedMapOf<String, Pair<BlockKey, BlockState>>()

    private fun at(key: BlockKey): Block {
        require(key.y in world.minHeight until world.maxHeight) { "Tower model exceeds world height" }
        return world.getBlockAt(key.x, key.y, key.z)
    }

    override fun snapshot(
        block: BlockKey
    ): BlockSnapshot {
        val b = at(block)
        check(b.state !is TileState) {
            "Tower placement would cover TileState at $block; " +
                "lossless tile-state snapshot/restore has not passed the Paper live gate"
        }
        return BlockSnapshot(
            b.blockData.asString,
            optionalStatePayload = null
        )
    }

    override fun snapshotOwned(block: BlockKey): BlockSnapshot {
        val b = at(block)
        val state = b.state // Placed snapshot retains NBT and can update its original coordinate.
        if (state !is TileState) return snapshot(block)
        val token = UUID.randomUUID().toString()
        ownedStateSnapshots[token] = block to state
        return BlockSnapshot(b.blockData.asString, token.toByteArray(Charsets.UTF_8))
    }

    override fun releaseSnapshot(snapshot: BlockSnapshot) {
        snapshot.optionalStatePayload?.let { ownedStateSnapshots.remove(it.toString(Charsets.UTF_8)) }
    }

    override fun apply(
        block: BlockKey,
        snapshot: BlockSnapshot,
        applyPhysics: Boolean
    ) {
        if (snapshot.optionalStatePayload != null) {
            val token = snapshot.optionalStatePayload.toString(Charsets.UTF_8)
            val saved = ownedStateSnapshots[token] ?: error("Unknown arena-local tile snapshot")
            check(saved.first == block && saved.second.blockData.asString == snapshot.blockData)
            check(saved.second.update(true, applyPhysics)) { "Owned tile-state snapshot restore failed" }
            return
        }
        at(block)
            .setBlockData(
                Bukkit.createBlockData(snapshot.blockData),
                applyPhysics
            )
    }

    override fun currentBlockData(
        block: BlockKey
    ): String = at(block)
        .blockData.asString
}
