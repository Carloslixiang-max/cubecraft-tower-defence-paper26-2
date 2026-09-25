package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.mob.*
import java.util.UUID

object PlayerSentMobDeathFinalizerFixture {
    private fun dead(
        id:Long,
        sender:UUID,
        attacked:TeamId,
        mobId:String="zombie"
    )=MobRuntimeState(
        MobIdentity(
            MobInstanceId(id),
            UUID.nameUUIDFromBytes(
                "death-finalizer-$id"
                    .toByteArray()
            ),
            mobId,
            sender,
            attacked,
            1
        ),
        MobRouteState(
            if(attacked==TeamId.RED)
                "red-route"
            else "blue-route",
            0,0.0,0.0
        ),
        MobCombatState(
            0.0,40.0,
            MobLifecycleState.DEAD
        )
    )

    fun run():List<FixtureResult> {
        val sender=
            UUID.fromString(
                "00000000-0000-0000-0000-000000027001"
            )
        val system=
            UUID.fromString(
                "00000000-0000-0000-0000-00000000a11d"
            )
        val context=ArenaContext(
            ArenaId("death-finalizer"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000027100"
            ),
            TestingMapFactory.minimal()
        ).also {
            it.state=ArenaState.RUNNING
            it.blueTeam.players += sender
            it.gameTick=500
        }

        val ledger=EconomyLedger()
        val finalizer=
            PlayerSentMobDeathFinalizer(
                SentMobExpRewardService(
                    RecommendedMatureMobDefinitions,
                    ledger,
                    PricingMode.NORMAL
                )
            )

        val playerMob=
            dead(
                1,sender,TeamId.RED
            )
        val systemMob=
            dead(
                2,system,TeamId.RED
            )
        context.entityIndex
            .registerMob(playerMob)
        context.entityIndex
            .registerMob(systemMob)

        val phase=
            MobLifecycleCleanupTickPhase(
                TrackedEntityRemovalPort {
                    true
                },
                finalizer
            )
        phase.tick(context)

        val exp=
            ledger.balance(
                EconomyAccount(
                    TeamId.BLUE,
                    sender,
                    EconomyCurrency.MATCH_EXP
                )
            )

        val failingContext=ArenaContext(
            ArenaId("death-finalizer-fail"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000027101"
            ),
            TestingMapFactory.minimal()
        ).also {
            it.state=ArenaState.RUNNING
            it.blueTeam.players += sender
            it.gameTick=600
        }
        val failedMob=
            dead(
                3,sender,TeamId.RED
            )
        failingContext.entityIndex
            .registerMob(failedMob)
        MobLifecycleCleanupTickPhase(
            TrackedEntityRemovalPort {
                false
            },
            finalizer
        ).tick(failingContext)

        return listOf(
            FixtureResult(
                "sent-mob-exp-awarded-on-final-removal",
                exp==7L &&
                    playerMob.combat.lifecycle==
                        MobLifecycleState.REMOVED
            ),
            FixtureResult(
                "system-armageddon-mob-no-player-exp",
                ledger.balance(
                    EconomyAccount(
                        TeamId.BLUE,
                        system,
                        EconomyCurrency.MATCH_EXP
                    )
                )==0L
            ),
            FixtureResult(
                "failed-live-removal-does-not-award-exp",
                failingContext.entityIndex
                    .mobsByUuid
                    .containsKey(
                        failedMob.identity.entityUuid
                    ) &&
                    phase.metricsSnapshot()
                        .finalizations==2
            )
        )
    }
}
