package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.map.BlockPos
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** An explicitly created add-on world. Never adopts, deletes or regenerates an existing world. */
class BukkitManagedFarmWorld(
    private val plugin: JavaPlugin,
    private val worldName: String = "ctd_farm",
    manifestName: String = "managed-farm.yml",
    private val onBuildFailure: () -> Unit = {}
) {
    private val manifest = File(plugin.dataFolder, manifestName)
    private var loadError: String? = null
    private var state: YamlConfiguration? = if (manifest.exists()) runCatching {
        YamlConfiguration().also { it.load(manifest) }
    }.getOrElse {
        loadError = "Cannot read managed-world manifest: ${it.message}"
        YamlConfiguration() // Keep the failed installation visible and blocked.
    } else null
    private var waitingForRestart = false
    private var activeInitialBuild = false
    val phase: String? get() = state?.getString("phase")
    val installed: Boolean get() = state != null

    private fun folder(): File {
        require(worldName.matches(Regex("[a-z0-9_]+")))
        val root = plugin.server.levelDirectory.toFile().canonicalFile
        val folder = File(root, "dimensions/minecraft/$worldName")
        check(folder.canonicalFile == File(root, "dimensions/minecraft/$worldName").absoluteFile && !Files.isSymbolicLink(folder.toPath())) { "Unsafe managed-world path" }
        return folder
    }

    /** Loading requires both an existing world and our exact ownership token; missing files are never recreated. */
    fun loadExisting(): World? {
        if (!installed) return null
        return runCatching {
            val s = state!!
            check(s.getInt("schema") == 1 && s.getString("world") == worldName) { "Invalid managed-world manifest" }
            val directory = folder()
            check(directory.isDirectory) { "Managed dimension is missing; restore the world backup" }
            check(File(directory, ".cubecrafttd-owner").readText().trim() == s.getString("owner")) { "Managed-world ownership token mismatch" }
            val world = plugin.server.getWorld(worldName) ?: creator().createWorld() ?: error("Paper could not load $worldName")
            check(world.worldPath.toFile().canonicalFile == directory.canonicalFile) { "Managed dimension path mismatch" }
            check(world.uid.toString() == s.getString("uuid")) { "Managed-world UUID mismatch" }
            keepTerrainStable(world)
            loadError = null
            plugin.logger.info("Managed Farm world loaded: $worldName uuid=${world.uid} phase=$phase")
            world
        }.getOrElse {
            loadError = it.message ?: it.javaClass.simpleName
            plugin.logger.severe("Managed Farm world BLOCKED: $loadError")
            null
        }
    }

    /** Only invoked after ownership/path/UUID validation, or on our newly created world. */
    private fun keepTerrainStable(world: World) {
        check(world.setGameRule(org.bukkit.GameRules.RANDOM_TICK_SPEED, 0)) {
            "Cannot freeze random block updates in the managed Farm dimension"
        }
    }

    private fun creator() = WorldCreator.ofKey(org.bukkit.NamespacedKey.minecraft(worldName)).generator(VoidGenerator()).generateStructures(false)

    fun createEmpty(): World {
        check(!installed && !manifest.exists()) { "A managed Farm already exists; use /ctdinstall status" }
        val directory = folder()
        val legacyDirectory = File(plugin.server.worldContainer, worldName)
        check(!directory.exists() && !legacyDirectory.exists() && plugin.server.getWorld(worldName) == null) { "World $worldName already exists; it will not be adopted or overwritten" }
        val world = creator().createWorld() ?: error("Paper could not create $worldName")
        check(world.worldPath.toFile().canonicalFile == directory.canonicalFile) { "Paper returned an unexpected dimension path" }
        val token = java.util.UUID.randomUUID().toString()
        Files.writeString(File(directory, ".cubecrafttd-owner").toPath(), token, java.nio.file.StandardOpenOption.CREATE_NEW)
        state = YamlConfiguration().also {
            it.set("schema", 1)
            it.set("world", worldName)
            it.set("uuid", world.uid.toString())
            it.set("owner", token)
            it.set("phase", "CREATED")
        }
        // Only this new dedicated world receives environment settings.
        world.setGameRule(org.bukkit.GameRules.SPAWN_MOBS, false)
        keepTerrainStable(world)
        // Paper 26.2 NORMAL dimensions share a default world clock. Do not set time or freeze it.
        world.save()
        persist()
        return world
    }

    /** Called only by the explicit admin install command, while all TD queues/arenas are idle. */
    fun configure(teamSize: Int) {
        require(teamSize in 1..6) { "Team size must be between 1 and 6" }
        check(phase == "CREATED") { "Farm is already configured; use /ctdinstall status" }
        val world = loadExisting() ?: error(loadError ?: "Managed world not loaded")
        val configFile = File(plugin.dataFolder, "config.yml")
        Files.copy(configFile.toPath(), File(plugin.dataFolder, "config.before-farm-install-${System.currentTimeMillis()}.yml").toPath())
        try {
            plugin.config.set("farm-scene.enabled", true)
            plugin.config.set("tower-models.require-complete", true)
            plugin.config.set("queue.team-size", teamSize)
            plugin.config.set("map-binding.paste-blocks-per-tick", 4000)
            BukkitEngineeringPlaytestConfigurator(plugin).apply(world, ORIGIN)
            setPhase("PREPARED")
            waitingForRestart = true
        } catch (t: Throwable) {
            plugin.reloadConfig()
            throw t
        }
    }

    fun blocker(binding: PaperMapBindingConfig): String? {
        if (!installed) return null
        if (waitingForRestart) return "Farm installation requires a server restart"
        loadError?.let { return it }
        if (binding.worldName != worldName || binding.origin != ORIGIN || !ReconstructedFarmAssets.enabled(plugin))
            return "Managed Farm binding differs from its installation profile"
        if (phase != "READY") return "Managed Farm phase=$phase; check /ctdinstall status"
        return null
    }

    fun startPendingBuild(operations: FarmMapOperationManager, binding: PaperMapBindingConfig) {
        if (phase != "PREPARED" || waitingForRestart || loadError != null) return
        verifyBindingOwnership(binding)
        setPhase("BUILDING")
        activeInitialBuild = true
        try {
            var lastPercent = -1
            operations.startSafePaste(
                onProgress = { p ->
                    val percent = p.percentage ?: 0
                    if (percent / 10 != lastPercent / 10 || p.phase == "COMPLETE") {
                        lastPercent = percent
                        plugin.logger.info("Managed Farm build ${p.phase}: $percent%")
                    }
                },
                onComplete = { activeInitialBuild = false; markBuilt(binding) },
                onFailure = {
                    recordBuildFailure()
                    plugin.logger.severe("Managed Farm build failed: ${it.message}; installation is blocked. Repair with /ctdresetfarm 28d24136afe8 after checking the log")
                }
            )
        } catch (t: Throwable) {
            recordBuildFailure()
            throw t
        }
    }

    fun retryBuild(operations: FarmMapOperationManager, binding: PaperMapBindingConfig) {
        check(!waitingForRestart && loadError == null) { "Restart with a valid managed Farm before building" }
        check(phase == "PREPARED") { "A failed/interrupted build needs /ctdresetfarm 28d24136afe8; a READY world is never rebuilt" }
        check(!operations.hasActivePaste()) { "Farm map operation already active" }
        startPendingBuild(operations, binding)
    }

    fun needsVerifiedReset(): Boolean = installed && phase in setOf("BUILDING", "BUILD_FAILED")

    fun verifyBindingOwnership(binding: PaperMapBindingConfig) {
        if (!installed) return
        check(!waitingForRestart) { "Restart with the saved Farm configuration before map operations" }
        check(binding.worldName == worldName && binding.origin == ORIGIN && ReconstructedFarmAssets.enabled(plugin)) { "Managed Farm configuration differs from the installation profile" }
        check(loadExisting() != null) { loadError ?: "Managed world ownership could not be verified" }
    }

    private fun recordBuildFailure() {
        activeInitialBuild = false
        try { setPhase("BUILD_FAILED") } finally { onBuildFailure() }
    }

    /** Also used after the existing verified reset repairs an interrupted install. */
    fun markBuilt(binding: PaperMapBindingConfig) {
        if (!installed || binding.worldName != worldName || binding.origin != ORIGIN || !ReconstructedFarmAssets.enabled(plugin)) return
        val world = loadExisting() ?: error(loadError ?: "Managed world not loaded")
        world.setSpawnLocation(62, 104, -16, 0f)
        world.save()
        setPhase("READY")
        plugin.logger.info("Managed Farm READY: /ctdjoin; admin preview: /ctdinstall visit")
    }

    fun visitLocation(): Location {
        check(phase == "READY" && !waitingForRestart && loadError == null) { "Wait for Managed Farm READY before visiting" }
        val world = plugin.server.getWorld(worldName) ?: error("Managed Farm is not loaded")
        return Location(world, 62.5, 104.0, -15.5, 0f, 0f)
    }

    fun status(): String = "Managed Farm: world=$worldName phase=${phase ?: "NOT_INSTALLED"} restart=$waitingForRestart error=${loadError ?: "none"}"

    // A graceful disable cancels and rolls back the initial empty-region paste first.
    fun buildCancelledCleanly() {
        if (activeInitialBuild && phase == "BUILDING") setPhase("PREPARED")
        activeInitialBuild = false
    }

    private fun setPhase(value: String) {
        val previous = state!!.getString("phase")
        state!!.set("phase", value)
        try { persist() } catch (t: Throwable) {
            state!!.set("phase", previous)
            throw t
        }
    }

    private fun persist() {
        val temporary = Files.createTempFile(plugin.dataFolder.toPath(), "managed-farm-", ".tmp")
        try {
            Files.writeString(temporary, state!!.saveToString())
            Files.move(temporary, manifest.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private class VoidGenerator : ChunkGenerator() {
        override fun shouldGenerateNoise() = false
        override fun shouldGenerateSurface() = false
        override fun shouldGenerateCaves() = false
        override fun shouldGenerateDecorations() = false
        override fun shouldGenerateMobs() = false
        override fun shouldGenerateStructures() = false
    }

    companion object {
        val ORIGIN = BlockPos(0, 80, 0)

        /** CI-only opt-in: real empty-world creation, collision refusal and UUID/block reload on boot two. */
        fun runInstallSmoke(plugin: JavaPlugin): World? {
            if (!java.lang.Boolean.getBoolean("cubecrafttd.install-smoke")) return null
            val primary = plugin.server.worlds.first()
            val primaryRule = primary.getGameRuleValue(org.bukkit.GameRules.SPAWN_MOBS)
            val primaryRandomTicks = primary.getGameRuleValue(org.bukkit.GameRules.RANDOM_TICK_SPEED)
            val primarySpawn = primary.spawnLocation.clone()
            val primaryClock = primary.fullTime
            val manager = BukkitManagedFarmWorld(plugin, "ctd_install_smoke", "managed-farm-smoke.yml")
            val reload = manager.installed
            val world = if (reload) manager.loadExisting() ?: error("Managed world reload failed") else manager.createEmpty()
            check(world.uid != primary.uid)
            check(world.getBlockAt(800, 80, 800).type.isAir) { "Void generator created terrain" }
            if (reload) check(world.getBlockAt(0, 80, 0).type == Material.EMERALD_BLOCK) { "Owned world did not survive restart" }
            else world.getBlockAt(0, 80, 0).setType(Material.EMERALD_BLOCK, false)
            check(world.getGameRuleValue(org.bukkit.GameRules.RANDOM_TICK_SPEED) == 0)
            // An owned world's reload also re-establishes stable terrain.
            check(world.setGameRule(org.bukkit.GameRules.RANDOM_TICK_SPEED, 7))
            check(manager.loadExisting() == world)
            check(world.getGameRuleValue(org.bukkit.GameRules.RANDOM_TICK_SPEED) == 0)
            world.save()
            check(runCatching { manager.createEmpty() }.isFailure) { "Installer recreated a managed world" }
            val collision = BukkitManagedFarmWorld(plugin, "ctd_install_smoke", "unclaimed-smoke.yml")
            check(runCatching { collision.createEmpty() }.isFailure) { "Installer adopted an existing unclaimed world" }
            check(primary.getGameRuleValue(org.bukkit.GameRules.SPAWN_MOBS) == primaryRule && primary.spawnLocation == primarySpawn)
            check(primary.getGameRuleValue(org.bukkit.GameRules.RANDOM_TICK_SPEED) == primaryRandomTicks) {
                "Managed world changed the primary world's random block ticks"
            }
            check(primary.fullTime == primaryClock) { "Installer changed the shared world clock" }
            plugin.logger.info("Managed Farm install smoke PASS: ${if (reload) "RELOADED" else "CREATED"}; world UUID, void generation, collision refusal, isolated random block ticks and primary-world settings checked")
            return world
        }
    }
}
