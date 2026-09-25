package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.ui.HotbarLayout
import dev.cubecrafttd.ui.HotbarLayoutPersistenceCodec
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID

class BukkitHotbarPreferenceStore(
    private val file: File
) {
    init {
        file.parentFile
            ?.mkdirs()
    }

    fun load(
        playerUuid: UUID
    ): HotbarLayout? {
        if(!file.exists())
            return null

        val yaml=
            YamlConfiguration
                .loadConfiguration(file)
        val section=
            yaml.getConfigurationSection(
                playerUuid.toString()
            ) ?: return null

        val values=
            section.getKeys(false)
                .mapNotNull { key ->
                    val value=
                        section.get(key)
                    if(value is Number)
                        key to value.toInt()
                    else null
                }
                .toMap()

        return HotbarLayoutPersistenceCodec
            .decode(values)
    }

    fun save(
        playerUuid: UUID,
        layout: HotbarLayout
    ) {
        val yaml=
            if(file.exists()) {
                YamlConfiguration
                    .loadConfiguration(file)
            } else {
                YamlConfiguration()
            }

        val root=
            playerUuid.toString()
        yaml.set(root,null)
        HotbarLayoutPersistenceCodec
            .encode(layout)
            .forEach { (action,slot) ->
                yaml.set(
                    "$root.$action",
                    slot
                )
            }
        yaml.save(file)
    }
}
