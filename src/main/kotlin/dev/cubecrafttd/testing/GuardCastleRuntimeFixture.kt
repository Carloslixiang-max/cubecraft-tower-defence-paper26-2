package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.castle.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.truth.*
import java.util.UUID

object GuardCastleRuntimeFixture {
    private fun <T> fallback(
        value: T
    ): ResolvedTruth<T> =
        ResolvedTruth(
            value,
            ResolutionSource
                .ENGINEERING_FALLBACK
        )

    fun run(): List<FixtureResult> {
        val context = ArenaContext(
            ArenaId("guard-castle"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000002000"
            ),
            TestingMapFactory.minimal(),
            rng=ArenaDeterministicRng(55)
        )
        context.state = ArenaState.RUNNING
        context.gameTick = 100

        val sender =
            UUID.fromString(
                "00000000-0000-0000-0000-000000002001"
            )
        val near = MobRuntimeState(
            MobIdentity(
                MobInstanceId(1),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000002011"
                ),
                "zombie",sender,TeamId.RED,1
            ),
            MobRouteState(
                "red-route",0,0.0,10.0
            ),
            MobCombatState(
                5.0,40.0,
                MobLifecycleState.MOVING
            )
        )
        val first = MobRuntimeState(
            MobIdentity(
                MobInstanceId(2),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000002012"
                ),
                "zombie",sender,TeamId.RED,1
            ),
            MobRouteState(
                "red-route",0,0.0,20.0
            ),
            MobCombatState(
                5.0,40.0,
                MobLifecycleState.MOVING
            )
        )
        context.entityIndex.registerMob(near)
        context.entityIndex.registerMob(first)

        val guard = GuardRuntime(
            GuardIdentity(
                10,TeamId.RED,
                UUID.fromString(
                    "00000000-0000-0000-0000-000000002100"
                )
            ),
            nextAttackTick=100
        )
        context.entityIndex.registerGuard(guard)

        val guardConfig =
            GuardResolvedCombatConfig(
                fallback(5.0),
                fallback(20L),
                fallback(15.0),
                fallback(
                    GuardTargetPriorityPolicy.FIRST
                )
            )
        val guardEngine = GuardCombatEngine(
            context,
            guardConfig,
            GuardGeometryProvider {
                _,uuid ->
                GuardGeometryView(
                    if (
                        uuid ==
                            near.identity.entityUuid
                    ) 5.0 else 8.0,
                    lineOfSight=true
                )
            }
        )
        val guardHit =
            guardEngine.tryAttack(guard)
        val guardNoCoin =
            guardHit.attribution
                ?.awardsPlayerKillCoins == false &&
                guardHit.attribution
                    ?.creditedPlayerUuid == null

        guard.lifecycle =
            GuardLifecycleState
                .DISABLED_ARMAGEDDON
        context.gameTick = 120
        val disabledHit =
            guardEngine.tryAttack(guard)

        // Castle attack runtime.
        val castleMob = MobRuntimeState(
            MobIdentity(
                MobInstanceId(30),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000002030"
                ),
                "zombie",sender,TeamId.RED,1
            ),
            MobRouteState(
                "red-route",0,1.0,100.0
            ),
            MobCombatState(
                40.0,40.0,
                MobLifecycleState.ATTACKING_CASTLE
            )
        )
        context.entityIndex.registerMob(
            castleMob
        )

        val castlePhase =
            CastleAttackTickPhase(
                CastleAttackResolvedConfig(
                    fallback(20L),
                    fallback(40L)
                )
            )

        val startHp =
            context.castles
                .getValue(TeamId.RED)
                .health
        context.gameTick = 200
        castlePhase.tick(context)
        val hpAt200 =
            context.castles
                .getValue(TeamId.RED)
                .health

        context.gameTick = 219
        castlePhase.tick(context)
        val hpAt219 =
            context.castles
                .getValue(TeamId.RED)
                .health

        context.gameTick = 220
        castlePhase.tick(context)
        val hpAt220 =
            context.castles
                .getValue(TeamId.RED)
                .health

        context.gameTick = 260
        castlePhase.tick(context)
        val hpAt260 =
            context.castles
                .getValue(TeamId.RED)
                .health

        castleMob.combat.lifecycle =
            MobLifecycleState.DEAD
        context.gameTick = 261
        castlePhase.tick(context)
        val metrics =
            castlePhase.metricsSnapshot()

        val unresolvedBlocked = try {
            val gate = TruthGate()
            val field =
                TruthField<Long>(
                    "guard.fireIntervalTicks",
                    null,
                    EvidenceStatus.VIDEO_REQUIRED
                )
            gate.resolveWithExplicitFallback(
                field
            )
            false
        } catch (_: IllegalStateException) {
            true
        }

        return listOf(
            FixtureResult(
                "guard-first-priority-kills-max-route-progress",
                guardHit.targetUuid ==
                    first.identity.entityUuid &&
                    first.combat.lifecycle ==
                        MobLifecycleState.DEAD
            ),
            FixtureResult(
                "guard-final-blow-zero-player-coins",
                guardNoCoin
            ),
            FixtureResult(
                "guard-disabled-armageddon-does-not-fire",
                disabledHit.targetUuid == null
            ),
            FixtureResult(
                "guard-unresolved-exact-hard-gated",
                unresolvedBlocked
            ),
            FixtureResult(
                "castle-first-hit-delay",
                hpAt200 == startHp &&
                    hpAt219 == startHp &&
                    hpAt220 == startHp - 1.0
            ),
            FixtureResult(
                "castle-repeat-interval",
                hpAt260 == startHp - 2.0
            ),
            FixtureResult(
                "castle-dead-mob-cancels-clock",
                metrics.clocksStarted == 1 &&
                    metrics.hitsApplied == 2 &&
                    metrics.clocksCancelled == 1
            )
        )
    }
}
