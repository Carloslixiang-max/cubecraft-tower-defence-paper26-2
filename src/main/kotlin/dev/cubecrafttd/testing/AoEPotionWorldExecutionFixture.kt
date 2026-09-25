package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.combat.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.player.*
import dev.cubecrafttd.stats.MatchStatsRecorder
import dev.cubecrafttd.status.*
import dev.cubecrafttd.truth.*
import dev.cubecrafttd.ui.*
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

object AoEPotionWorldExecutionFixture {
    private val redPlayer=
        UUID.fromString(
            "00000000-0000-0000-0000-000000042001"
        )
    private val bluePlayer=
        UUID.fromString(
            "00000000-0000-0000-0000-000000042002"
        )

    private fun context(
        id: String
    ): ArenaContext =
        ArenaContext(
            ArenaId(id),
            UUID.fromString(
                "00000000-0000-0000-0000-000000042100"
            ),
            TestingMapFactory.minimal()
        ).also {
            it.state=ArenaState.RUNNING
            it.gameTick=100L
            it.redTeam.players +=
                redPlayer
            it.blueTeam.players +=
                bluePlayer
        }

    private fun mob(
        id: Long,
        attackedTeam: TeamId,
        sender: UUID,
        health: Double = 40.0,
        routeId: String = "red-route"
    ): MobRuntimeState =
        MobRuntimeState(
            MobIdentity(
                MobInstanceId(id),
                UUID.nameUUIDFromBytes(
                    "aoe-world-$id"
                        .toByteArray()
                ),
                "zombie",
                sender,
                attackedTeam,
                1
            ),
            MobRouteState(
                routeId,
                0,
                0.0,
                0.0
            ),
            MobCombatState(
                health,
                40.0,
                MobLifecycleState.MOVING
            )
        )

    fun run(): List<FixtureResult> {
        val config=
            EngineeringPlaytestProfile
                .create()

        val freezeLength=
            RuntimeFallbackBindings
                .aoeEffectLengthBlocks(
                    config,
                    RecommendedMatureBazaarDefinitions
                        .potion("freeze")
                )
        val meteorLength=
            RuntimeFallbackBindings
                .aoeEffectLengthBlocks(
                    config,
                    RecommendedMatureBazaarDefinitions
                        .potion("meteor")
                )
        val speedConfig=
            RuntimeFallbackBindings
                .aoePotionConfig(
                    config,
                    "speed"
                )

        val geometryRadius=
            AoEEngineeringWorldGeometry
                .radiusFromEffectLength(
                    11.0
                )
        val geometryInside=
            AoEEngineeringWorldGeometry
                .inside(
                    0.0,64.0,0.0,
                    5.0,64.0,0.0,
                    geometryRadius
                )
        val geometryOutside=
            AoEEngineeringWorldGeometry
                .inside(
                    0.0,64.0,0.0,
                    6.0,64.0,0.0,
                    geometryRadius
                )

        val queue=
            AoEPotionRuntimeQueue()
        queue.schedule(
            AoEPotionActionPlan(
                "meteor",
                redPlayer,
                listOf(
                    AoEPotionPulse(
                        0L,
                        emptyList()
                    ),
                    AoEPotionPulse(
                        5L,
                        emptyList()
                    )
                )
            ),
            100L,
            true
        )
        val due100=
            queue.due(100L)
        val due104=
            queue.due(104L)
        val due105=
            queue.due(105L)

        val damageContext=
            context("aoe-damage")
        val damageMob=
            mob(
                1,
                TeamId.RED,
                bluePlayer,
                health=5.0
            )
        damageContext.entityIndex
            .registerMob(damageMob)
        val damageQueue=
            AoEPotionRuntimeQueue()
        damageQueue.schedule(
            AoEPotionActionPlan(
                "meteor",
                redPlayer,
                listOf(
                    AoEPotionPulse(
                        0L,
                        listOf(
                            damageMob.identity
                                .entityUuid
                        ),
                        damagePerTarget=
                            8.88
                    )
                )
            ),
            100L,
            true
        )
        AoEPotionTickPhase(
            damageQueue
        ).tick(
            damageContext
        )
        val damageCredit=
            KillAttributionService
                .matureFinalBlow(
                    damageMob.combat
                        .lastEligibleDamageSource
                )

        val healContext=
            context("aoe-heal")
        val healMob=
            mob(
                2,
                TeamId.BLUE,
                redPlayer,
                health=35.0
            )
        healContext.entityIndex
            .registerMob(healMob)
        val healQueue=
            AoEPotionRuntimeQueue()
        healQueue.schedule(
            AoEPotionActionPlan(
                "heal",
                redPlayer,
                listOf(
                    AoEPotionPulse(
                        0L,
                        listOf(
                            healMob.identity
                                .entityUuid
                        ),
                        healPerTarget=20.0
                    )
                )
            ),
            100L,
            true
        )
        AoEPotionTickPhase(
            healQueue
        ).tick(
            healContext
        )

        val speedContext=
            context("aoe-speed")
        val speedMob=
            mob(
                3,
                TeamId.BLUE,
                redPlayer
            )
        speedContext.entityIndex
            .registerMob(speedMob)
        val speedPlan=
            AoEPotionRuntimePlanner
                .plan(
                    RecommendedMatureBazaarDefinitions
                        .potion("speed"),
                    speedConfig,
                    redPlayer,
                    100L,
                    listOf(
                        AoEPotionTargetCandidate(
                            speedMob.identity
                                .entityUuid,
                            AoEPotionTargetRelation
                                .FRIENDLY_TROOPS,
                            true
                        )
                    ),
                    speedContext.entityIndex
                )
        val speedQueue=
            AoEPotionRuntimeQueue()
        speedQueue.schedule(
            speedPlan,
            100L,
            true
        )
        AoEPotionTickPhase(
            speedQueue
        ).tick(
            speedContext
        )
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
        ).tick(
            speedContext
        )

        val ledger=
            EconomyLedger()
        val stats=
            MatchStatsRecorder()
        val finalizerContext=
            context("aoe-finalizer")
        val dead=
            mob(
                4,
                TeamId.RED,
                bluePlayer,
                health=0.0
            )
        dead.combat.lifecycle=
            MobLifecycleState.DEAD
        dead.combat.lastEligibleDamageSource=
            DamageSourceIdentity
                .PlayerPotion(
                    redPlayer,
                    "meteor",
                    true
                )

        val finalizer=
            MatchMobDeathFinalizer(
                PlayerSentMobDeathFinalizer(
                    SentMobExpRewardService(
                        RecommendedMatureMobDefinitions,
                        ledger,
                        PricingMode.NORMAL
                    )
                ),
                MobKillRewardService(
                    ledger,
                    RuntimeFallbackBindings
                        .mobKillReward(
                            config
                        ),
                    PricingMode.NORMAL
                ),
                stats,
                AtomicLong(9000L)
            )

        finalizer.finalize(
            finalizerContext,
            dead
        )

        val redCoinsAfterFirst=
            ledger.balance(
                EconomyAccount(
                    TeamId.RED,
                    redPlayer,
                    EconomyCurrency.MATCH_COINS
                )
            )
        val blueExpAfterFirst=
            ledger.balance(
                EconomyAccount(
                    TeamId.BLUE,
                    bluePlayer,
                    EconomyCurrency.MATCH_EXP
                )
            )
        val expectedExp=
            RecommendedMatureMobDefinitions
                .get("zombie")
                .level(1)
                .expRewardOnDeath

        finalizer.finalize(
            finalizerContext,
            dead
        )

        val finalStats=
            stats.snapshot()
                .byPlayer[redPlayer]

        return listOf(
            FixtureResult(
                "aoe-runtime-fallbacks-preserve-provenance",
                freezeLength.value==11.0 &&
                    freezeLength.source==
                        ResolutionSource
                            .ENGINEERING_FALLBACK &&
                    meteorLength.value==11.0 &&
                    meteorLength.source==
                        ResolutionSource
                            .OBSERVED_ORIGINAL &&
                    RuntimeFallbackBindings
                        .aoeKillAwardsCoins(
                            config
                        ).value
            ),
            FixtureResult(
                "aoe-engineering-geometry-uses-length-as-diameter",
                geometryRadius==5.5 &&
                    geometryInside &&
                    !geometryOutside
            ),
            FixtureResult(
                "aoe-runtime-queue-deterministic-due-boundaries",
                due100.size==1 &&
                    due104.isEmpty() &&
                    due105.size==1 &&
                    queue.pendingCount()==0
            ),
            FixtureResult(
                "aoe-pulse-lethal-uses-player-potion-attribution",
                damageMob.combat.lifecycle==
                    MobLifecycleState.DEAD &&
                    damageCredit.kind==
                        MatureKillCreditKind
                            .PLAYER_POTION &&
                    damageCredit
                        .creditedPlayerUuid==
                        redPlayer &&
                    damageCredit
                        .awardsPlayerKillCoins
            ),
            FixtureResult(
                "aoe-heal-caps-at-max-health",
                healMob.combat.health==40.0
            ),
            FixtureResult(
                "aoe-speed-status-affects-next-movement",
                speedMob.route
                    .routeProgress==1.5 &&
                    speedMob.statusEffects
                        .get(
                            StatusEffectType
                                .SPEED_BOOST
                        )!=null
            ),
            FixtureResult(
                "aoe-finalizer-settles-kill-coins-and-sent-exp",
                redCoinsAfterFirst==1L &&
                    blueExpAfterFirst==
                        expectedExp &&
                    finalStats?.let {
                        it.troopsKilled==1 &&
                            it.coinsEarned==1L
                    } == true
            ),
            FixtureResult(
                "aoe-finalizer-is-idempotent-on-repeat",
                ledger.balance(
                    EconomyAccount(
                        TeamId.RED,
                        redPlayer,
                        EconomyCurrency.MATCH_COINS
                    )
                )==redCoinsAfterFirst &&
                    ledger.balance(
                        EconomyAccount(
                            TeamId.BLUE,
                            bluePlayer,
                            EconomyCurrency.MATCH_EXP
                        )
                    )==blueExpAfterFirst &&
                    stats.snapshot()
                        .byPlayer[redPlayer]
                        ?.troopsKilled==1
            )
        )
    }
}
