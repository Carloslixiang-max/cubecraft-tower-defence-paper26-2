package dev.cubecrafttd.map

import dev.cubecrafttd.arena.TeamId

data class ValidationIssue(val code: String, val message: String, val fatal: Boolean)

data class MapValidationReport(val issues: List<ValidationIssue>) {
    val isValid: Boolean get() = issues.none { it.fatal }
}

object MapRuntimeValidator {
    fun validate(map: MapRuntimeDefinition): MapValidationReport {
        val issues = mutableListOf<ValidationIssue>()

        if (map.routesById.isEmpty()) {
            issues += ValidationIssue("MAP-NO-ROUTES", "No runtime routes compiled", true)
        }

        TeamId.entries.forEach { team ->
            if (map.placementRegions[team] == null) {
                issues += ValidationIssue("MAP-NO-PLACEMENT-$team", "Missing placement region for $team", true)
            }
            if (map.teamSpawns[team]?.mobSpawn == null) {
                issues += ValidationIssue("MAP-NO-MOB-SPAWN-$team", "Missing mob spawn for $team", true)
            }
            if (map.terminalCrossSections[team] == null) {
                issues += ValidationIssue("MAP-NO-TERMINAL-$team", "Missing measured/reconstructed route terminal for $team", true)
            }
            if (map.castleContactCandidates[team] == null) {
                issues += ValidationIssue(
                    "MAP-NO-CONTACT-CANDIDATE-$team",
                    "No MOVING -> ATTACKING_CASTLE integration candidate for $team",
                    true
                )
            }

            // Exact original Castle/Guard geometry is explicitly optional.
            if (map.castleContactRegionsExact[team] == null) {
                issues += ValidationIssue(
                    "MAP-EXACT-CASTLE-UNKNOWN-$team",
                    "Original exact Castle contact region remains unknown; reconstruction candidate will be reported separately",
                    false
                )
            }
            if (map.guardAnchors[team].isNullOrEmpty()) {
                issues += ValidationIssue(
                    "MAP-GUARD-ANCHORS-UNKNOWN-$team",
                    "Original Guard anchor/firing geometry is unresolved and must not be inferred from Castle boundary",
                    false
                )
            }
        }

        map.routesById.forEach { (id, route) ->
            if (route.nodes.size < 2 || route.totalLength <= 0.0) {
                issues += ValidationIssue("ROUTE-INVALID-$id", "Route $id has invalid geometry", true)
            }
            if (route.segments.any { it.length <= 0.0 }) {
                issues += ValidationIssue("ROUTE-ZERO-SEGMENT-$id", "Route $id contains zero-length segment", true)
            }
            if (map.routeOwnerById[id] == null) {
                issues += ValidationIssue("ROUTE-NO-TEAM-$id", "Route $id has no owning/defending team", true)
            }
        }

        if (map.productionOriginal && map.authenticity != MapAuthenticity.ORIGINAL) {
            issues += ValidationIssue(
                "AUTH-PRODUCTION-MISMATCH",
                "productionOriginal=true is illegal for authenticity=${map.authenticity}",
                true
            )
        }

        if (!map.productionOriginal && map.authenticity == MapAuthenticity.ORIGINAL) {
            issues += ValidationIssue(
                "AUTH-ORIGINAL-FLAG-MISMATCH",
                "authenticity=ORIGINAL requires productionOriginal=true",
                true
            )
        }

        return MapValidationReport(issues)
    }
}
