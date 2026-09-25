package dev.cubecrafttd.tower.lifecycle

import dev.cubecrafttd.arena.ArenaContext
import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.BlockPos
import kotlin.math.sqrt

enum class TowerSpacingDistanceMetric {
    EUCLIDEAN_3D_ENGINEERING
}

data class TowerSpacingRule(
    val towerId: String,
    val minimumBlocks: Double,
    val evidenceStatus: String
) {
    init { require(minimumBlocks >= 0.0) }
}

data class TowerSpacingConflict(
    val existingTowerInstanceId: Long,
    val distanceBlocks: Double,
    val minimumBlocks: Double
)

object RecommendedMatureTowerSpacingRules {
    val rules = mapOf(
        "ice" to TowerSpacingRule(
            "ice",12.0,"OFFICIAL_2017_HIGH"
        ),
        "mage" to TowerSpacingRule(
            "mage",8.0,"OFFICIAL_2017_HIGH"
        ),
        "poison" to TowerSpacingRule(
            "poison",15.0,"OFFICIAL_2017_HIGH"
        ),
        "quake" to TowerSpacingRule(
            "quake",10.0,"OFFICIAL_2017_HIGH"
        )
    )
}

class SameTowerSpacingPolicy(
    private val rules: Map<String,TowerSpacingRule> =
        RecommendedMatureTowerSpacingRules.rules,
    private val distanceMetric:
        TowerSpacingDistanceMetric =
        TowerSpacingDistanceMetric
            .EUCLIDEAN_3D_ENGINEERING
) {
    fun firstConflict(
        context: ArenaContext,
        towerId: String,
        team: TeamId,
        candidateCenter: BlockPos
    ): TowerSpacingConflict? {
        val rule = rules[towerId] ?: return null

        return context.entityIndex
            .towersByTeam.getValue(team)
            .asSequence()
            .mapNotNull {
                context.entityIndex
                    .towersByInstanceId[it]
            }
            .filter {
                it.identity.towerId == towerId
            }
            .map { existing ->
                val p = existing.geometry.baseOrigin
                val dx =
                    p.x - candidateCenter.x
                val dy =
                    p.y - candidateCenter.y
                val dz =
                    p.z - candidateCenter.z
                val distance =
                    sqrt(
                        dx*dx + dy*dy + dz*dz
                    )
                existing to distance
            }
            .filter {
                it.second + 1e-12 <
                    rule.minimumBlocks
            }
            .minByOrNull { it.second }
            ?.let { (tower,distance) ->
                TowerSpacingConflict(
                    tower.identity
                        .instanceId.value,
                    distance,
                    rule.minimumBlocks
                )
            }
    }

    fun requireLegal(
        context: ArenaContext,
        towerId: String,
        team: TeamId,
        candidateCenter: BlockPos
    ) {
        val conflict = firstConflict(
            context,towerId,team,candidateCenter
        ) ?: return
        error(
            "$towerId must be at least " +
                "${conflict.minimumBlocks} blocks " +
                "from same tower type; nearest=" +
                "${conflict.distanceBlocks}"
        )
    }
}
