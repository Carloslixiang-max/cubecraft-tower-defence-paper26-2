package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.status.*
import dev.cubecrafttd.truth.*
import java.util.UUID

object MobStatusMovementFixture {
    private fun mob(
        id: Long,
        status: StatusEffectInstance?
    ): MobRuntimeState =
        MobRuntimeState(
            MobIdentity(
                MobInstanceId(id),
                UUID.nameUUIDFromBytes(
                    "status-move-$id".toByteArray()
                ),
                "zombie",
                UUID.fromString(
                    "00000000-0000-0000-0000-000000040001"
                ),
                TeamId.RED,
                1
            ),
            MobRouteState(
                "red-route",
                0,
                0.0,
                0.0
            ),
            MobCombatState(
                40.0,
                40.0,
                MobLifecycleState.MOVING
            )
        ).also {
            status?.let(
                it.statusEffects::apply
            )
        }

    fun run(): List<FixtureResult> {
        val context=
            ArenaContext(
                ArenaId("status-movement"),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000040100"
                ),
                TestingMapFactory.minimal()
            ).also {
                it.state=ArenaState.RUNNING
                it.gameTick=100L
            }

        val normal=mob(1,null)
        val slowed=mob(
            2,
            StatusEffectInstance(
                StatusEffectType.ICE_SLOW,
                "fixture",
                1.0,
                90L,
                200L
            )
        )
        val stunned=mob(
            3,
            StatusEffectInstance(
                StatusEffectType.STUN,
                "fixture",
                1.0,
                90L,
                200L
            )
        )
        val expiredStun=mob(
            4,
            StatusEffectInstance(
                StatusEffectType.STUN,
                "fixture",
                1.0,
                90L,
                100L
            )
        )

        listOf(
            normal,
            slowed,
            stunned,
            expiredStun
        ).forEach(
            context.entityIndex::registerMob
        )

        val phase=
            MobMovementTickPhase(
                movementRate=
                    MobMovementRateResolver {
                        _,_,_ ->
                        ResolvedTruth(
                            1.0,
                            ResolutionSource
                                .ENGINEERING_FALLBACK
                        )
                    },
                livePosition=
                    MobPositionUpdatePort {
                        _,_ -> Unit
                    },
                iceSlowMovementMultiplier=
                    ResolvedTruth(
                        0.5,
                        ResolutionSource
                            .ENGINEERING_FALLBACK
                    )
            )

        phase.tick(context)

        return listOf(
            FixtureResult(
                "status-movement-ice-slow-applies-explicit-multiplier",
                normal.route.routeProgress==1.0 &&
                    slowed.route.routeProgress==0.5
            ),
            FixtureResult(
                "status-movement-stun-blocks-route-progress",
                stunned.route.routeProgress==0.0
            ),
            FixtureResult(
                "status-movement-expired-stun-is-removed-before-move",
                expiredStun.route.routeProgress==1.0 &&
                    expiredStun.statusEffects
                        .get(
                            StatusEffectType.STUN
                        )==null
            )
        )
    }
}
