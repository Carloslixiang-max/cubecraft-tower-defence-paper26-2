package dev.cubecrafttd.paper

import org.bukkit.Bukkit
import java.util.UUID

class BukkitEntityAdapter : PaperEntityAdapter {
    override fun remove(uuid: UUID): Boolean {
        val entity = Bukkit.getEntity(uuid) ?: return false
        entity.remove()
        return true
    }

    override fun isAlive(uuid: UUID): Boolean {
        val entity = Bukkit.getEntity(uuid) ?: return false
        return entity.isValid && !entity.isDead
    }

    override fun currentPosition(uuid: UUID): EntityPosition? {
        val location = Bukkit.getEntity(uuid)?.location ?: return null
        return EntityPosition(location.x, location.y, location.z)
    }
}
