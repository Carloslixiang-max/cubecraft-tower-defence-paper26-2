package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.MapRuntimeDefinition
import org.bukkit.Location
import org.bukkit.World
import java.util.UUID

data class PlayerPlacementFailure(
    val playerUuid: UUID,
    val reason: String
)

data class PlayerPlacementReport(
    val placed: Set<UUID>,
    val failures:
        List<PlayerPlacementFailure>
) {
    val success: Boolean
        get() = failures.isEmpty()
}

/**
 * Placement has no recovery side effects.
 *
 * Durable snapshots already exist before this service runs. If any placement
 * fails, the owning match controller aborts the arena and invokes the single
 * MatchEnd/ArenaTeardown recovery path exactly once.
 */
class BukkitMatchPlayerPlacementService(
    private val world: World
) {
    fun placeTeams(
        runtime: MapRuntimeDefinition,
        redPlayers: Set<UUID>,
        bluePlayers: Set<UUID>
    ): PlayerPlacementReport {
        val placed=linkedSetOf<UUID>()
        val failures=
            mutableListOf<PlayerPlacementFailure>()

        fun place(
            team: TeamId,
            uuid: UUID
        ) {
            val player=
                world.server.getPlayer(uuid)
            if(
                player==null ||
                !player.isOnline
            ) {
                failures +=
                    PlayerPlacementFailure(
                        uuid,
                        "player is not online"
                    )
                return
            }

            val point=
                runtime.teamSpawns
                    .getValue(team)
                    .playerSpawn
                    ?: run {
                        failures +=
                            PlayerPlacementFailure(
                                uuid,
                                "player spawn missing"
                            )
                        return
                    }

            val ok=player.teleport(
                Location(
                    world,
                    point.x,
                    point.y,
                    point.z
                )
            )

            if(ok) {
                placed += uuid
            } else {
                failures +=
                    PlayerPlacementFailure(
                        uuid,
                        "teleport returned false"
                    )
            }
        }

        redPlayers
            .sortedBy(UUID::toString)
            .forEach {
                place(TeamId.RED,it)
            }
        bluePlayers
            .sortedBy(UUID::toString)
            .forEach {
                place(TeamId.BLUE,it)
            }

        return PlayerPlacementReport(
            placed,failures
        )
    }
}
