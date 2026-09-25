package dev.cubecrafttd.tower.visual

interface SchematicRepository {
    fun load(towerId: String, level: Int, path: TowerPath?, bodyRevision: String): TowerBodyDefinition
}
