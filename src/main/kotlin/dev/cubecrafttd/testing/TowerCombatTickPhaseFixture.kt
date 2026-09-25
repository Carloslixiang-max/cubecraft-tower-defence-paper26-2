package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.tower.visual.TowerPath
import java.util.UUID

object TowerCombatTickPhaseFixture {
    private class CountingGeometry(
        private val relevant: Map<UUID,MobGeometryView>,
        unrelatedCount: Int
    ) : MobGeometryProvider {
        var lookups = 0
        private val unrelated = (1..unrelatedCount)
            .associate {
                UUID.nameUUIDFromBytes(
                    "unrelated-$it".toByteArray()
                ) to MobGeometryView(1.0,true)
            }

        override fun geometry(
            tower: TowerRuntimeState,
            mobUuid: UUID
        ): MobGeometryView? {
            lookups++
            return relevant[mobUuid]
                ?: unrelated[mobUuid]
        }
    }

    fun run(): List<FixtureResult> {
        val context = ArenaContext(
            ArenaId("perf-fixture"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000009001"
            ),
            TestingMapFactory.minimal(),
            rng = ArenaDeterministicRng(1234)
        )
        context.state = ArenaState.RUNNING
        context.gameTick = 100

        val owner =
            UUID.fromString(
                "00000000-0000-0000-0000-000000009002"
            )
        val tower = TowerRuntimeState(
            TowerIdentity(
                TowerInstanceId(1),
                "archer",owner,TeamId.RED
            ),
            TowerUpgradeState(
                2,TowerPath.BOTTOM
            ),
            TowerGeometryState(
                Vec3(0.0,0.0,2.0),
                Vec3(0.0,0.0,2.0),
                listOf(Vec3(0.0,0.0,2.0)),
                "fixture"
            ),
            cooldown=TowerCooldownState(100)
        )
        context.entityIndex.registerTower(tower)

        val relevant = linkedMapOf<UUID,MobGeometryView>()
        repeat(5) { i ->
            val uuid = UUID.nameUUIDFromBytes(
                "arena-mob-$i".toByteArray()
            )
            val mob = MobRuntimeState(
                MobIdentity(
                    MobInstanceId(i.toLong()+1),
                    uuid,"zombie",owner,
                    TeamId.RED,1
                ),
                MobRouteState(
                    "red-route",0,0.0,
                    (i+1).toDouble()
                ),
                MobCombatState(
                    40.0,40.0,
                    MobLifecycleState.MOVING
                )
            )
            context.entityIndex.registerMob(mob)
            relevant[uuid] =
                MobGeometryView(5.0,true)
        }

        val geometryWith1000Unrelated =
            CountingGeometry(relevant,1000)
        var feedbackCalls = 0
        val phase = TowerCombatTickPhase(
            geometryWith1000Unrelated,
            TowerRuntimeConfigProvider {
                _,t,engine ->
                engine.resolveConfig(
                    t,
                    TargetPriorityPolicy.FIRST,
                    requiresLineOfSight=true
                )
            },
            TowerAttackPlannerProvider {
                _,_ ->
                DirectTowerAttackPlanners.archer()
            },
            TowerAttackFeedbackPort {
                _,_,report ->
                if(
                    report.status ==
                        TowerAttackCycleStatus.FIRED
                ) {
                    feedbackCalls++
                }
            }
        )

        phase.tick(context)
        val metrics = phase.metricsSnapshot()

        val damaged = context.entityIndex.mobsByUuid
            .values
            .count { it.combat.health < 40.0 }

        return listOf(
            FixtureResult(
                "tower-combat-phase-visits-only-indexed-towers",
                metrics.towersVisited == 1
            ),
            FixtureResult(
                "1000-unrelated-entities-do-not-enter-candidate-work",
                geometryWith1000Unrelated.lookups == 5 &&
                    metrics.candidatesBuilt == 5
            ),
            FixtureResult(
                "tower-combat-phase-fires-one-attack",
                metrics.attacksFired == 1 &&
                    damaged == 1
            ),
            FixtureResult(
                "tower-combat-phase-first-target",
                context.entityIndex.mobsByUuid
                    .values
                    .maxBy { it.route.routeProgress }
                    .combat.health == 35.0
            ),
            FixtureResult(
                "tower-combat-feedback-fires-once",
                feedbackCalls == 1
            )
        )
    }
}
