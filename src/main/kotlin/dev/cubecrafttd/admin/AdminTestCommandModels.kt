package dev.cubecrafttd.admin

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.match.ArmageddonType
import dev.cubecrafttd.tower.visual.TowerPath

sealed interface AdminTestIntent {
    data class SpawnMob(
        val mobId: String,
        val level: Int,
        val attackedTeam: TeamId
    ) : AdminTestIntent

    data class BuildTower(
        val towerId: String,
        val level: Int,
        val path: TowerPath,
        val team: TeamId,
        val center: BlockPos
    ) : AdminTestIntent

    data class ForceArmageddon(
        val type: ArmageddonType
    ) : AdminTestIntent

    data object ForceEnd :
        AdminTestIntent

    data object Invariants :
        AdminTestIntent

    data object RunFixtures :
        AdminTestIntent

    data object MapCheck :
        AdminTestIntent
}

object AdminTestCommandParser {
    fun parse(
        args: List<String>
    ): AdminTestIntent {
        check(args.isNotEmpty()) {
            "Missing test command"
        }

        return when(
            args[0].lowercase()
        ) {
            "spawnmob" -> {
                check(args.size==4) {
                    "spawnmob <mob> <level> <red|blue>"
                }
                AdminTestIntent.SpawnMob(
                    args[1].lowercase(),
                    args[2].toInt(),
                    team(args[3])
                )
            }

            "tower" -> {
                check(args.size==8) {
                    "tower <id> <level> <top|bottom> <red|blue> <x> <y> <z>"
                }
                AdminTestIntent.BuildTower(
                    args[1].lowercase(),
                    args[2].toInt(),
                    when(
                        args[3].lowercase()
                    ) {
                        "top" ->
                            TowerPath.TOP
                        "bottom" ->
                            TowerPath.BOTTOM
                        else ->
                            error(
                                "Path must be top/bottom"
                            )
                    },
                    team(args[4]),
                    BlockPos(
                        args[5].toInt(),
                        args[6].toInt(),
                        args[7].toInt()
                    )
                )
            }

            "armageddon" -> {
                check(args.size==2)
                AdminTestIntent
                    .ForceArmageddon(
                        ArmageddonType.valueOf(
                            args[1].uppercase()
                        )
                    )
            }

            "forceend" ->
                AdminTestIntent.ForceEnd
            "invariants" ->
                AdminTestIntent.Invariants
            "fixtures" ->
                AdminTestIntent.RunFixtures
            "mapcheck" ->
                AdminTestIntent.MapCheck
            else ->
                error(
                    "Unknown test command ${args[0]}"
                )
        }
    }

    private fun team(
        value: String
    ): TeamId = when(
        value.lowercase()
    ) {
        "red" -> TeamId.RED
        "blue" -> TeamId.BLUE
        else -> error("Team must be red/blue")
    }
}
