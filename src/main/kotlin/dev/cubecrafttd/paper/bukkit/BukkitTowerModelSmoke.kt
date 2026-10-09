package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.paper.PaperBlockWorldAdapter
import dev.cubecrafttd.tower.lifecycle.TowerBodyMutationService
import dev.cubecrafttd.tower.visual.*
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Dispenser
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Files
import java.security.MessageDigest

/** Synthetic models only; validates real Paper data and rollback, not original models. */
object BukkitTowerModelSmoke {
    fun run(plugin: JavaPlugin) {
        val directory = Files.createTempDirectory(plugin.dataFolder.toPath(), "tower-model-smoke-")
        try {
            val model = """
                format-version: 1
                tower-id: archer
                level: 1
                path: TOP
                evidence-status: COMMUNITY_RECONSTRUCTION
                evidence-refs: [https://example.invalid/synthetic-smoke]
                blocks:
                  - {x: 0, y: 0, z: 0, block-data: 'minecraft:dispenser[facing=north]'}
            """.trimIndent().toByteArray(Charsets.UTF_8)
            Files.write(directory.resolve("test.yml"), model)
            val sha = MessageDigest.getInstance("SHA-256").digest(model).joinToString("") { "%02x".format(it) }
            Files.writeString(directory.resolve("catalog.yml"),
                "format-version: 1\nmodels:\n  - {file: test.yml, sha256: '$sha'}\n")
            val catalog = BukkitTowerBodyCatalogLoader.loadDirectory(directory, false)
            val body = catalog.definition("archer", 1, TowerPath.TOP)
            check(body.blocks.single().blockData == "minecraft:dispenser[facing=north,triggered=false]")
            check(catalog.loadedCount() == 1)
            check(runCatching { BukkitTowerBodyCatalogLoader.loadDirectory(directory, true) }.isFailure)
            Files.writeString(directory.resolve("test.yml"), "tampered")
            check(runCatching { BukkitTowerBodyCatalogLoader.loadDirectory(directory, false) }.isFailure)

            val world = plugin.server.worlds.first()
            val base = BlockPos(world.spawnLocation.blockX, world.maxHeight - 4, world.spawnLocation.blockZ)
            val key = BlockKey(base.x, base.y, base.z)
            val second = BlockKey(base.x + 1, base.y, base.z)
            val adapter = BukkitBlockWorldAdapter(world)
            val previous = adapter.snapshot(key)
            val previousSecond = adapter.snapshot(second)
            val existingEntities = world.getBlockAt(base.x, base.y, base.z).chunk.entities.map { it.uniqueId }.toSet()
            val ledger = TowerBodyLedger()
            var failAtSecond = false
            val injected = object : PaperBlockWorldAdapter by adapter {
                override fun apply(block: BlockKey, snapshot: BlockSnapshot, applyPhysics: Boolean) {
                    if (failAtSecond && block == second) {
                        failAtSecond = false
                        error("Injected model upgrade failure")
                    }
                    adapter.apply(block, snapshot, applyPhysics)
                }
            }
            val mutations = TowerBodyMutationService(injected, ledger, RotationCache { data, _ -> data })
            try {
                mutations.place(108, base, body, QuarterTurn.R0, 0)
                val tile = world.getBlockAt(base.x, base.y, base.z).state as Dispenser
                tile.snapshotInventory.setItem(2, ItemStack(Material.DIAMOND, 3))
                val tag = NamespacedKey(plugin, "tower-model-smoke")
                tile.persistentDataContainer.set(tag, PersistentDataType.STRING, "preserve-me")
                check(tile.update(true, false))
                check(runCatching { adapter.snapshot(key) }.isFailure) // Unowned path remains protected.
                val next = body.copy(level = 2, bodyRevision = "synthetic-upgrade", blocks = listOf(
                    RelativeBodyBlock(BlockPos(0, 0, 0), "minecraft:stone"),
                    RelativeBodyBlock(BlockPos(1, 0, 0), "minecraft:oak_planks")))
                failAtSecond = true
                check(runCatching { mutations.upgrade(108, base, next, QuarterTurn.R0, 1) }.isFailure)
                val restored = world.getBlockAt(base.x, base.y, base.z).state as Dispenser
                check(restored.inventory.getItem(2) == ItemStack(Material.DIAMOND, 3))
                check(restored.persistentDataContainer.get(tag, PersistentDataType.STRING) == "preserve-me")
                check(ledger.size() == 1 && adapter.currentBlockData(second) == previousSecond.blockData)
                mutations.upgrade(108, base, next, QuarterTurn.R0, 2)
                check(ledger.size() == 2)
                check(!mutations.remove(108).conflictReport.hasConflicts)
                check(ledger.isEmpty())
                check(adapter.currentBlockData(key) == previous.blockData)
                check(adapter.currentBlockData(second) == previousSecond.blockData)
                check(runCatching { adapter.snapshot(BlockKey(base.x, world.maxHeight, base.z)) }.isFailure)
            } finally {
                // Even smoke failures leave both pre-existing blocks intact.
                adapter.apply(key, previous, false)
                adapter.apply(second, previousSecond, false)
                // A replaced container may eject its synthetic inventory even without physics.
                world.getBlockAt(base.x, base.y, base.z).chunk.entities.filterIsInstance<Item>()
                    .filter { it.uniqueId !in existingEntities && it.itemStack.type == Material.DIAMOND &&
                        it.location.distanceSquared(world.getBlockAt(base.x, base.y, base.z).location) < 16.0 }
                    .forEach { it.remove() }
            }
        } finally {
            Files.list(directory).use { it.forEach(Files::delete) }
            Files.delete(directory)
        }
    }
}
