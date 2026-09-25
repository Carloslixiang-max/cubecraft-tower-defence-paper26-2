package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.mob.*

object TeamRouteAssignmentFixture {
    fun run(): List<FixtureResult> {
        val map=TestingMapFactory.minimal()
        val policy=
            TeamConfiguredRouteAssignmentPolicy(
                mapOf(
                    TeamId.RED to "red-route",
                    TeamId.BLUE to "blue-route"
                )
            )

        val red=policy.chooseRoute(
            map,TeamId.RED,
            MobInstanceId(1)
        )
        val blue=policy.chooseRoute(
            map,TeamId.BLUE,
            MobInstanceId(2)
        )

        val wrongTeamBlocked=try {
            TeamConfiguredRouteAssignmentPolicy(
                mapOf(
                    TeamId.RED to "blue-route",
                    TeamId.BLUE to "blue-route"
                )
            ).chooseRoute(
                map,TeamId.RED,
                MobInstanceId(3)
            )
            false
        } catch (_: IllegalArgumentException) {
            true
        }

        val unknownBlocked=try {
            TeamConfiguredRouteAssignmentPolicy(
                mapOf(
                    TeamId.RED to "missing",
                    TeamId.BLUE to "blue-route"
                )
            ).chooseRoute(
                map,TeamId.RED,
                MobInstanceId(4)
            )
            false
        } catch (_: IllegalArgumentException) {
            true
        }

        val missingTeamBlocked=try {
            TeamConfiguredRouteAssignmentPolicy(
                mapOf(
                    TeamId.RED to "red-route"
                )
            )
            false
        } catch (_: IllegalArgumentException) {
            true
        }

        return listOf(
            FixtureResult(
                "route-policy-explicit-red-blue",
                red=="red-route" &&
                    blue=="blue-route"
            ),
            FixtureResult(
                "route-policy-wrong-team-blocked",
                wrongTeamBlocked
            ),
            FixtureResult(
                "route-policy-unknown-blocked",
                unknownBlocked
            ),
            FixtureResult(
                "route-policy-missing-team-blocked",
                missingTeamBlocked
            )
        )
    }
}
