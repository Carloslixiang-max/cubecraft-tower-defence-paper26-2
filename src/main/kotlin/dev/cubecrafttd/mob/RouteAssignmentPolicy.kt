package dev.cubecrafttd.mob

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.MapRuntimeDefinition

interface RouteAssignmentPolicy {
    fun chooseRoute(
        map: MapRuntimeDefinition,
        attackedTeam: TeamId,
        mobInstanceId: MobInstanceId
    ): String
}

/**
 * Safe adapter for tests/admin-controlled route selection.
 * It does not claim to reproduce CubeCraft's unknown original Farm branch-selection rule.
 */
class ExplicitRouteAssignmentPolicy(
    private val routeId: String
) : RouteAssignmentPolicy {
    override fun chooseRoute(
        map: MapRuntimeDefinition,
        attackedTeam: TeamId,
        mobInstanceId: MobInstanceId
    ): String {
        require(map.routesById.containsKey(routeId)) { "Unknown routeId $routeId" }
        require(map.routeOwnerById[routeId] == attackedTeam) {
            "Route $routeId does not belong to defending team $attackedTeam"
        }
        return routeId
    }
}
