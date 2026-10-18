package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.ArenaTaskHandle
import dev.cubecrafttd.arena.FarmReuseGatePersistentState
import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.map.schematic.*
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Files

/** CI-only asynchronous world regressions, confined to the owned test dimension. */
object BukkitFarmInstallRegressionSmoke {
    fun start(plugin: JavaPlugin, world: World) {
        val directory = Files.createTempDirectory(plugin.dataFolder.toPath(), "reuse-evidence-smoke-")
        try {
            val file = directory.resolve("gate.yml").toFile()
            val persistence = BukkitFarmReuseGatePersistence(file)
            listOf("broken: [", "", "hard-tower-conflicts: []\nsuspect-tracked-entities: [invalid-uuid]\n",
                "hard-tower-conflicts: []\nsuspect-tracked-entities: []\nverified-reset-in-progress: garbage\n").forEach { corrupt ->
                file.writeText(corrupt)
                check(persistence.load().uncleanRestartSuspectedResidue) { "Corrupt reuse evidence became clean" }
                check(file.readText() == corrupt) { "Corrupt evidence was overwritten during load" }
            }
            persistence.save(FarmReuseGatePersistentState(uncleanRestartSuspectedResidue = true))
            check(persistence.load().uncleanRestartSuspectedResidue)
            check(directory.toFile().listFiles()!!.any { it.name.contains(".corrupt-") && it.readText().contains("garbage") })
            persistence.save(FarmReuseGatePersistentState())
            check(persistence.load() == FarmReuseGatePersistentState())
        } finally {
            Files.list(directory).use { stream -> stream.forEach(Files::delete) }
            Files.delete(directory)
        }

        val base = BlockPos(96, world.maxHeight - 24, 96)
        val block = world.getBlockAt(base.x, base.y, base.z)
        check(block.type.isAir && world.getBlockAt(base.x + 1, base.y, base.z).type.isAir)
        val volume = DecodedMapVolume("synthetic", SchematicDimensions(2, 1, 1), SchematicOffset(0, 0, 0),
            mapOf("minecraft:air" to 0, "minecraft:stone" to 1),
            mapOf(0 to "minecraft:air", 1 to "minecraft:stone"), intArrayOf(1, 0), "synthetic")
        val service = BukkitSchematicPasteService(plugin, world)
        var case = 0
        lateinit var advance: () -> Unit
        advance = {
            when (case++) {
                0 -> {
                    val progress = mutableListOf<Int>()
                    service.pasteIntoEmptyRegion(volume, base, 1,
                        onProgress = { it.percentage?.let(progress::add) },
                        onComplete = {
                            check(progress.last() == 100 && progress.zipWithNext().all { it.first <= it.second })
                            check(block.type == Material.STONE)
                            block.setType(Material.AIR, false)
                            advance()
                        }, onFailure = { throw it })
                }
                1 -> service.pasteIntoEmptyRegion(volume, base, 1,
                    onComplete = { error("Injected completion callback failure") },
                    onFailure = {
                        check(it.message == "Injected completion callback failure")
                        check(block.type == Material.STONE) { "Completed scene was rolled back by callback failure" }
                        block.setType(Material.AIR, false)
                        advance()
                    })
                2 -> service.pasteIntoEmptyRegion(volume, base, 1,
                    onProgress = { if (it.phase == "PASTE" && it.writtenBlocks == 1) error("Injected progress failure") },
                    onFailure = {
                        check(it.message == "Injected progress failure" && block.type.isAir)
                        advance()
                    })
                3 -> {
                    lateinit var handle: ArenaTaskHandle
                    var cancelled = false
                    handle = service.pasteIntoEmptyRegion(volume, base, 1,
                        onProgress = {
                            if (!cancelled && it.phase == "PASTE" && it.writtenBlocks == 1) {
                                cancelled = true
                                handle.cancel()
                                plugin.server.scheduler.runTask(plugin, Runnable {
                                    check(handle.isCancelled && block.type.isAir)
                                    advance()
                                })
                            }
                        }, onComplete = { error("Cancelled paste completed") }, onFailure = { throw it })
                }
                else -> plugin.logger.info("Farm lifecycle regression PASS: corrupt evidence, atomic gate persistence, sparse progress, completion callback failure, rollback and cancellation")
            }
        }
        advance()
    }
}
