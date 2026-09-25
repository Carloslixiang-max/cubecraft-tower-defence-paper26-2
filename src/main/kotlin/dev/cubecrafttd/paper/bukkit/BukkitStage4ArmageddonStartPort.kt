package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.ArenaContext
import dev.cubecrafttd.match.*
import org.bukkit.plugin.Plugin

class BukkitStage4ArmageddonStartPort(
    private val plugin: Plugin
) : ArmageddonStartPort {
    private val started=
        linkedMapOf<String,ArmageddonType>()

    override fun start(
        type: ArmageddonType,
        context: ArenaContext,
        gameTick: Long
    ) {
        check(
            started.putIfAbsent(
                context.arenaId.value,
                type
            )==null
        ) {
            "Armageddon already started for ${context.arenaId.value}"
        }

        plugin.logger.warning(
            "Stage-4 arena ${context.arenaId.value}: " +
                "Armageddon $type common activation started at gameTick=$gameTick; " +
                "type-specific live effects are NOT yet certified."
        )
    }

    fun startedType(
        arenaId: String
    ): ArmageddonType? =
        started[arenaId]

    fun clear(
        arenaId: String
    ) {
        started.remove(arenaId)
    }
}
