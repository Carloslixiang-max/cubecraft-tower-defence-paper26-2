package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.map.BlockPos
import org.bukkit.World
import org.bukkit.entity.Entity

/**
 * Persistent marker for entities owned by the CubeCraft TD runtime.
 *
 * Scoreboard tags are serialized with entity NBT, so a later process can
 * identify leftovers after an unclean server stop without guessing by type.
 */
object BukkitTrackedEntityTag {
    const val TAG=
        "cubecrafttd_tracked"

    fun mark(
        entity: Entity
    ) {
        check(
            TAG in entity.scoreboardTags ||
                entity.addScoreboardTag(TAG)
        ) {
            "Failed to persist TD entity tag for ${entity.uniqueId}"
        }
    }

    fun taggedInVolume(
        world: World,
        origin: BlockPos,
        width: Int,
        height: Int,
        length: Int
    ): List<Entity> {
        require(width>0)
        require(height>0)
        require(length>0)

        val maxX=
            origin.x.toDouble() +
                width.toDouble()
        val maxY=
            origin.y.toDouble() +
                height.toDouble()
        val maxZ=
            origin.z.toDouble() +
                length.toDouble()

        return world.entities
            .asSequence()
            .filter {
                TAG in it.scoreboardTags
            }
            .filter { entity ->
                val p=entity.location
                p.x>=origin.x.toDouble() &&
                    p.x<maxX &&
                    p.y>=origin.y.toDouble() &&
                    p.y<maxY &&
                    p.z>=origin.z.toDouble() &&
                    p.z<maxZ
            }
            .sortedBy {
                it.uniqueId.toString()
            }
            .toList()
    }
}
