package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.map.*
import dev.cubecrafttd.map.importer.MapRuntimeCompiler
import dev.cubecrafttd.mob.*
import java.util.UUID

object ArenaIsolationFixture {
    private fun context(id: String): ArenaContext {
        val map = MapRuntimeCompiler.compile(FarmFullRuntimeFixture.candidate())
        return ArenaContext(ArenaId(id), UUID.nameUUIDFromBytes(id.toByteArray()), map)
    }

    fun run(): List<FixtureResult> {
        val redSender = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val a = context("arena-a")
        val b = context("arena-b")

        val mobA = MobRuntimeState(
            MobIdentity(
                MobInstanceId(101),
                UUID.fromString("00000000-0000-0000-0000-000000000101"),
                "zombie", redSender, TeamId.BLUE, 1
            ),
            MobRouteState("blue_branch_high_x", 0, 0.0, 0.0),
            MobCombatState(100.0, 100.0, MobLifecycleState.MOVING)
        )
        val mobB = MobRuntimeState(
            MobIdentity(
                MobInstanceId(202),
                UUID.fromString("00000000-0000-0000-0000-000000000202"),
                "zombie", redSender, TeamId.BLUE, 1
            ),
            MobRouteState("blue_branch_high_x", 0, 0.0, 0.0),
            MobCombatState(100.0, 100.0, MobLifecycleState.MOVING)
        )

        a.entityIndex.registerMob(mobA)
        b.entityIndex.registerMob(mobB)
        a.entityIndex.assertConsistent()
        b.entityIndex.assertConsistent()

        val isolated = mobA.identity.entityUuid !in b.entityIndex.mobsByUuid &&
            mobB.identity.entityUuid !in a.entityIndex.mobsByUuid

        a.entityIndex.unregisterMob(mobA.identity.entityUuid)
        val bUnaffected = b.entityIndex.mobsByUuid.size == 1

        val world=
            UUID.fromString(
                "00000000-0000-0000-0000-000000046100"
            )
        val otherWorld=
            UUID.fromString(
                "00000000-0000-0000-0000-000000046200"
            )
        val playerA=
            UUID.fromString(
                "00000000-0000-0000-0000-000000046001"
            )
        val playerB=
            UUID.fromString(
                "00000000-0000-0000-0000-000000046002"
            )
        val playerC=
            UUID.fromString(
                "00000000-0000-0000-0000-000000046003"
            )
        val baseMap=
            TestingMapFactory.minimal()
        val farMap=
            MapRuntimeWorldTranslator
                .translate(
                    baseMap,
                    MapWorldTranslation(
                        1_000,0,0
                    )
                )

        fun reservation(
            id: String,
            targetWorld: UUID,
            players: Set<UUID>,
            map:
                MapRuntimeDefinition
        ) =
            ArenaSpatialReservation
                .fromMap(
                    ArenaId(id),
                    targetWorld,
                    players,
                    map
                )

        val overlapRegistry=
            ArenaIsolationRegistry()
        check(
            overlapRegistry.reserve(
                reservation(
                    "overlap-a",
                    world,
                    setOf(playerA),
                    baseMap
                )
            ) is
                ArenaReservationResult
                    .Accepted
        )
        val overlapRejected=
            overlapRegistry.reserve(
                reservation(
                    "overlap-b",
                    world,
                    setOf(playerB),
                    baseMap
                )
            ) is
                ArenaReservationResult
                    .Rejected

        val separatedRegistry=
            ArenaIsolationRegistry()
        val separatedA=
            separatedRegistry.reserve(
                reservation(
                    "separated-a",
                    world,
                    setOf(playerA),
                    baseMap
                )
            )
        val separatedB=
            separatedRegistry.reserve(
                reservation(
                    "separated-b",
                    world,
                    setOf(playerB),
                    farMap
                )
            )

        val differentWorldRegistry=
            ArenaIsolationRegistry()
        val differentWorldA=
            differentWorldRegistry
                .reserve(
                    reservation(
                        "world-a",
                        world,
                        setOf(playerA),
                        baseMap
                    )
                )
        val differentWorldB=
            differentWorldRegistry
                .reserve(
                    reservation(
                        "world-b",
                        otherWorld,
                        setOf(playerB),
                        baseMap
                    )
                )

        val playerRegistry=
            ArenaIsolationRegistry()
        check(
            playerRegistry.reserve(
                reservation(
                    "player-a",
                    world,
                    setOf(
                        playerA,
                        playerB
                    ),
                    baseMap
                )
            ) is
                ArenaReservationResult
                    .Accepted
        )
        val playerRejected=
            playerRegistry.reserve(
                reservation(
                    "player-b",
                    otherWorld,
                    setOf(
                        playerA,
                        playerC
                    ),
                    farMap
                )
            ) as
                ArenaReservationResult
                    .Rejected

        val releaseRegistry=
            ArenaIsolationRegistry()
        val releaseId=
            ArenaId("release-a")
        check(
            releaseRegistry.reserve(
                reservation(
                    releaseId.value,
                    world,
                    setOf(playerA),
                    baseMap
                )
            ) is
                ArenaReservationResult
                    .Accepted
        )
        val released=
            releaseRegistry.release(
                releaseId
            )
        val reserveAfterRelease=
            releaseRegistry.reserve(
                reservation(
                    "release-b",
                    world,
                    setOf(playerB),
                    baseMap
                )
            )

        val playerReleaseRegistry=
            ArenaIsolationRegistry()
        val playerReleaseId=
            ArenaId("player-release")
        check(
            playerReleaseRegistry.reserve(
                reservation(
                    playerReleaseId.value,
                    world,
                    setOf(playerA,playerB),
                    baseMap
                )
            ) is ArenaReservationResult.Accepted
        )
        val playerReleased=
            playerReleaseRegistry.releasePlayer(
                playerReleaseId,
                playerA
            )
        val playerReleaseSnapshot=
            playerReleaseRegistry
                .snapshot()
                .single()
        val releasedPlayerCanReserve=
            playerReleaseRegistry.reserve(
                reservation(
                    "player-release-next",
                    otherWorld,
                    setOf(playerA),
                    farMap
                )
            )

        return listOf(
            FixtureResult("arena-two-context-index-isolation", isolated),
            FixtureResult("arena-unregister-does-not-cross-context", bUnaffected),
            FixtureResult(
                "arena-reservation-overlap-same-world-rejected",
                overlapRejected
            ),
            FixtureResult(
                "arena-reservation-nonoverlap-same-world-accepted",
                separatedA is
                    ArenaReservationResult
                        .Accepted &&
                    separatedB is
                        ArenaReservationResult
                            .Accepted
            ),
            FixtureResult(
                "arena-reservation-same-space-different-world-accepted",
                differentWorldA is
                    ArenaReservationResult
                        .Accepted &&
                    differentWorldB is
                        ArenaReservationResult
                            .Accepted
            ),
            FixtureResult(
                "arena-reservation-shared-player-rejected-cross-world",
                playerRejected.conflicts
                    .any {
                        it is
                            ArenaIsolationConflict
                                .PlayerAlreadyReserved &&
                            it.playerUuid==
                                playerA
                    }
            ),
            FixtureResult(
                "arena-reservation-release-frees-space",
                released &&
                    releaseRegistry
                        .activeCount()==1 &&
                    reserveAfterRelease is
                        ArenaReservationResult
                            .Accepted
            ),
            FixtureResult(
                "arena-reservation-release-player-updates-membership",
                playerReleased &&
                    playerA !in
                        playerReleaseSnapshot.players &&
                    playerB in
                        playerReleaseSnapshot.players
            ),
            FixtureResult(
                "arena-reservation-released-player-can-enter-other-arena",
                releasedPlayerCanReserve is
                    ArenaReservationResult
                        .Accepted
            )
        )
    }
}
