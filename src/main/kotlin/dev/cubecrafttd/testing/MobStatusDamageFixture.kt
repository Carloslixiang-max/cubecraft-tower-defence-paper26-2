package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.status.*
import dev.cubecrafttd.truth.*
import java.util.UUID

object MobStatusDamageFixture {
    private val owner=
        UUID.fromString(
            "00000000-0000-0000-0000-000000041001"
        )

    private fun mob(
        id: Long,
        health: Double,
        effect: StatusEffectInstance
    ): MobRuntimeState =
        MobRuntimeState(
            MobIdentity(
                MobInstanceId(id),
                UUID.nameUUIDFromBytes(
                    "status-damage-$id".toByteArray()
                ),
                "zombie",
                owner,
                TeamId.RED,
                1
            ),
            MobRouteState(
                "red-route",
                0,
                0.0,
                id.toDouble()
            ),
            MobCombatState(
                health,
                40.0,
                MobLifecycleState.MOVING
            )
        ).also {
            it.statusEffects.apply(effect)
        }

    fun run(): List<FixtureResult> {
        val context=
            ArenaContext(
                ArenaId("status-damage"),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000041100"
                ),
                TestingMapFactory.minimal()
            ).also {
                it.state=ArenaState.RUNNING
                it.gameTick=120L
            }

        val poison=
            mob(
                1,
                10.0,
                StatusEffectInstance(
                    type=StatusEffectType.POISON,
                    sourceId="tower:77",
                    magnitude=2.0,
                    appliedTick=100L,
                    expireTick=200L,
                    tickIntervalTicks=20L,
                    sourcePlayerUuid=owner,
                    sourceTowerInstanceId=77L
                )
            )
        val burn=
            mob(
                2,
                10.0,
                StatusEffectInstance(
                    type=StatusEffectType.BURN,
                    sourceId="tower:88",
                    magnitude=3.0,
                    appliedTick=100L,
                    expireTick=200L,
                    sourcePlayerUuid=owner,
                    sourceTowerInstanceId=88L
                )
            )
        val offCadence=
            mob(
                3,
                10.0,
                StatusEffectInstance(
                    type=StatusEffectType.POISON,
                    sourceId="tower:90",
                    magnitude=4.0,
                    appliedTick=100L,
                    expireTick=200L,
                    tickIntervalTicks=30L,
                    sourcePlayerUuid=owner,
                    sourceTowerInstanceId=90L
                )
            )
        val lethal=
            mob(
                4,
                2.0,
                StatusEffectInstance(
                    type=StatusEffectType.POISON,
                    sourceId="tower:99",
                    magnitude=2.0,
                    appliedTick=100L,
                    expireTick=200L,
                    tickIntervalTicks=20L,
                    sourcePlayerUuid=owner,
                    sourceTowerInstanceId=99L
                )
            )

        listOf(
            poison,burn,offCadence,lethal
        ).forEach(
            context.entityIndex::registerMob
        )

        val phase=
            MobStatusDamageTickPhase(
                MobStatusDamageResolvedConfig(
                    ResolvedTruth(
                        20L,
                        ResolutionSource
                            .ENGINEERING_FALLBACK
                    )
                )
            )

        phase.tick(context)
        val metrics=
            phase.metricsSnapshot()

        return listOf(
            FixtureResult(
                "status-damage-poison-uses-effect-interval",
                poison.combat.health==8.0
            ),
            FixtureResult(
                "status-damage-burn-uses-resolved-fallback-cadence",
                burn.combat.health==7.0
            ),
            FixtureResult(
                "status-damage-respects-off-cadence",
                offCadence.combat.health==10.0
            ),
            FixtureResult(
                "status-damage-lethal-preserves-player-tower-attribution",
                lethal.combat.lifecycle==
                    MobLifecycleState.DEAD &&
                    lethal.combat.lastEligibleDamageSource==
                        DamageSourceIdentity.PlayerTower(
                            owner,99L
                        ) &&
                    metrics.pulsesApplied==3 &&
                    metrics.lethalResolutions==1
            )
        )
    }
}
