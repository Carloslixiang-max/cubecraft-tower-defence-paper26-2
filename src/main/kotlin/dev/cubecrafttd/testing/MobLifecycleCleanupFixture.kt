package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.mob.*
import java.util.UUID

object MobLifecycleCleanupFixture {
    private fun deadMob(
        id:Long,
        uuid:UUID
    )=MobRuntimeState(
        MobIdentity(
            MobInstanceId(id),
            uuid,
            "zombie",
            UUID.nameUUIDFromBytes(
                "owner-$id".toByteArray()
            ),
            TeamId.RED,
            1
        ),
        MobRouteState(
            "red-route",0,0.0,0.0
        ),
        MobCombatState(
            0.0,40.0,
            MobLifecycleState.DEAD
        )
    )

    fun run():List<FixtureResult> {
        val context=ArenaContext(
            ArenaId("cleanup"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000022100"
            ),
            TestingMapFactory.minimal()
        ).also {
            it.state=ArenaState.RUNNING
        }

        val a=UUID.fromString(
            "00000000-0000-0000-0000-000000022001"
        )
        val b=UUID.fromString(
            "00000000-0000-0000-0000-000000022002"
        )
        context.entityIndex
            .registerMob(deadMob(1,a))
        context.entityIndex
            .registerMob(deadMob(2,b))

        var bAttempts=0
        val phase=
            MobLifecycleCleanupTickPhase(
                TrackedEntityRemovalPort {
                    uuid ->
                    when(uuid) {
                        a -> true
                        b -> {
                            bAttempts++
                            bAttempts>=2
                        }
                        else -> false
                    }
                }
            )

        phase.tick(context)
        val first=
            phase.metricsSnapshot()

        val aGone=
            a !in context.entityIndex
                .mobsByUuid
        val bStill=
            b in context.entityIndex
                .mobsByUuid

        phase.tick(context)
        val second=
            phase.metricsSnapshot()

        return listOf(
            FixtureResult(
                "mob-cleanup-removes-successful-live-entity",
                aGone &&
                    bStill &&
                    first.removed==1 &&
                    first.failedRemovals==1
            ),
            FixtureResult(
                "mob-cleanup-retries-failed-live-removal",
                b !in context.entityIndex
                    .mobsByUuid &&
                    bAttempts==2 &&
                    second.removed==2
            ),
            FixtureResult(
                "mob-cleanup-phase-order",
                phase.order==90
            )
        )
    }
}
