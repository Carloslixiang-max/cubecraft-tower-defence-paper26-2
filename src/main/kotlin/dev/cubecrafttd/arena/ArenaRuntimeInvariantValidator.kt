package dev.cubecrafttd.arena

import dev.cubecrafttd.tower.lifecycle.RecommendedMatureTowerSpacingRules

data class ArenaInvariantIssue(
    val code: String,
    val detail: String
)

data class ArenaInvariantReport(
    val issues: List<ArenaInvariantIssue>
) {
    val valid: Boolean get() = issues.isEmpty()
}

object ArenaRuntimeInvariantValidator {
    fun validate(
        context: ArenaContext
    ): ArenaInvariantReport {
        val issues=
            mutableListOf<ArenaInvariantIssue>()

        runCatching {
            context.entityIndex
                .assertConsistent()
        }.onFailure {
            issues += ArenaInvariantIssue(
                "ENTITY_INDEX_INCONSISTENT",
                it.message ?: "unknown"
            )
        }

        context.entityIndex
            .mobsByUuid.values
            .forEach { mob ->
                val owner =
                    context.mapRuntime
                        .routeOwnerById[
                            mob.route.routeId
                        ]
                if(owner !=
                    mob.identity.attackedTeam
                ) {
                    issues +=
                        ArenaInvariantIssue(
                            "MOB_ROUTE_WRONG_TEAM",
                            "mob=${mob.identity.instanceId.value}"
                        )
                }
                if(
                    mob.combat.health < 0.0 ||
                    mob.combat.health >
                        mob.combat.maxHealth
                ) {
                    issues +=
                        ArenaInvariantIssue(
                            "MOB_HEALTH_INVALID",
                            "mob=${mob.identity.instanceId.value}"
                        )
                }
            }

        context.castles.values.forEach {
            castle ->
            if(
                castle.health < 0.0 ||
                castle.health >
                    castle.maxHealth
            ) {
                issues += ArenaInvariantIssue(
                    "CASTLE_HEALTH_INVALID",
                    castle.team.name
                )
            }
        }

        context.entityIndex
            .towersByTeam
            .forEach { (team,ids) ->
                val leachCount=ids
                    .mapNotNull {
                        context.entityIndex
                            .towersByInstanceId[it]
                    }
                    .count {
                        it.identity.towerId==
                            "leach"
                    }
                if(leachCount>1) {
                    issues +=
                        ArenaInvariantIssue(
                            "LEACH_TEAM_LIMIT",
                            "$team=$leachCount"
                        )
                }
            }

        if(
            context.state==
                ArenaState.CLOSED
        ) {
            if(context.taskGroup.activeCount()!=0) {
                issues += ArenaInvariantIssue(
                    "CLOSED_WITH_TASKS",
                    context.taskGroup
                        .activeCount()
                        .toString()
                )
            }
            if(!context.entityIndex.isEmpty()) {
                issues += ArenaInvariantIssue(
                    "CLOSED_WITH_ENTITIES",
                    "index not empty"
                )
            }
            if(!context.towerBodyLedger.isEmpty()) {
                issues += ArenaInvariantIssue(
                    "CLOSED_WITH_BODY_LEDGER",
                    context.towerBodyLedger
                        .size().toString()
                )
            }
        }

        return ArenaInvariantReport(issues)
    }
}
