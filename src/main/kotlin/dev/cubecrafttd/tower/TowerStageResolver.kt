package dev.cubecrafttd.tower

import dev.cubecrafttd.tower.visual.TowerPath

object TowerStageResolver {
    fun resolve(
        definition: TowerDefinition,
        path: TowerPath,
        level: Int
    ): TowerPathDefinition {
        val option = when (path) {
            TowerPath.TOP -> TowerPathOption.TOP
            TowerPath.BOTTOM -> TowerPathOption.BOTTOM
        }
        return definition.levels.firstOrNull {
            it.level == level &&
                (it.option == option ||
                    it.option == TowerPathOption.NONE)
        } ?: error(
            "No numeric stage for ${definition.towerId} $path L$level"
        )
    }

    fun initial(
        definition: TowerDefinition,
        selectedPath: TowerPath
    ): TowerPathDefinition =
        definition.levels.firstOrNull {
            it.level == 1 &&
                it.option == TowerPathOption.NONE
        } ?: resolve(definition,selectedPath,1)
}
