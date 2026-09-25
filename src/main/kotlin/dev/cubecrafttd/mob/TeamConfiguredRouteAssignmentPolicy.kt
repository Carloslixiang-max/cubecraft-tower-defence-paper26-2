package dev.cubecrafttd.mob

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.MapRuntimeDefinition

/**
 * Explicit engineering bridge for maps where the original branch-selection
 * rule has not been recovered.
 *
 * The configured route IDs are validated against the compiled map and attacked
 * team. No "first route" or random default is hidden here.
 */
class TeamConfiguredRouteAssignmentPolicy(
    private val routeIdByAttackedTeam:
        Map<TeamId,String>
) : RouteAssignmentPolicy {
    init {
        require(
            routeIdByAttackedTeam.keys
                .containsAll(TeamId.entries)
        ) {
            "Every attacked team needs an explicit route ID"
        }
    }

    override fun chooseRoute(
        map: MapRuntimeDefinition,
        attackedTeam: TeamId,
        mobInstanceId: MobInstanceId
    ): String {
        val routeId =
            routeIdByAttackedTeam[attackedTeam]
                ?: error(
                    "Missing route for $attackedTeam"
                )
        require(
            map.routesById.containsKey(routeId)
        ) {
            "Unknown configured routeId $routeId"
        }
        require(
            map.routeOwnerById[routeId] ==
                attackedTeam
        ) {
            "Route $routeId does not belong to $attackedTeam"
        }
        return routeId
    }

    fun configuredRoute(
        attackedTeam: TeamId
    ): String =
        routeIdByAttackedTeam.getValue(
            attackedTeam
        )
}
