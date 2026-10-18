package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.tower.visual.QuarterTurn
import org.bukkit.Bukkit
import org.bukkit.block.structure.StructureRotation

/** Rotate state directions together with RotationCache's (x,z) coordinates. */
object BukkitTowerBlockRotation {
    fun rotate(state: String, turn: QuarterTurn): String {
        val data = Bukkit.createBlockData(state)
        data.rotate(when (turn) {
            QuarterTurn.R0 -> StructureRotation.NONE
            QuarterTurn.R90 -> StructureRotation.CLOCKWISE_90
            QuarterTurn.R180 -> StructureRotation.CLOCKWISE_180
            QuarterTurn.R270 -> StructureRotation.COUNTERCLOCKWISE_90
        })
        return data.asString
    }
}
