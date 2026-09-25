package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.tower.visual.TowerPath
import dev.cubecrafttd.truth.*
import java.util.UUID

object LeachChargeTickPhaseFixture {
    private fun <T> fallback(v:T)=
        ResolvedTruth(
            v,
            ResolutionSource
                .ENGINEERING_FALLBACK
        )

    fun run():List<FixtureResult> {
        val context=ArenaContext(
            ArenaId("leach-phase"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000020100"
            ),
            TestingMapFactory.minimal()
        )
        context.state=ArenaState.RUNNING
        val owner=UUID.fromString(
            "00000000-0000-0000-0000-000000020001"
        )
        val tower=TowerRuntimeState(
            TowerIdentity(
                TowerInstanceId(1),
                "leach",owner,TeamId.RED
            ),
            TowerUpgradeState(
                2,TowerPath.TOP
            ),
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
        context.entityIndex
            .registerTower(tower)

        val target=MobRuntimeState(
            MobIdentity(
                MobInstanceId(1),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000020010"
                ),
                "zombie",owner,
                TeamId.RED,1
            ),
            MobRouteState(
                "red-route",0,0.0,5.0
            ),
            MobCombatState(
                200.0,200.0,
                MobLifecycleState.MOVING
            )
        )
        context.entityIndex
            .registerMob(target)

        val phase=LeachChargeTickPhase(
            geometry=
                MobGeometryProvider {
                    _,_ ->
                    MobGeometryView(
                        5.0,true
                    )
                },
            config=LeachChargeConfig(
                fallback(2.0),
                fallback(1.0)
            ),
            lethalResolver=
                MobLethalHitResolver(
                    SlimeLethalConfig(
                        fallback(2)
                    )
                )
        )

        phase.tick(context)
        val after1=target.combat.health
        phase.tick(context)
        val after2=target.combat.health
        val metrics=
            phase.metricsSnapshot()

        context.entityIndex
            .unregisterTower(
                tower.identity.instanceId
            )
        phase.tick(context)
        val cleaned=
            phase.charge(
                tower.identity.instanceId
            )==0.0

        return listOf(
            FixtureResult(
                "leach-charges-before-beam",
                after1==200.0 &&
                    phase.order==55
            ),
            FixtureResult(
                "leach-beam-fires-on-full-charge",
                after2==95.0 &&
                    metrics.beamsFired==1 &&
                    metrics.beamTargets==1
            ),
            FixtureResult(
                "leach-charge-state-cleans-with-tower",
                cleaned
            )
        )
    }
}
