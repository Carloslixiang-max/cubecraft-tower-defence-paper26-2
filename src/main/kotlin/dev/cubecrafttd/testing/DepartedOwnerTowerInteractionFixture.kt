package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.tower.lifecycle.*
import dev.cubecrafttd.tower.visual.TowerPath
import java.util.UUID

object DepartedOwnerTowerInteractionFixture {
    fun run(): List<FixtureResult> {
        val owner=UUID.fromString(
            "00000000-0000-0000-0000-000000051201"
        )
        val teammate=UUID.fromString(
            "00000000-0000-0000-0000-000000051202"
        )
        val enemy=UUID.fromString(
            "00000000-0000-0000-0000-000000051203"
        )
        val context=ArenaContext(
            ArenaId("departed-owner"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000051299"
            ),
            TestingMapFactory.minimal()
        )
        context.redTeam.players +=
            setOf(owner,teammate)
        context.blueTeam.players += enemy

        val departed=linkedSetOf<UUID>()
        val policy=
            DepartedOwnerTeamTowerInteractionPolicy(
                context,
                departed
            )
        val tower=
            TowerRuntimeState(
                identity=
                    TowerIdentity(
                        TowerInstanceId(51L),
                        "archer",
                        owner,
                        TeamId.RED
                    ),
                upgrade=
                    TowerUpgradeState(
                        1,
                        TowerPath.TOP
                    ),
                geometry=
                    TowerGeometryState(
                        baseOrigin=
                            Vec3(0.0,0.0,0.0),
                        rangeOrigin=
                            Vec3(0.0,0.0,0.0),
                        firingOrigins=
                            listOf(
                                Vec3(0.0,0.0,0.0)
                            ),
                        bodyRevision="fixture"
                    )
            )

        val ownerAllowed=
            policy.mayUpgrade(owner,tower) &&
                policy.maySell(owner,tower)
        val teammateBlockedBefore=
            !policy.mayUpgrade(
                teammate,tower
            ) &&
                !policy.maySell(
                    teammate,tower
                )

        departed += owner
        val teammateAllowedAfter=
            policy.mayUpgrade(
                teammate,tower
            ) &&
                policy.maySell(
                    teammate,tower
                )
        val enemyBlocked=
            !policy.mayUpgrade(
                enemy,tower
            ) &&
                !policy.maySell(
                    enemy,tower
                )

        departed += teammate
        val departedTeammateBlocked=
            !policy.mayUpgrade(
                teammate,tower
            ) &&
                !policy.maySell(
                    teammate,tower
                )

        return listOf(
            FixtureResult(
                "departed-owner-policy-owner-normal-access",
                ownerAllowed
            ),
            FixtureResult(
                "departed-owner-policy-teammate-blocked-while-owner-present",
                teammateBlockedBefore
            ),
            FixtureResult(
                "departed-owner-policy-teammate-can-manage-after-owner-leaves",
                teammateAllowedAfter
            ),
            FixtureResult(
                "departed-owner-policy-enemy-never-gains-access",
                enemyBlocked
            ),
            FixtureResult(
                "departed-owner-policy-departed-teammate-not-active-manager",
                departedTeammateBlocked
            )
        )
    }
}
