package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.tower.visual.TowerPath
import java.util.UUID

object RecommendedTowerPlannerProviderFixture {
    private val owner=UUID.fromString(
        "00000000-0000-0000-0000-000000019001"
    )
    private val primary=UUID.fromString(
        "00000000-0000-0000-0000-000000019010"
    )
    private val extra=UUID.fromString(
        "00000000-0000-0000-0000-000000019011"
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
            emptyList(),
            "fixture"
        )
    )

    fun run():List<FixtureResult> {
        val context=ArenaContext(
            ArenaId("planner-provider"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000019100"
            ),
            TestingMapFactory.minimal(),
            rng=ArenaDeterministicRng(5)
        )
        val provider=
            RecommendedTowerAttackPlannerProvider(
                context.rng,
                AreaTowerTargetResolver {
                    _,_,_,_ ->
                    listOf(primary,extra)
                },
                ChainTowerTargetResolver {
                    _,_,_,_ ->
                    listOf(extra)
                }
            )
        val candidate=TowerTargetCandidate(
            primary,
            10.0,
            dev.cubecrafttd.mob
                .RecommendedMatureMobCombatProfiles
                .get("zombie"),
            5.0,true
        )
        val attackContext=
            TowerAttackContext(
                1,owner,100
            )

        val artilleryTower=
            tower(
                1,"artillery",2,
                TowerPath.TOP
            )
        val artilleryStage=
            TowerStageResolver.resolve(
                RecommendedMatureTowerDefinitions
                    .get("artillery"),
                TowerPath.TOP,2
            )
        val artillery=
            provider.planner(
                context,artilleryTower
            ).plan(
                artilleryStage,
                candidate,
                attackContext
            )

        val zeusTower=
            tower(
                2,"zeus",4,
                TowerPath.TOP
            )
        val zeusStage=
            TowerStageResolver.resolve(
                RecommendedMatureTowerDefinitions
                    .get("zeus"),
                TowerPath.TOP,4
            )
        val zeus=
            provider.planner(
                context,zeusTower
            ).plan(
                zeusStage,
                candidate,
                attackContext.copy(
                    towerInstanceId=2
                )
            )

        val turretTower=
            tower(
                3,"turret",3,
                TowerPath.BOTTOM
            )
        val turretStage=
            TowerStageResolver.resolve(
                RecommendedMatureTowerDefinitions
                    .get("turret"),
                TowerPath.BOTTOM,3
            )
        val turret=
            provider.planner(
                context,turretTower
            ).plan(
                turretStage,
                candidate,
                attackContext.copy(
                    towerInstanceId=3
                )
            )

        val sorcererTower=
            tower(
                4,"sorcerer",4,
                TowerPath.BOTTOM
            )
        val sorcererStage=
            TowerStageResolver.resolve(
                RecommendedMatureTowerDefinitions
                    .get("sorcerer"),
                TowerPath.BOTTOM,4
            )
        val sorcerer=
            provider.planner(
                context,sorcererTower
            ).plan(
                sorcererStage,
                candidate,
                attackContext.copy(
                    towerInstanceId=4
                )
            )

        val necroTower=
            tower(
                5,"necromancer",3,
                TowerPath.TOP
            )
        val necroStage=
            TowerStageResolver.resolve(
                RecommendedMatureTowerDefinitions
                    .get("necromancer"),
                TowerPath.TOP,3
            )
        val necro=
            provider.planner(
                context,necroTower
            ).plan(
                necroStage,
                candidate,
                attackContext.copy(
                    towerInstanceId=5
                )
            )

        val babyTower=
            tower(
                6,"zeus",3,
                TowerPath.BOTTOM
            )
        val babyStage=
            TowerStageResolver.resolve(
                RecommendedMatureTowerDefinitions
                    .get("zeus"),
                TowerPath.BOTTOM,3
            )
        val baby=
            provider.planner(
                context,babyTower
            ).plan(
                babyStage,
                candidate,
                attackContext.copy(
                    towerInstanceId=6
                )
            )

        return listOf(
            FixtureResult(
                "planner-artillery-area",
                artillery.impacts.size==2 &&
                    artillery.impacts
                        .any{
                            it.targetUuid==extra
                        }
            ),
            FixtureResult(
                "planner-zeus-chain",
                zeus.impacts.size==2 &&
                    zeus.impacts
                        .all{
                            it.damages.single()
                                .amount==40.0
                        }
            ),
            FixtureResult(
                "planner-turret-bounce",
                turret.impacts.size==2 &&
                    turret.impacts
                        .all{
                            it.damages.single()
                                .amount==30.0
                        }
            ),
            FixtureResult(
                "planner-sorcerer-effective-combat",
                sorcerer.metadata[
                    "combatModel"
                ]==
                    "ENGINEERING_EFFECTIVE_STAGE_STATS" &&
                    sorcerer.impacts.single()
                        .damages.single()
                        .amount==28.0
            ),
            FixtureResult(
                "planner-necromancer-effective-combat",
                necro.metadata[
                    "combatModel"
                ]==
                    "ENGINEERING_EFFECTIVE_STAGE_STATS" &&
                    necro.impacts.single()
                        .damages.single()
                        .amount==30.0
            ),
            FixtureResult(
                "planner-baby-zeus-effective-combat",
                baby.metadata[
                    "combatModel"
                ]==
                    "ENGINEERING_EFFECTIVE_STAGE_STATS" &&
                    baby.impacts.single()
                        .damages.single()
                        .amount==45.0
            )
        )
    }
}
