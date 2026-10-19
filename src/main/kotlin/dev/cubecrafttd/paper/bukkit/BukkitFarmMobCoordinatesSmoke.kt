package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.mob.MobInstanceId
import dev.cubecrafttd.troop.MobEntitySpawnRequest
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.LivingEntity
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

/** Only called by the opt-in CI smoke in its owned dimension. */
object BukkitFarmMobCoordinatesSmoke {
    fun run(plugin: JavaPlugin, world: World) {
        val surface = Vec3(world.spawnLocation.blockX + 4.0,
            world.maxHeight - 12.0, world.spawnLocation.blockZ + 4.0)
        val block = world.getBlockAt(surface.x.toInt(), surface.y.toInt(), surface.z.toInt())
        val before = block.blockData.clone()
        var uuid: UUID? = null
        try {
            block.setType(Material.JUNGLE_PLANKS, false)
            uuid = BukkitMobEntitySpawnPort(world, BukkitFarmMobCoordinates::feet).spawn(
                MobEntitySpawnRequest(MobInstanceId(120001), UUID(0, 120001),
                    TeamId.RED, "zombie", 1, "projection-smoke", surface))
            val entity = plugin.server.getEntity(uuid) as LivingEntity
            check(!entity.hasGravity())
            check(entity.location.x == surface.x + 0.5 && entity.location.y == surface.y + 1.0 &&
                entity.location.z == surface.z + 0.5)
            check(entity.location.block.type.isAir) { "Mob feet intersect route surface" }
            val destination = Vec3(surface.x + 0.25, surface.y, surface.z + 0.75)
            val runtime = BukkitEntityRuntimeAdapter(plugin.server, BukkitFarmMobCoordinates::feet)
            runtime.move(uuid, destination)
            val actual = runtime.position(uuid)!!
            check(actual == BukkitFarmMobCoordinates.feet(destination)) { "Movement projection differs from spawn" }
        } finally {
            uuid?.let { plugin.server.getEntity(it)?.remove() }
            block.setBlockData(before, false)
        }
        plugin.logger.info("Farm mob coordinates PASS: centered feet above path, fractional movement and core-owned gravity")
    }
}
