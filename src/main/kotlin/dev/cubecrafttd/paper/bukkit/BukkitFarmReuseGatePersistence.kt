package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.FarmReuseGatePersistentState
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID

class BukkitFarmReuseGatePersistence(
    private val file: File
) {
    fun load():
        FarmReuseGatePersistentState {
        if(!file.isFile) {
            return FarmReuseGatePersistentState()
        }

        val yaml=
            YamlConfiguration
                .loadConfiguration(file)

        val hard=
            yaml.getStringList(
                "hard-tower-conflicts"
            ).filter {
                it.isNotBlank()
            }.toSet()

        val entities=
            yaml.getStringList(
                "suspect-tracked-entities"
            ).mapNotNull {
                runCatching {
                    UUID.fromString(it)
                }.getOrNull()
            }.toSet()

        return FarmReuseGatePersistentState(
            hardTowerConflictKeys=hard,
            suspectTrackedEntities=
                entities
        )
    }

    fun save(
        state:
            FarmReuseGatePersistentState
    ) {
        file.parentFile?.mkdirs()
        val yaml=
            YamlConfiguration()
        yaml.set(
            "hard-tower-conflicts",
            state.hardTowerConflictKeys
                .sorted()
        )
        yaml.set(
            "suspect-tracked-entities",
            state.suspectTrackedEntities
                .map(UUID::toString)
                .sorted()
        )
        yaml.save(file)
    }
}
