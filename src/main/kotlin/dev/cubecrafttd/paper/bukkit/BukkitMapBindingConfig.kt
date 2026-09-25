package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.map.Vec3
import org.bukkit.Server
import org.bukkit.World
import org.bukkit.configuration.file.FileConfiguration

data class PaperMapBindingConfig(
    val worldName: String?,
    val origin: BlockPos?,
    val routeIdByAttackedTeam:
        Map<TeamId,String?>,
    val guardAnchors:
        Map<TeamId,List<Vec3>>,
    val playerSpawns:
        Map<TeamId,Vec3?>,
    val allowFarmPaste: Boolean,
    val pasteBlocksPerTick: Int
) {
    init {
        require(pasteBlocksPerTick > 0)
    }
}

object BukkitMapBindingConfigLoader {
    fun load(
        config: FileConfiguration
    ): PaperMapBindingConfig {
        val worldName =
            config.getString(
                "map-binding.world"
            )?.takeIf {
                it.isNotBlank()
            }

        val origin =
            if (
                config.isSet(
                    "map-binding.origin.x"
                ) &&
                config.isSet(
                    "map-binding.origin.y"
                ) &&
                config.isSet(
                    "map-binding.origin.z"
                )
            ) {
                BlockPos(
                    config.getInt(
                        "map-binding.origin.x"
                    ),
                    config.getInt(
                        "map-binding.origin.y"
                    ),
                    config.getInt(
                        "map-binding.origin.z"
                    )
                )
            } else null

        val routes=mapOf(
            TeamId.RED to
                config.getString(
                    "map-binding.route-assignment.red"
                )?.takeIf(String::isNotBlank),
            TeamId.BLUE to
                config.getString(
                    "map-binding.route-assignment.blue"
                )?.takeIf(String::isNotBlank)
        )

        val anchors=mapOf(
            TeamId.RED to
                parseAnchors(
                    config.getStringList(
                        "map-binding.guard-anchors.red"
                    )
                ),
            TeamId.BLUE to
                parseAnchors(
                    config.getStringList(
                        "map-binding.guard-anchors.blue"
                    )
                )
        )

        val playerSpawns=mapOf(
            TeamId.RED to
                parseOptionalVec3(
                    config.getString(
                        "map-binding.player-spawn.red"
                    )
                ),
            TeamId.BLUE to
                parseOptionalVec3(
                    config.getString(
                        "map-binding.player-spawn.blue"
                    )
                )
        )

        return PaperMapBindingConfig(
            worldName=worldName,
            origin=origin,
            routeIdByAttackedTeam=routes,
            guardAnchors=anchors,
            playerSpawns=playerSpawns,
            allowFarmPaste=
                config.getBoolean(
                    "map-binding.allow-farm-paste",
                    false
                ),
            pasteBlocksPerTick=
                config.getInt(
                    "map-binding.paste-blocks-per-tick",
                    4000
                ).coerceAtLeast(1)
        )
    }

    private fun parseAnchors(
        values: List<String>
    ): List<Vec3> =
        values.map(::parseVec3)

    private fun parseOptionalVec3(
        value: String?
    ): Vec3? =
        value
            ?.takeIf(String::isNotBlank)
            ?.let(::parseVec3)

    private fun parseVec3(
        raw: String
    ): Vec3 {
        val parts=raw.split(',')
            .map(String::trim)
        require(parts.size==3) {
            "Coordinate must be x,y,z: '$raw'"
        }
        return Vec3(
            parts[0].toDouble(),
            parts[1].toDouble(),
            parts[2].toDouble()
        )
    }
}

fun PaperMapBindingConfig.resolveWorld(
    server: Server
): World? =
    worldName?.let(server::getWorld)
