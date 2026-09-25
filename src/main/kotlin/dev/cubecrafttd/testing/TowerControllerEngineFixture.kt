package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.combat.AttackBatchApplier
import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.tower.visual.TowerPath
import dev.cubecrafttd.truth.*
import java.util.UUID

object TowerControllerEngineFixture {
    fun run(): List<FixtureResult> {
        val owner =
            UUID.fromString(
                "00000000-0000-0000-0000-000000008001"
            )
        val index = ArenaEntityIndex()
        val m1 = MobRuntimeState(
            MobIdentity(
                MobInstanceId(1),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000000001"
                ),
                "zombie",owner,TeamId.BLUE,1
            ),
            MobRouteState("r",0,0.0,10.0),
            MobCombatState(
                40.0,40.0,
                MobLifecycleState.MOVING
            )
        )
        val m2 = MobRuntimeState(
            MobIdentity(
                MobInstanceId(2),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000000002"
                ),
                "zombie",owner,TeamId.BLUE,1
            ),
            MobRouteState("r",0,0.0,20.0),
            MobCombatState(
                40.0,40.0,
                MobLifecycleState.MOVING
            )
        )
        index.registerMob(m1)
        index.registerMob(m2)

        val tower = TowerRuntimeState(
            TowerIdentity(
                TowerInstanceId(900),
                "archer",
                owner,
                TeamId.BLUE
            ),
            TowerUpgradeState(
                2,
                TowerPath.BOTTOM
            ),
            TowerGeometryState(
                Vec3(0.0,0.0,0.0),
                Vec3(0.0,0.0,0.0),
                listOf(Vec3(0.0,0.0,0.0)),
                "fixture"
            )
        )
        val rng = ArenaDeterministicRng(99)
        val engine = TowerControllerEngine(
            rng,
            AttackBatchApplier(index)
        )
        val config = engine.resolveConfig(
            tower,
            TargetPriorityPolicy.FIRST,
            requiresLineOfSight = true
        )
        engine.arm(tower,100)

        val candidates = listOf(
            TowerTargetCandidate(
                m1.identity.entityUuid,
                m1.route.routeProgress,
                m1.form.let {
                    RecommendedMatureMobCombatProfiles
                        .get(it.formId)
                },
                5.0,true
            ),
            TowerTargetCandidate(
                m2.identity.entityUuid,
                m2.route.routeProgress,
                m2.form.let {
                    RecommendedMatureMobCombatProfiles
                        .get(it.formId)
                },
                6.0,true
            )
        )

        val before = engine.tryAttack(
            tower,99,candidates,config,
            DirectTowerAttackPlanners.archer()
        )
        val fired = engine.tryAttack(
            tower,100,candidates,config,
            DirectTowerAttackPlanners.archer()
        )
        val tooSoon = engine.tryAttack(
            tower,101,candidates,config,
            DirectTowerAttackPlanners.archer()
        )

        tower.lifecycle =
            TowerLifecycleState.MUTATING
        val inactive = engine.tryAttack(
            tower,200,candidates,config,
            DirectTowerAttackPlanners.archer()
        )

        val unresolvedTower = tower.copy(
            identity=tower.identity.copy(
                instanceId=TowerInstanceId(901)
            ),
            upgrade=TowerUpgradeState(
                4,TowerPath.TOP
            ),
            cooldown=TowerCooldownState()
        )
        val hardGate = try {
            engine.resolveConfig(
                unresolvedTower,
                TargetPriorityPolicy.RANDOM,
                true
            )
            false
        } catch (_: IllegalStateException) {
            true
        }
        val fallback = engine.resolveConfig(
            unresolvedTower,
            TargetPriorityPolicy.RANDOM,
            true,
            ResolvedTruth(
                1.0,
                ResolutionSource.ENGINEERING_FALLBACK
            )
        )

        return listOf(
            FixtureResult(
                "controller-cooldown-before-first-ready",
                before.status ==
                    TowerAttackCycleStatus.COOLDOWN
            ),
            FixtureResult(
                "controller-first-target-damages-max-route-progress",
                fired.status ==
                    TowerAttackCycleStatus.FIRED &&
                    fired.selectedTargetUuid ==
                        m2.identity.entityUuid &&
                    m2.combat.health == 35.0 &&
                    m1.combat.health == 40.0
            ),
            FixtureResult(
                "controller-advances-archer-bottom2-clock",
                fired.nextAttackTick == 140L &&
                    tooSoon.status ==
                        TowerAttackCycleStatus.COOLDOWN
            ),
            FixtureResult(
                "controller-mutation-state-blocks-attacks",
                inactive.status ==
                    TowerAttackCycleStatus.INACTIVE
            ),
            FixtureResult(
                "controller-unresolved-mature-rate-hard-gate",
                hardGate &&
                    fallback.timing.source ==
                        ResolutionSource.ENGINEERING_FALLBACK
            )
        )
    }
}
