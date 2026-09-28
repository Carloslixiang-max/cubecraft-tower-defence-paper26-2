package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.ui.LifetimeWinProgress
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.UUID

/**
 * Minimal persistent Tower Defence progression currently needed by the
 * recovered Auto-centre rule.
 *
 * Only lifetime wins are stored. No losses, games played, points or ranks are
 * inferred because those persistence semantics have not been recovered here.
 */
class BukkitLifetimeWinStore(
    private val file: File
) {
    init {
        file.parentFile
            ?.mkdirs()
    }

    @Synchronized
    fun load(
        playerUuid: UUID
    ): Int {
        if(!file.exists()) {
            return 0
        }
        val yaml=
            YamlConfiguration
                .loadConfiguration(
                    file
                )
        return yaml
            .getInt(
                path(playerUuid),
                0
            )
            .coerceAtLeast(0)
    }

    @Synchronized
    fun recordWin(
        playerUuid: UUID
    ): Int {
        val yaml=
            if(file.exists()) {
                YamlConfiguration
                    .loadConfiguration(
                        file
                    )
            } else {
                YamlConfiguration()
            }

        val current=
            yaml.getInt(
                path(playerUuid),
                0
            ).coerceAtLeast(0)
        val next=
            LifetimeWinProgress
                .nextAfterWin(
                    current
                )
        yaml.set(
            path(playerUuid),
            next
        )
        yaml.save(file)
        return next
    }

    private fun path(
        playerUuid: UUID
    ): String =
        playerUuid.toString() +
            ".wins"
}
