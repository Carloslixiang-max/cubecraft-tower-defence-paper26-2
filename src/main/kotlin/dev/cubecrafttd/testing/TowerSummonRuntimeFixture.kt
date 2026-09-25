package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.mob.TargetLayer
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.tower.visual.TowerPath
import java.util.UUID

object TowerSummonRuntimeFixture {
    private val owner=UUID.fromString(
        "00000000-0000-0000-0000-000000021001"
    )

    private fun tower(
        id:Long,
        towerId:String,
        level:Int,
        path:TowerPath
    )=TowerRuntimeState(
        TowerIdentity(
            TowerInstanceId(id),
            towerId,owner,TeamId.RED
        ),
        TowerUpgradeState(level,path),
        TowerGeometryState(
            dev.cubecrafttd.map.Vec3(
                0.0,0.0,0.0
            ),
            dev.cubecrafttd.map.Vec3(
                0.0,0.0,0.0
            ),
            emptyList(),"fixture"
        )
    )

    fun run():List<FixtureResult> {
        val context=ArenaContext(
            ArenaId("summons"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000021100"
            ),
            TestingMapFactory.minimal()
        )
        context.state=ArenaState.RUNNING

        val sorcerer=tower(
            1,"sorcerer",4,
            TowerPath.BOTTOM
        )
        val necro=tower(
            2,"necromancer",1,
            TowerPath.TOP
        )
        val baby=tower(
            3,"zeus",3,
            TowerPath.BOTTOM
        )
        listOf(sorcerer,necro,baby)
            .forEach(
                context.entityIndex::registerTower
            )

        val live=linkedSetOf<UUID>()
        val phase=TowerSummonMaintenancePhase(
            counts=
                RecommendedTowerSummonCountResolver
                    .withExplicitSorcererCounts(
                        mapOf(
                            "sorcerer.none.l1" to 1,
                            "sorcerer.none.l2" to 1,
                            "sorcerer.bottom.l3" to 2
                        )
                    ),
            spawnPort=
                TowerSummonSpawnPort {
                    val uuid=
                        UUID.nameUUIDFromBytes(
                            "child-${it.tower.identity.instanceId.value}-${it.ordinal}"
                                .toByteArray()
                        )
                    live += uuid
                    uuid
                },
            removalPort=
                TowerSummonRemovalPort {
                    live.remove(it)
                    true
                }
        )
        phase.tick(context)

        val sizeAfterSpawn=
            phase.registrySize()
        val liveAfterSpawn=
            live.size

        context.entityIndex
            .unregisterTower(
                sorcerer.identity.instanceId
            )
        phase.tick(context)

        val sorcererL4=
            TowerStageResolver.resolve(
                RecommendedMatureTowerDefinitions
                    .get("sorcerer"),
                TowerPath.BOTTOM,4
            )
        val necroBase=
            TowerStageResolver.resolve(
                RecommendedMatureTowerDefinitions
                    .get("necromancer"),
                TowerPath.TOP,1
            )
        val necroShulker=
            TowerStageResolver.resolve(
                RecommendedMatureTowerDefinitions
                    .get("necromancer"),
                TowerPath.TOP,3
            )

        return listOf(
            FixtureResult(
                "summon-maintenance-counts",
                sizeAfterSpawn==6 &&
                    liveAfterSpawn==6
            ),
            FixtureResult(
                "summon-maintenance-cleans-sold-tower",
                phase.registrySize()==2 &&
                    live.size==2
            ),
            FixtureResult(
                "sorcerer-stage-ground-only",
                sorcererL4
                    .targetLayersOverride==
                    setOf(TargetLayer.GROUND)
            ),
            FixtureResult(
                "necromancer-stage-target-layers",
                necroBase
                    .targetLayersOverride==
                    setOf(TargetLayer.GROUND) &&
                    necroShulker
                        .targetLayersOverride==
                        setOf(
                            TargetLayer.GROUND,
                            TargetLayer.AIR
                        )
            ),
            FixtureResult(
                "summon-phase-order",
                phase.order==57
            )
        )
    }
}
