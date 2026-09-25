package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.paper.PaperBlockWorldAdapter
import dev.cubecrafttd.tower.visual.*
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.block.TileState

class BukkitBlockWorldAdapter(
    private val world: World
) : PaperBlockWorldAdapter {
    override fun snapshot(
        block: BlockKey
    ): BlockSnapshot {
        val b = world.getBlockAt(block.x,block.y,block.z)
        check(b.state !is TileState) {
            "Tower placement would cover TileState at $block; " +
                "lossless tile-state snapshot/restore has not passed the Paper live gate"
        }
        return BlockSnapshot(
            b.blockData.asString,
            optionalStatePayload = null
        )
    }

    override fun apply(
        block: BlockKey,
        snapshot: BlockSnapshot,
        applyPhysics: Boolean
    ) {
        check(snapshot.optionalStatePayload == null) {
            "Tile-state payload restore is not live-validated yet; refusing lossy restore at $block"
        }
        world.getBlockAt(block.x,block.y,block.z)
            .setBlockData(
                Bukkit.createBlockData(snapshot.blockData),
                applyPhysics
            )
    }

    override fun currentBlockData(
        block: BlockKey
    ): String = world
        .getBlockAt(block.x,block.y,block.z)
        .blockData.asString
}
