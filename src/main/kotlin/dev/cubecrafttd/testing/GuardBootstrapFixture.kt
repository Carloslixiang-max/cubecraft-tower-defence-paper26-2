package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.castle.*
import dev.cubecrafttd.map.GuardAnchor
import dev.cubecrafttd.map.Vec3
import java.util.UUID

object GuardBootstrapFixture {
    private fun mapWithGuards() =
        TestingMapFactory.minimal().copy(
            guardAnchors=mapOf(
                TeamId.RED to listOf(
                    GuardAnchor(
                        TeamId.RED,
                        Vec3(1.0,2.0,3.0),
                        listOf("fixture")
                    ),
                    GuardAnchor(
                        TeamId.RED,
                        Vec3(4.0,2.0,3.0),
                        listOf("fixture")
                    )
                ),
                TeamId.BLUE to listOf(
                    GuardAnchor(
                        TeamId.BLUE,
                        Vec3(10.0,2.0,3.0),
                        listOf("fixture")
                    ),
                    GuardAnchor(
                        TeamId.BLUE,
                        Vec3(13.0,2.0,3.0),
                        listOf("fixture")
                    )
                )
            )
        )

    fun run(): List<FixtureResult> {
        val context=ArenaContext(
            ArenaId("guard-bootstrap"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000034100"
            ),
            mapWithGuards()
        )
        val requests=
            mutableListOf<GuardEntitySpawnRequest>()
        val report=
            GuardBootstrapService()
                .bootstrap(
                    context,
                    GuardEntitySpawnPort {
                        request ->
                        requests += request
                        UUID.nameUUIDFromBytes(
                            "guard-${request.guardInstanceId}"
                                .toByteArray()
                        )
                    }
                )

        val badMap=mapWithGuards()
        val bad=ArenaContext(
            ArenaId("guard-bootstrap-bad"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000034200"
            ),
            badMap.copy(
                guardAnchors=
                    badMap.guardAnchors +
                        (
                            TeamId.RED to
                                badMap.guardAnchors
                                    .getValue(
                                        TeamId.RED
                                    )
                                    .take(1)
                        )
            )
        )
        val rejectsBad=
            runCatching {
                GuardBootstrapService()
                    .bootstrap(
                        bad,
                        GuardEntitySpawnPort {
                            UUID.randomUUID()
                        }
                    )
            }.isFailure

        return listOf(
            FixtureResult(
                "guard-bootstrap-two-per-team",
                report.guardUuids.size==4 &&
                    report.guardsPerTeam[
                        TeamId.RED
                    ]==2 &&
                    report.guardsPerTeam[
                        TeamId.BLUE
                    ]==2
            ),
            FixtureResult(
                "guard-bootstrap-deterministic-ids-and-anchors",
                requests.map {
                    it.guardInstanceId
                }==listOf(1L,2L,3L,4L) &&
                    requests.take(2)
                        .all {
                            it.team==
                                TeamId.RED
                        } &&
                    requests.drop(2)
                        .all {
                            it.team==
                                TeamId.BLUE
                        }
            ),
            FixtureResult(
                "guard-bootstrap-rejects-non-two-anchor-map",
                rejectsBad
            )
        )
    }
}
