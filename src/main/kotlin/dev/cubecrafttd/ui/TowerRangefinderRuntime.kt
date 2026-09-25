package dev.cubecrafttd.ui

import dev.cubecrafttd.tower.TowerInstanceId

data class TowerRangefinderQuery(
    val hoveredTower: TowerInstanceId?,
    val sneaking: Boolean,
    val distanceSquaredByTower: Map<TowerInstanceId,Double>,
    val pinnedTowerIds: Set<TowerInstanceId>
)

object TowerRangefinderRuntime {
    /**
     * 2021 direct update evidence:
     * 1) hover over the top of a tower -> show that tower range;
     * 2) shift -> show nearest 10 tower ranges;
     * 3) tower menu can pin a tower range permanently.
     *
     * There is no direct evidence for a separate global on/off setting, so the
     * earlier invented global gate was removed in v87.
     */
    fun visibleTowerIds(
        query: TowerRangefinderQuery
    ): LinkedHashSet<TowerInstanceId> {
        val out=linkedSetOf<TowerInstanceId>()
        out += query.pinnedTowerIds

        if(query.sneaking) {
            query.distanceSquaredByTower
                .entries
                .sortedWith(
                    compareBy<Map.Entry<TowerInstanceId,Double>> {
                        it.value
                    }.thenBy {
                        it.key.value
                    }
                )
                .take(10)
                .forEach { out += it.key }
        } else {
            query.hoveredTower?.let { out += it }
        }
        return out
    }
}
