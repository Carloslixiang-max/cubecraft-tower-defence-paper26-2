package dev.cubecrafttd.tower.visual

import dev.cubecrafttd.tower.RecommendedMatureTowerDefinitions
import dev.cubecrafttd.tower.TowerFootprint
import dev.cubecrafttd.tower.TowerStageResolver

data class TowerBodyStageKey(val towerId: String, val level: Int, val path: TowerPath)

/** Loaded once before bootstrap; absent stages are explicitly placeholders unless strict. */
class TowerBodyCatalog(
    definitions: List<TowerBodyDefinition>,
    private val requireComplete: Boolean = false
) : TowerBodyDefinitionProvider {
    private val byStage = linkedMapOf<TowerBodyStageKey, TowerBodyDefinition>()

    init {
        definitions.forEach { body ->
            val path = requireNotNull(body.path) { "Catalog models require an explicit path" }
            val tower = RecommendedMatureTowerDefinitions.get(body.towerId)
            TowerStageResolver.resolve(tower, path, body.level)
            require(body.evidenceStatus != BodyEvidenceStatus.ENGINEERING_PLACEHOLDER)
            require(body.bodyRevision.isNotBlank() && body.evidenceRefs.isNotEmpty())
            require(body.evidenceRefs.all { it.startsWith("https://") })
            val radius = if (tower.footprint == TowerFootprint.THREE_BY_THREE) 1 else 2
            require(body.blocks.isNotEmpty())
            require(body.blocks.map { it.pos }.distinct().size == body.blocks.size) { "Duplicate model voxel" }
            require(body.blocks.all {
                it.pos.x in -radius..radius && it.pos.z in -radius..radius &&
                    it.pos.y in 0..63 && it.blockData.startsWith("minecraft:")
            }) { "Model exceeds its footprint or bounded vertical envelope" }
            val key = TowerBodyStageKey(body.towerId, body.level, path)
            require(byStage.putIfAbsent(key, body) == null) { "Duplicate model stage: $key" }
        }
        if (requireComplete) require(missingStages().isEmpty()) { "Tower model catalog is incomplete: ${missingStages()}" }
    }

    fun loadedCount(): Int = byStage.size

    fun missingStages(): Set<TowerBodyStageKey> = buildSet {
        RecommendedMatureTowerDefinitions.all().forEach { (id, tower) ->
            TowerPath.entries.forEach { path ->
                (1..4).forEach { level ->
                    if (runCatching { TowerStageResolver.resolve(tower, path, level) }.isSuccess) {
                        val key = TowerBodyStageKey(id, level, path)
                        if (key !in byStage) add(key)
                    }
                }
            }
        }
    }

    override fun definition(towerId: String, level: Int, path: TowerPath): TowerBodyDefinition =
        byStage[TowerBodyStageKey(towerId, level, path)]
            ?: if (requireComplete) error("Missing tower model")
            else EngineeringPlaceholderTowerBodyProvider.definition(towerId, level, path)
}
