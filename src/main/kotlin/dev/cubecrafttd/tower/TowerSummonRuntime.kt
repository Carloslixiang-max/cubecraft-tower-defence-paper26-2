package dev.cubecrafttd.tower

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.tower.visual.TowerPath
import java.util.UUID

@JvmInline
value class TowerSummonInstanceId(
    val value: Long
)

data class TowerSummonRuntimeState(
    val summonInstanceId:
        TowerSummonInstanceId,
    val towerInstanceId:
        TowerInstanceId,
    val kind: SummonKind,
    val liveEntityUuid: UUID
)

class TowerSummonRegistry {
    private val byId=
        linkedMapOf<
            TowerSummonInstanceId,
            TowerSummonRuntimeState
        >()
    private val byTower=
        linkedMapOf<
            TowerInstanceId,
            MutableSet<TowerSummonInstanceId>
        >()

    fun register(
        state: TowerSummonRuntimeState
    ) {
        check(
            state.summonInstanceId !in byId
        )
        byId[state.summonInstanceId]=
            state
        byTower.getOrPut(
            state.towerInstanceId
        ) { linkedSetOf() }
            .add(state.summonInstanceId)
    }

    fun forTower(
        towerId: TowerInstanceId
    ): List<TowerSummonRuntimeState> =
        byTower[towerId]
            .orEmpty()
            .mapNotNull(byId::get)
            .sortedBy {
                it.summonInstanceId.value
            }

    fun unregister(
        id: TowerSummonInstanceId
    ): TowerSummonRuntimeState? {
        val state=byId.remove(id)
            ?: return null
        byTower[state.towerInstanceId]
            ?.remove(id)
        if(
            byTower[state.towerInstanceId]
                ?.isEmpty()==true
        ) {
            byTower.remove(
                state.towerInstanceId
            )
        }
        return state
    }

    fun towerIds():
        Set<TowerInstanceId> =
        byTower.keys.toSet()

    fun size(): Int = byId.size
}

data class TowerSummonSpawnCommand(
    val tower: TowerRuntimeState,
    val request: SummonRequest,
    val ordinal: Int
)

fun interface TowerSummonSpawnPort {
    fun spawn(
        command: TowerSummonSpawnCommand
    ): UUID
}

fun interface TowerSummonRemovalPort {
    fun remove(
        liveEntityUuid: UUID
    ): Boolean
}

fun interface TowerSummonCountResolver {
    fun desiredCount(
        towerId: String,
        stage: TowerPathDefinition
    ): Int
}

object RecommendedTowerSummonCountResolver {
    fun withExplicitSorcererCounts(
        sorcererCounts:
            Map<String,Int>
    ): TowerSummonCountResolver =
        TowerSummonCountResolver {
            towerId,stage ->
            when(towerId) {
                "necromancer" -> 1
                "zeus" -> {
                    check(
                        "baby_zeus" in
                            stage.abilityTags
                    )
                    1
                }
                "sorcerer" -> {
                    if(
                        "kamikaze" in
                            stage.abilityTags
                    ) {
                        0
                    } else if(
                        "up_to_4_animals" in
                            stage.abilityTags
                    ) {
                        4
                    } else {
                        val key=
                            RuntimeSummonKey
                                .stage(
                                    towerId,
                                    stage
                                )
                        sorcererCounts[key]
                            ?: error(
                                "Sorcerer active summon count unresolved for $key"
                            )
                    }
                }
                else -> 0
            }
        }
}

object RuntimeSummonKey {
    fun stage(
        towerId: String,
        stage: TowerPathDefinition
    ): String =
        "$towerId." +
            stage.option.name.lowercase() +
            ".l${stage.level}"
}

data class TowerSummonMaintenanceMetrics(
    var towersVisited: Int = 0,
    var spawned: Int = 0,
    var removed: Int = 0
)

class TowerSummonMaintenancePhase(
    private val counts:
        TowerSummonCountResolver,
    private val spawnPort:
        TowerSummonSpawnPort,
    private val removalPort:
        TowerSummonRemovalPort,
    private val registry:
        TowerSummonRegistry =
        TowerSummonRegistry(),
    private val definitions:
        TowerDefinitionRepository =
        RecommendedMatureTowerDefinitions
) : ArenaTickPhase {
    override val order: Int =
        NormalArenaPhaseOrder
            .SUMMON_MAINTENANCE
    override val id: String =
        "tower-summons"

    private var nextId=1L
    private val metrics=
        TowerSummonMaintenanceMetrics()

    override fun tick(
        context: ArenaContext
    ) {
        val summonTowers=
            context.entityIndex
                .towersByInstanceId
                .values
                .filter {
                    it.lifecycle ==
                        TowerLifecycleState.ACTIVE &&
                    it.identity.towerId in
                        setOf(
                            "sorcerer",
                            "necromancer",
                            "zeus"
                        )
                }
                .sortedBy {
                    it.identity
                        .instanceId.value
                }

        val activeTowerIds=
            summonTowers
                .mapTo(linkedSetOf()) {
                    it.identity.instanceId
                }

        // Remove children of sold/destroyed towers.
        (
            registry.towerIds() -
                activeTowerIds
        ).forEach(::removeAllForTower)

        summonTowers.forEach { tower ->
            val path=tower.upgrade.path
                ?: error(
                    "Tower path unresolved"
                )
            val stage=
                TowerStageResolver.resolve(
                    definitions.get(
                        tower.identity.towerId
                    ),
                    path,
                    tower.upgrade.level
                )

            val desired=
                counts.desiredCount(
                    tower.identity.towerId,
                    stage
                )
            if(desired<=0) {
                removeAllForTower(
                    tower.identity.instanceId
                )
                return@forEach
            }

            metrics.towersVisited++
            val current=
                registry.forTower(
                    tower.identity.instanceId
                ).toMutableList()

            val request=when(
                tower.identity.towerId
            ) {
                "sorcerer" ->
                    SpecialTowerProcessors
                        .sorcererSummon(
                            stage,null,
                            TowerAttackContext(
                                tower.identity
                                    .instanceId.value,
                                tower.identity
                                    .ownerUuid,
                                context.gameTick
                            )
                        )
                "necromancer" ->
                    SpecialTowerProcessors
                        .necromancerSummon(
                            stage,null,
                            TowerAttackContext(
                                tower.identity
                                    .instanceId.value,
                                tower.identity
                                    .ownerUuid,
                                context.gameTick
                            )
                        )
                "zeus" ->
                    SpecialTowerProcessors
                        .babyZeusSummon(
                            stage,null,
                            TowerAttackContext(
                                tower.identity
                                    .instanceId.value,
                                tower.identity
                                    .ownerUuid,
                                context.gameTick
                            )
                        )
                else ->
                    error("Not a summon tower")
            }

            while(current.size < desired) {
                val ordinal=current.size
                val uuid=spawnPort.spawn(
                    TowerSummonSpawnCommand(
                        tower,request,ordinal
                    )
                )
                val state=
                    TowerSummonRuntimeState(
                        TowerSummonInstanceId(
                            nextId++
                        ),
                        tower.identity
                            .instanceId,
                        request.kind,
                        uuid
                    )
                registry.register(state)
                current += state
                metrics.spawned++
            }

            while(current.size > desired) {
                val state=current.removeLast()
                removalPort.remove(
                    state.liveEntityUuid
                )
                registry.unregister(
                    state.summonInstanceId
                )
                metrics.removed++
            }
        }
    }

    private fun removeAllForTower(
        towerId: TowerInstanceId
    ) {
        registry.forTower(towerId)
            .asReversed()
            .forEach { state ->
                removalPort.remove(
                    state.liveEntityUuid
                )
                registry.unregister(
                    state.summonInstanceId
                )
                metrics.removed++
            }
    }

    fun registrySize(): Int =
        registry.size()

    fun metricsSnapshot():
        TowerSummonMaintenanceMetrics =
        metrics.copy()
}
