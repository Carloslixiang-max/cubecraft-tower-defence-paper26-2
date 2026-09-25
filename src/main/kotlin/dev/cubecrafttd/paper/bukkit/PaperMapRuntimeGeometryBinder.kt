package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.*
import dev.cubecrafttd.mob.TeamConfiguredRouteAssignmentPolicy

data class BoundFarmRuntime(
    val runtime: MapRuntimeDefinition,
    val routePolicy:
        TeamConfiguredRouteAssignmentPolicy
)

object PaperMapRuntimeGeometryBinder {
    fun bind(
        base: MapRuntimeDefinition,
        config: PaperMapBindingConfig
    ): BoundFarmRuntime {
        val routeIds=
            TeamId.entries.associateWith {
                team ->
                config.routeIdByAttackedTeam[
                    team
                ] ?: error(
                    "Missing route assignment for $team"
                )
            }

        val routePolicy=
            TeamConfiguredRouteAssignmentPolicy(
                routeIds
            )

        // Force validation now, before match start.
        TeamId.entries.forEach { team ->
            routePolicy.chooseRoute(
                base,team,
                dev.cubecrafttd.mob
                    .MobInstanceId(0)
            )
        }

        val configuredAnchors=
            TeamId.entries.associateWith {
                team ->
                val positions=
                    config.guardAnchors
                        .getValue(team)
                check(positions.size==2) {
                    "$team requires exactly 2 Guard anchors; found ${positions.size}"
                }
                positions.map {
                    GuardAnchor(
                        team=team,
                        position=it,
                        evidenceRefs=listOf(
                            "ENGINEERING_MAP_BINDING_CONFIG"
                        )
                    )
                }
            }

        val teamSpawns=
            base.teamSpawns.mapValues {
                (team,geometry) ->
                val configured=
                    config.playerSpawns[
                        team
                    ] ?: error(
                        "Missing player spawn for $team"
                    )
                geometry.copy(
                    playerSpawn=configured
                )
            }

        return BoundFarmRuntime(
            runtime=base.copy(
                guardAnchors=
                    configuredAnchors,
                teamSpawns=teamSpawns,
                evidenceRefs=
                    base.evidenceRefs +
                        "Configured Guard anchors/player spawns are ENGINEERING fallback, not original Farm truth"
            ),
            routePolicy=routePolicy
        )
    }
}
