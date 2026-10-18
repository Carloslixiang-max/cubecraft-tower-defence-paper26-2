package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.FarmReuseGatePersistentState
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID

/** Corrupt residue evidence must block reuse, never silently become an empty/clean gate. */
class BukkitFarmReuseGatePersistence(
    private val file: File,
    private val reportFailure: (String) -> Unit = {}
) {
    private var corruptSource = false
    fun load(): FarmReuseGatePersistentState {
        if (Files.notExists(file.toPath(), java.nio.file.LinkOption.NOFOLLOW_LINKS)) return FarmReuseGatePersistentState()
        return runCatching {
            check(file.isFile) { "Reuse gate is not a regular file" }
            val yaml = YamlConfiguration().also { it.load(file) }
            if (yaml.isSet("schema")) check(yaml.get("schema") == 1) { "Unsupported reuse-gate schema" }
            fun strings(key: String): List<String> {
                val raw = yaml.get(key) as? List<*> ?: error("Missing/invalid reuse-gate list: $key")
                return raw.map { value ->
                    (value as? String)?.takeIf { it.isNotBlank() } ?: error("Invalid reuse-gate entry: $key")
                }
            }
            fun flag(key: String): Boolean =
                if (!yaml.isSet(key)) false else yaml.get(key) as? Boolean ?: error("Invalid reuse-gate flag: $key")
            FarmReuseGatePersistentState(
                hardTowerConflictKeys = strings("hard-tower-conflicts").toSet(),
                suspectTrackedEntities = strings("suspect-tracked-entities").map(UUID::fromString).toSet(),
                verifiedResetInProgress = flag("verified-reset-in-progress"),
                uncleanRestartSuspectedResidue = flag("unclean-restart-suspected-residue")
            )
        }.getOrElse {
            corruptSource = true
            reportFailure("Farm reuse evidence is unreadable; reuse remains BLOCKED until verified reset: ${it.message}")
            FarmReuseGatePersistentState(uncleanRestartSuspectedResidue = true)
        }
    }

    fun save(state: FarmReuseGatePersistentState) {
        file.parentFile?.mkdirs()
        if (corruptSource) {
            check(file.isFile) { "Unreadable reuse evidence must be restored before it can be replaced" }
            val backup = File(file.parentFile, file.name + ".corrupt-" + UUID.randomUUID() + ".bak")
            Files.copy(file.toPath(), backup.toPath())
            corruptSource = false
            reportFailure("Original unreadable Farm reuse evidence preserved in ${backup.name}")
        }
        val yaml = YamlConfiguration()
        yaml.set("schema", 1)
        yaml.set("hard-tower-conflicts", state.hardTowerConflictKeys.sorted())
        yaml.set("suspect-tracked-entities", state.suspectTrackedEntities.map(UUID::toString).sorted())
        yaml.set("verified-reset-in-progress", state.verifiedResetInProgress)
        yaml.set("unclean-restart-suspected-residue", state.uncleanRestartSuspectedResidue)
        val temporary = Files.createTempFile(file.toPath().toAbsolutePath().parent, "farm-reuse-", ".tmp")
        try {
            Files.writeString(temporary, yaml.saveToString())
            Files.move(temporary, file.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }
}
