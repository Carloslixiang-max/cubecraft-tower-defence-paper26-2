package dev.cubecrafttd.paper

import java.util.UUID

/** Runtime entity bridge. Combat code must not call world-global discovery through this adapter. */
interface PaperEntityAdapter {
    fun remove(uuid: UUID): Boolean
    fun isAlive(uuid: UUID): Boolean
    fun currentPosition(uuid: UUID): EntityPosition?
}

data class EntityPosition(val x: Double, val y: Double, val z: Double)
