package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.castle.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.troop.*
import dev.cubecrafttd.truth.*
import java.util.UUID

object NormalArenaLoopFixture {
    private fun <T> fallback(
        value: T
    )=ResolvedTruth(
        value,
        ResolutionSource.ENGINEERING_FALLBACK
    )

    fun run(): List<FixtureResult> {
        val context=ArenaContext(
            ArenaId("normal-loop"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000008000"
            ),
            TestingMapFactory.minimal()
        )
        context.state=ArenaState.RUNNING

        val cadence=
            TroopSpawnCadence(
                1L,
                "ENGINEERING_FIXTURE"
            )
        val runtime=
            NormalArenaRuntimeState
                .withCadence(cadence)

        val sender=
            UUID.fromString(
                "00000000-0000-0000-0000-000000008001"
            )
        runtime.queues
            .getValue(TeamId.RED)
            .enqueue(
                TroopQueueEntry(
                    TroopQueueEntryId(1),
                    sender,
                    TeamId.RED,
                    "zombie",1,1,
                    0L,"fixture-purchase"
                )
            )

        val spawned=
            linkedMapOf<UUID,MobEntitySpawnRequest>()
        val spawnPhase=
            TroopSpawnTickPhase(
                runtime,
                ExplicitRouteAssignmentPolicy(
                    "red-route"
                ),
                MobEntitySpawnPort { req ->
                    val uuid=
                        UUID.nameUUIDFromBytes(
                            "spawn-${req.instanceId.value}"
                                .toByteArray()
                        )
                    spawned[uuid]=req
                    uuid
                },
                MobInstanceIdAllocator(100)
            )

        val moved=
            linkedMapOf<UUID,dev.cubecrafttd.map.Vec3>()
        val movementPhase=
            MobMovementTickPhase(
                MobMovementRateResolver {
                    _,_,_ -> fallback(10.0)
                },
                MobPositionUpdatePort {
                    uuid,pos ->
                    moved[uuid]=pos
                }
            )

        val castlePhase=
            CastleAttackTickPhase(
                CastleAttackResolvedConfig(
                    fallback(0L),
                    fallback(40L)
                )
            )

        val engine=
            NormalArenaRuntimeEngine(
                listOf(
                    spawnPhase,
                    movementPhase,
                    castlePhase
                )
            )

        val hpBefore=
            context.castles
                .getValue(TeamId.RED)
                .health
        engine.tick(context)
        val mob=
            context.entityIndex
                .mobsByUuid.values.single()
        val hpAfter=
            context.castles
                .getValue(TeamId.RED)
                .health

        val rollbackContext=ArenaContext(
            ArenaId("movement-rollback"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000008100"
            ),
            TestingMapFactory.minimal()
        )
        rollbackContext.state=
            ArenaState.RUNNING
        val rollbackMob=MobRuntimeState(
            MobIdentity(
                MobInstanceId(200),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000008200"
                ),
                "zombie",sender,
                TeamId.RED,1
            ),
            MobRouteState(
                "red-route",0,0.0,0.0
            ),
            MobCombatState(
                40.0,40.0,
                MobLifecycleState.MOVING
            )
        )
        rollbackContext.entityIndex
            .registerMob(rollbackMob)
        val oldRoute=rollbackMob.route
        val movementFailure=try {
            MobMovementTickPhase(
                MobMovementRateResolver {
                    _,_,_ -> fallback(1.0)
                },
                MobPositionUpdatePort {
                    _,_ ->
                    error("synthetic live move failure")
                }
            ).tick(rollbackContext)
            false
        } catch (_: IllegalStateException) {
            true
        }

        return listOf(
            FixtureResult(
                "normal-loop-phase-order",
                engine.phaseIds()==listOf(
                    "troop-spawn",
                    "mob-movement",
                    "castle-attacks"
                )
            ),
            FixtureResult(
                "normal-loop-spawns-fifo-head",
                runtime.queues
                    .getValue(TeamId.RED)
                    .peek()==null &&
                    spawned.size==1 &&
                    mob.identity.instanceId.value==
                        100L
            ),
            FixtureResult(
                "normal-loop-moves-to-terminal",
                mob.combat.lifecycle==
                    MobLifecycleState
                        .ATTACKING_CASTLE &&
                    moved[mob.identity.entityUuid]==
                        dev.cubecrafttd.map.Vec3(
                            10.0,0.0,0.0
                        )
            ),
            FixtureResult(
                "normal-loop-castle-hit-same-arena-tick-with-zero-fallback-delay",
                hpAfter==hpBefore-1.0
            ),
            FixtureResult(
                "movement-live-update-failure-rolls-core-progress-back",
                movementFailure &&
                    rollbackMob.route==oldRoute
            )
        )
    }
}
