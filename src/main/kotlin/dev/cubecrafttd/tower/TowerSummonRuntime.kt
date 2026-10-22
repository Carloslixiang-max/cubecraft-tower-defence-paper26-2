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

    /** Refresh the same visual after a tower upgrade; never performs combat. */
    fun update(command: TowerSummonSpawnCommand, liveEntityUuid: UUID) {}
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
                    if ("baby_zeus" in stage.abilityTags) 1 else 0
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
        ).forEach { removeAllForTower(context, it) }

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
                    context, tower.identity.instanceId
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

            // A count of one is unchanged when Necromancer becomes a Shulker
            // or Snowman. Replace the old kind rather than retaining its UUID.
            if (current.any { it.kind != request.kind }) {
                removeAllForTower(context, tower.identity.instanceId)
                current.clear()
            }

            while(current.size < desired) {
                val ordinal=current.size
                val uuid=spawnPort.spawn(
                    TowerSummonSpawnCommand(
                        tower,request,ordinal
                    )
                )
                // Teardown must own the entity even if later registration or
                // projection throws. A private phase registry is insufficient.
                check(context.entityIndex.towerSummons.add(uuid)) {
                    "Summon spawn returned an already-owned UUID: $uuid"
                }
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
                remove(context, state)
            }
            current.forEachIndexed { ordinal, state ->
                spawnPort.update(TowerSummonSpawnCommand(tower, request, ordinal), state.liveEntityUuid)
            }
        }
    }

    private fun removeAllForTower(context: ArenaContext, towerId: TowerInstanceId) {
        registry.forTower(towerId).asReversed().forEach { remove(context, it) }
    }

    private fun remove(context: ArenaContext, state: TowerSummonRuntimeState) {
        // On failure retain BOTH records: the arena failure path can retry and
        // report residue instead of forgetting the entity and declaring clean.
        check(removalPort.remove(state.liveEntityUuid)) {
            "Failed to remove tower summon ${state.liveEntityUuid}"
        }
        registry.unregister(state.summonInstanceId)
        context.entityIndex.towerSummons.remove(state.liveEntityUuid)
        metrics.removed++
    }

    fun registrySize(): Int =
        registry.size()

    fun metricsSnapshot():
        TowerSummonMaintenanceMetrics =
        metrics.copy()
}
