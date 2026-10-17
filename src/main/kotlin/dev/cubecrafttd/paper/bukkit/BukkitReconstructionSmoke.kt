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
    fun run(plugin: JavaPlugin) {
        val models = BukkitTowerBodyCatalogLoader.loadBundled(plugin, true)
        check(models.loadedCount() == 82 && models.missingStages().isEmpty())
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

        val world = plugin.server.worlds.first()
        val base = BlockPos(world.spawnLocation.blockX + 8, world.maxHeight - 20, world.spawnLocation.blockZ + 8)
        val adapter = BukkitBlockWorldAdapter(world)
        val ledger = TowerBodyLedger()
        val mutations = TowerBodyMutationService(adapter, ledger, RotationCache { data, _ -> data })
        RecommendedMatureTowerDefinitions.all().keys.forEachIndexed { index, id ->
            val maxLevel = if (id in setOf("turret", "necromancer", "leach")) 3 else 4
            val towerId = 11400L + index
            try {
                mutations.place(towerId, base, models.definition(id, 1, TowerPath.TOP), QuarterTurn.R0, 0)
                mutations.upgrade(towerId, base, models.definition(id, maxLevel, TowerPath.TOP), QuarterTurn.R0, 1)
                mutations.upgrade(towerId, base, models.definition(id, maxLevel, TowerPath.BOTTOM), QuarterTurn.R0, 2)
                check(!mutations.remove(towerId).conflictReport.hasConflicts)
                check(ledger.isEmpty())
            } finally {
                mutations.remove(towerId)
            }
        }
        plugin.logger.info("Reconstruction assets PASS: 11 tower families, 82 stages, zero placeholders; scene palette/composition and model restore checked")
    }
}
