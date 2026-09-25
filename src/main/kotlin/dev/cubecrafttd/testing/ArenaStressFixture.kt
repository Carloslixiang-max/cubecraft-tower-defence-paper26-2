package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.tower.visual.TowerPath
import java.util.UUID

object ArenaStressFixture {
    private class StressGeometry(
        private val arenaMobIds: Set<UUID>,
        unrelatedCount: Int
    ) : MobGeometryProvider {
        var lookups: Int = 0
        private val unrelated=
            (1..unrelatedCount)
                .mapTo(linkedSetOf()) {
                    UUID.nameUUIDFromBytes(
                        "outside-$it".toByteArray()
                    )
                }

        override fun geometry(
            tower: TowerRuntimeState,
            mobUuid: UUID
        ): MobGeometryView? {
            lookups++
            check(mobUuid !in unrelated) {
                "Unrelated entity leaked into arena candidate work"
            }
            return if(mobUuid in arenaMobIds)
                MobGeometryView(
                    1000.0,true
                )
            else null
        }
    }

    fun run(): List<FixtureResult> {
        val context=ArenaContext(
            ArenaId("stress"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000010000"
            ),
            TestingMapFactory.minimal(),
            rng=ArenaDeterministicRng(1)
        )
        context.state=ArenaState.RUNNING
        context.gameTick=100

        val owner=
            UUID.fromString(
                "00000000-0000-0000-0000-000000010001"
            )

        repeat(60) { i ->
            val team=
                if(i<30) TeamId.RED
                else TeamId.BLUE
            context.entityIndex
                .registerTower(
                    TowerRuntimeState(
                        TowerIdentity(
                            TowerInstanceId(
                                (i+1).toLong()
                            ),
                            "mage",owner,team
                        ),
                        TowerUpgradeState(
                            1,TowerPath.TOP
                        ),
                        TowerGeometryState(
                            Vec3(
                                i.toDouble(),
                                0.0,0.0
                            ),
                            Vec3(
                                i.toDouble(),
                                0.0,0.0
                            ),
                            emptyList(),
                            "stress"
                        ),
                        cooldown=
                            TowerCooldownState(
                                100
                            )
                    )
                )
        }

        val mobIds=
            linkedSetOf<UUID>()
        repeat(240) { i ->
            val attackedTeam=
                if(i<120) TeamId.RED
                else TeamId.BLUE
            val uuid=
                UUID.nameUUIDFromBytes(
                    "stress-mob-$i"
                        .toByteArray()
                )
            mobIds += uuid
            context.entityIndex
                .registerMob(
                    MobRuntimeState(
                        MobIdentity(
                            MobInstanceId(
                                (i+1).toLong()
                            ),
                            uuid,"zombie",
                            owner,attackedTeam,1
                        ),
                        MobRouteState(
                            if(attackedTeam==
                                TeamId.RED)
                                "red-route"
                            else
                                "blue-route",
                            0,0.0,i.toDouble()
                        ),
                        MobCombatState(
                            40.0,40.0,
                            MobLifecycleState.MOVING
                        )
                    )
                )
        }

        val geometry=
            StressGeometry(
                mobIds,1000
            )
        val phase=TowerCombatTickPhase(
            geometry,
            TowerRuntimeConfigProvider {
                _,tower,engine ->
                engine.resolveConfig(
                    tower,
                    TargetPriorityPolicy.FIRST,
                    true
                )
            },
            TowerAttackPlannerProvider {
                _,_ ->
                DirectTowerAttackPlanners.mage()
            }
        )
        phase.tick(context)
        val m=phase.metricsSnapshot()

        val expectedLookups=
            60 * 120

        val invariant=
            ArenaRuntimeInvariantValidator
                .validate(context)

        return listOf(
            FixtureResult(
                "stress-60-towers-240-mobs",
                m.towersVisited==60 &&
                    m.candidatesBuilt==7200
            ),
            FixtureResult(
                "stress-1000-unrelated-zero-extra-work",
                geometry.lookups==
                    expectedLookups
            ),
            FixtureResult(
                "stress-out-of-range-no-attacks",
                m.attacksFired==0 &&
                    m.noTarget==60
            ),
            FixtureResult(
                "stress-arena-invariants-valid",
                invariant.valid
            )
        )
    }
}
