package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.map.schematic.*
import dev.cubecrafttd.tower.RecommendedMatureTowerDefinitions
import dev.cubecrafttd.tower.lifecycle.TowerBodyMutationService
import dev.cubecrafttd.tower.visual.*
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

/** Loads every supplied state and exercises real world placement/upgrade/restore. */
object BukkitReconstructionSmoke {
    fun run(plugin: JavaPlugin, world: org.bukkit.World) {
        BukkitArenaChunkLeaseSmoke.run(plugin, world)
        val models = BukkitTowerBodyCatalogLoader.loadBundled(plugin, true)
        check(models.loadedCount() == 82 && models.missingStages().isEmpty())
        check("facing=east" in BukkitTowerBlockRotation.rotate("minecraft:oak_stairs[facing=north]", QuarterTurn.R90))
        check("axis=z" in BukkitTowerBlockRotation.rotate("minecraft:oak_log[axis=x]", QuarterTurn.R90))
        check("east=true" in BukkitTowerBlockRotation.rotate("minecraft:oak_fence[north=true]", QuarterTurn.R90))
        val scenery = ReconstructedFarmAssets.scene(plugin)
        scenery.palette.keys.forEach { Bukkit.createBlockData(it) }
        check(scenery.countBlock("minecraft:air") < scenery.blockIds.size - 400_000)
        val track = DecodedMapVolume("synthetic", SchematicDimensions(3, 18, 3), SchematicOffset(0, 0, 0),
            mapOf("minecraft:air" to 0, "minecraft:oak_planks" to 1),
            mapOf(0 to "minecraft:air", 1 to "minecraft:oak_planks"), IntArray(162).also { it[1 + 3 + 3 * 9] = 1 }, "synthetic")
        val composed = ReconstructedFarmAssets.compose(track, scenery)
        check(composed.blockAt(61, 6, 46) == "minecraft:oak_planks")
        check(composed.blockAt(61, 7, 46) == "minecraft:air")
        check(scenery.blockAt(61, 6, 46) != "minecraft:oak_planks") // Source stays immutable.

        val base = BlockPos(world.spawnLocation.blockX + 8, world.maxHeight - 20, world.spawnLocation.blockZ + 8)
        val adapter = BukkitBlockWorldAdapter(world)
        val ledger = TowerBodyLedger()
        val mutations = TowerBodyMutationService(adapter, ledger, RotationCache(BukkitTowerBlockRotation::rotate))
        RecommendedMatureTowerDefinitions.all().keys.forEachIndexed { index, id ->
            val maxLevel = if (id in setOf("turret", "necromancer", "leach")) 3 else 4
            val towerId = 11400L + index
            try {
                mutations.place(towerId, base, models.definition(id, 1, TowerPath.TOP), QuarterTurn.R0, 0)
                if (id == "mage") {
                    val target = world.spawnEntity(org.bukkit.Location(world, base.x + 12.5, base.y + 0.5, base.z + 0.5),
                        org.bukkit.entity.EntityType.ARMOR_STAND)
                    target.setGravity(false)
                    val wall = BlockKey(base.x + 6, base.y, base.z)
                    val previous = adapter.snapshot(wall)
                    try {
                        val ray = BukkitLineOfSightPort(plugin.server)
                        val inside = dev.cubecrafttd.map.Vec3(base.x + 0.5, base.y + 0.5, base.z + 0.5)
                        fun owned(block: org.bukkit.block.Block) =
                            ledger.get(BlockKey(block.x, block.y, block.z))?.towerInstanceId == towerId
                        check(!ray.hasTowerLineOfSight(inside, target) { false }) { "Unfiltered ray did not hit the supplied Mage body" }
                        check(ray.hasTowerLineOfSight(inside, target, ::owned)) { "Filtered ray remained blocked by its own Mage body; target=${target.location}" }
                        adapter.apply(wall, BlockSnapshot("minecraft:stone"), false)
                        check(!ray.hasTowerLineOfSight(inside, target, ::owned)) { "Filtered ray incorrectly ignored an external stone wall" }
                    } finally {
                        target.remove()
                        adapter.apply(wall, previous, false)
                    }
                }
                mutations.upgrade(towerId, base, models.definition(id, maxLevel, TowerPath.TOP), QuarterTurn.R0, 1)
                mutations.upgrade(towerId, base, models.definition(id, maxLevel, TowerPath.BOTTOM), QuarterTurn.R0, 2)
                check(!mutations.remove(towerId).conflictReport.hasConflicts)
                check(ledger.isEmpty())
                // Exercise EVERY supplied level/path/state in all orientations,
                // including upgrades' stairs, connected panes and log axes.
                for (path in TowerPath.entries) {
                    for (level in 1..maxLevel) {
                        for (turn in QuarterTurn.entries) {
                            val body = models.definition(id, level, path)
                            mutations.place(towerId, base, body, turn, 3)
                            check(!mutations.remove(towerId).conflictReport.hasConflicts)
                            check(ledger.isEmpty())
                        }
                    }
                }
            } finally {
                mutations.remove(towerId)
            }
        }
        plugin.logger.info("Reconstruction assets PASS: 11 tower families, 82 stages in all four rotations, zero placeholders; scene composition, model restore and self-body/external-wall raytrace checked")
    }
}
