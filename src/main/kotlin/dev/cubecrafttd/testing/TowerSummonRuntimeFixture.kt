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
        val commands=mutableListOf<TowerSummonSpawnCommand>()
        var updates=0
        var removalAllowed=true
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
                object : TowerSummonSpawnPort {
                    override fun spawn(command: TowerSummonSpawnCommand): UUID {
                        val uuid=UUID(0, 122000L + commands.size)
                        commands += command
                        live += uuid
                        return uuid
                    }
                    override fun update(command: TowerSummonSpawnCommand, liveEntityUuid: UUID) {
                        check(liveEntityUuid in live)
                        updates++
                    }
                },
            removalPort=
                TowerSummonRemovalPort {
                    if (!removalAllowed) false else { live.remove(it); true }
                }
        )
        phase.tick(context)

        val sizeAfterSpawn=
            phase.registrySize()
        val liveAfterSpawn=
            live.size

        val ownedAfterSpawn = context.entityIndex.towerSummons == live
        phase.tick(context)
        val stableProjection = commands.size == 6 && updates == 12
        val oldGolem = commands.indexOfFirst { it.request.kind == SummonKind.IRON_GOLEM }
            .let { UUID(0, 122000L + it) }
        necro.upgrade.level = 3
        phase.tick(context)
        val shulkerReplaced = oldGolem !in live && commands.last().request.kind == SummonKind.SHULKER &&
            live.size == 6 && context.entityIndex.towerSummons == live

        context.entityIndex
            .unregisterTower(
                sorcerer.identity.instanceId
            )
        phase.tick(context)

        val soldClean = phase.registrySize()==2 && live.size==2 && context.entityIndex.towerSummons==live
        context.entityIndex.unregisterTower(baby.identity.instanceId)
        val beforeFailure = live.toSet()
        removalAllowed=false
        val failedRemoval=runCatching { phase.tick(context) }.isFailure
        val failureRetained = failedRemoval && live==beforeFailure &&
            context.entityIndex.towerSummons==beforeFailure && phase.registrySize()==2
        removalAllowed=true
        phase.tick(context)
        val retryClean=live.size==1 && phase.registrySize()==1 && context.entityIndex.towerSummons==live

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

        // A normal Zeus I/II or chain-lightning branch has no child at all.
        // Previously its count resolver threw and aborted the running arena.
        val counts=RecommendedTowerSummonCountResolver.withExplicitSorcererCounts(mapOf(
            "sorcerer.none.l1" to 1, "sorcerer.none.l2" to 1, "sorcerer.bottom.l3" to 2))
        val zeus=RecommendedMatureTowerDefinitions.get("zeus")
        val preBranchSafe=(1..2).all { level -> TowerPath.entries.all { path ->
            counts.desiredCount("zeus", TowerStageResolver.resolve(zeus, path, level))==0
        } }
        val chainSafe=(3..4).all { level ->
            counts.desiredCount("zeus", TowerStageResolver.resolve(zeus, TowerPath.TOP, level))==0 &&
            counts.desiredCount("zeus", TowerStageResolver.resolve(zeus, TowerPath.BOTTOM, level))==1
        }

        return listOf(
            FixtureResult("summon-zeus-prebranch-stages-do-not-abort-arena", preBranchSafe),
            FixtureResult("summon-zeus-only-baby-branch-has-child", chainSafe),
            FixtureResult("summon-owned-by-arena-teardown-index", ownedAfterSpawn),
            FixtureResult("summon-stable-projection-without-respawn", stableProjection),
            FixtureResult("summon-necromancer-kind-replaced-at-constant-count", shulkerReplaced),
            FixtureResult("summon-failed-removal-retains-both-ownership-records", failureRetained),
            FixtureResult("summon-failed-removal-can-retry-without-orphans", retryClean),
            FixtureResult(
                "summon-maintenance-counts",
                sizeAfterSpawn==6 &&
                    liveAfterSpawn==6
            ),
            FixtureResult(
                "summon-maintenance-cleans-sold-tower",
                soldClean
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
