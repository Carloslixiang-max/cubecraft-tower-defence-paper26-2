package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.castle.GuardBootstrapService
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.match.*
import dev.cubecrafttd.map.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.player.*
import dev.cubecrafttd.progression.*
import dev.cubecrafttd.recovery
    .JournaledPlayerRecoveryOrchestrator
import dev.cubecrafttd.stats.MatchStatsRecorder
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.tower.lifecycle.*
import dev.cubecrafttd.tower.visual
    .RotationCache
import dev.cubecrafttd.tower.visual
    .EngineeringPlaceholderTowerBodyProvider
import dev.cubecrafttd.truth.*
import dev.cubecrafttd.ui.*
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

data class BukkitAoEPotionCommitReport(
    val potionId: String,
    val radiusBlocks: Double,
    val candidateCount: Int,
    val scheduledPulseCount: Int,
    val cooldownReadyAtTick: Long
)

data class BukkitPlayerDepartureReport(
    val arenaId: String,
    val playerUuid: UUID,
    val team: TeamId,
    val newlyDeparted: Boolean,
    val activePlayersRemaining: Int,
    val releasedTowerCount: Int,
    val allPlayersDeparted: Boolean
)

data class BukkitVoluntaryLeaveReport(
    val departure:
        BukkitPlayerDepartureReport,
    val restoredNow: Boolean,
    val arenaCleaned: Boolean
)

data class BukkitArenaPerformanceSnapshot(
    val arenaId: String,
    val gameTick: Long,
    val towerCount: Int,
    val mobCount: Int,
    val guardCount: Int,
    val transientDisplayCount: Int,
    val projectileCount: Int,
    val liveTick:
        ArenaTickProfileSnapshot,
    val coreTick:
        ArenaTickProfileSnapshot
)

data class BukkitLiveArenaHandle(
    val context: ArenaContext,
    val ledger: EconomyLedger,
    val runtimeState:
        NormalArenaRuntimeState,
    val composition:
        NormalArenaRuntimeComposition,
    val stats: MatchStatsRecorder,
    val endCoordinator:
        MatchEndCoordinator,
    val mobKillRewards:
        MobKillRewardService,
    val matchClock:
        NormalMatchClockRuntime,
    val armageddonVote:
        EngineeringArmageddonVoteRuntime,
    val allowLiveArmageddonVoting:
        Boolean,
    val armageddonRuntime:
        BukkitArenaArmageddonRuntime,
    val progressionService:
        TroopProgressionService,
    val aoeCooldowns:
        DeterministicCooldownTracker,
    val liveTickProfiler:
        ArenaTickProfiler,
    val departedPlayers:
        MutableSet<UUID>,
    var session:
        MatchSessionState? = null,
    var menuRouter:
        MatchMenuActionRouter? = null,
    var towerWorldActions:
        MatchTowerWorldActionService? = null,
    var towerManagement:
        TowerManagementActionService? = null,
    var towerPlacement:
        TowerPlacementInteractionService? = null,
    val nextTransactionId:
        AtomicLong
)

class BukkitNormalArenaController(
    private val plugin: JavaPlugin,
    private val arenaService: ArenaService,
    private val recovery:
        JournaledPlayerRecoveryOrchestrator,
    private val mapBinding:
        PaperMapBindingConfig,
    private val fallback:
        RuntimeFallbackConfigV1,
    private val stage4Gate:
        PaperStage4GateStore,
    private val hotbarPreferences:
        BukkitHotbarPreferenceStore
) {
    private val matchHud=
        BukkitMatchHudService(
            plugin.server
        )

    private val matchEndFeedback=
        BukkitMatchEndFeedbackService(
            plugin.server
        )

    private val isolationRegistry=
        ArenaIsolationRegistry()

    private val farmReusePersistence=
        BukkitFarmReuseGatePersistence(
            java.io.File(
                plugin.dataFolder,
                "farm-reuse-gate.yml"
            )
        )

    private val farmReuseGate=
        FarmReuseGate(
            initial=
                farmReusePersistence.load(),
            entityPresence=
                TrackedEntityPresencePort {
                    uuid ->
                    plugin.server
                        .getEntity(uuid) != null
                },
            persist=
                farmReusePersistence::save
        )

    private val handles=
        linkedMapOf<
            ArenaId,
            BukkitLiveArenaHandle
        >()

    fun activeArenaIds():
        List<String> =
        handles.keys
            .map { it.value }
            .sorted()

    fun farmReuseStatus():
        FarmReuseGateSnapshot =
        farmReuseGate.snapshot()

    fun blockFarmReuseAfterUncleanRestart() {
        check(handles.isEmpty()) {
            "Cannot mark unclean-restart Farm residue while an arena is active"
        }
        farmReuseGate
            .markUncleanRestartSuspectedResidue()
    }

    fun beginFarmVerifiedReset() {
        check(handles.isEmpty()) {
            "Cannot begin Farm reset while an arena is active"
        }
        farmReuseGate
            .beginVerifiedWorldReset()
    }

    fun abortFarmVerifiedResetBeforeMutation() {
        check(handles.isEmpty()) {
            "Cannot abort Farm reset maintenance while an arena is active"
        }
        farmReuseGate
            .abortVerifiedWorldResetBeforeMutation()
    }

    fun clearFarmReuseAfterVerifiedWorldReset() {
        check(handles.isEmpty()) {
            "Cannot clear Farm reuse gate while an arena is active"
        }
        farmReuseGate
            .clearAfterVerifiedWorldReset()
    }

    fun trackedMob(
        entityUuid: UUID
    ): Boolean =
        handles.values.any {
            entityUuid in
                it.context.entityIndex
                    .mobsByUuid
        }

    fun runnableArmageddonTypes():
        Set<ArmageddonType> =
        ArmageddonType.entries
            .filterTo(linkedSetOf()) {
                ArmageddonFallbackValidator
                    .validate(it,fallback)
                    .isEmpty()
            }

    fun startOneVsOneTest(
        arenaIdText: String,
        redPlayer: UUID,
        bluePlayer: UUID,
        armageddonType: ArmageddonType
    ): ArenaId =
        startOneVsOneInternal(
            arenaIdText,
            redPlayer,
            bluePlayer,
            armageddonType,
            resolvedSelection=null,
            allowLiveArmageddonVoting=true,
            pricingMode=
                PricingMode.NORMAL
        )

    fun startOneVsOneResolvedTest(
        arenaIdText: String,
        redPlayer: UUID,
        bluePlayer: UUID,
        selection:
            ResolvedArmageddonSelection,
        pricingMode:
            PricingMode = PricingMode.NORMAL
    ): ArenaId =
        startOneVsOneInternal(
            arenaIdText,
            redPlayer,
            bluePlayer,
            selection.type,
            resolvedSelection=selection,
            allowLiveArmageddonVoting=false,
            pricingMode=
                pricingMode
        )

    private fun startOneVsOneInternal(
        arenaIdText: String,
        redPlayer: UUID,
        bluePlayer: UUID,
        armageddonType: ArmageddonType,
        resolvedSelection:
            ResolvedArmageddonSelection?,
        allowLiveArmageddonVoting:
            Boolean,
        pricingMode:
            PricingMode
    ): ArenaId {
        check(redPlayer!=bluePlayer)
        val arenaId=ArenaId(arenaIdText)
        check(arenaId !in handles) {
            "Arena already active: $arenaIdText"
        }

        val reuse=
            farmReuseGate.snapshot()
        check(!reuse.blocked) {
            "Farm reuse blocked by previous teardown residue: " +
                reuse.summary()
        }

        val armageddonMissing=
            ArmageddonFallbackValidator
                .validate(
                    armageddonType,
                    fallback
                )
        check(armageddonMissing.isEmpty()) {
            "Selected Armageddon $armageddonType is incomplete: " +
                armageddonMissing.joinToString {
                    it.key
                }
        }

        val voteAllowedTypes=
            runnableArmageddonTypes()
        val armageddonVote=
            EngineeringArmageddonVoteRuntime(
                eligiblePlayers=setOf(redPlayer,bluePlayer),
                allowedTypes=voteAllowedTypes,
                defaultType=armageddonType
            )

        val preflight=
            BukkitLiveCompositionPreflight(
                plugin,
                mapBinding,
                fallback
            ).run()
        val world=
            mapBinding.resolveWorld(
                plugin.server
            ) ?: error(
                "Configured world is not loaded"
            )

        val context=ArenaContext(
            arenaId,
            world.uid,
            preflight.mapRuntime
        )
        val departedPlayers=
            linkedSetOf<UUID>()
        val ledger=EconomyLedger()
        val runtimeState=
            NormalArenaRuntimeState
                .withCadence(
                    preflight.gameplay
                        .troopSpawnCadence
                )
        val aoeCooldowns=
            DeterministicCooldownTracker()
        val stats=
            MatchStatsRecorder()
        val nextTransactionId=
            AtomicLong(10_000L)
        val rewards=
            MobKillRewardService(
                ledger,
                RuntimeFallbackBindings
                    .mobKillReward(
                        fallback
                    ),
                pricingMode
            )
        val sentExpFinalizer=
            PlayerSentMobDeathFinalizer(
                SentMobExpRewardService(
                    RecommendedMatureMobDefinitions,
                    ledger,
                    pricingMode
                )
            )
        val deathFinalizer=
            MatchMobDeathFinalizer(
                sentExpFinalizer,
                rewards,
                stats,
                nextTransactionId
            )
        val entityAdapter=
            BukkitEntityRuntimeAdapter(
                plugin.server
            )
        val mobSpawnPort=
            BukkitMobEntitySpawnPort(
                world
            )
        val lineOfSight=
            BukkitLineOfSightPort(
                plugin.server
            )
        val towerGeometry=
            BukkitTowerMobGeometryProvider(
                plugin.server,
                lineOfSight
            )

        val areaTargets=
            BukkitAreaTowerTargetResolver(
                plugin.server
            )
        val chainTargets=
            BukkitPolicyChainTargetResolver(
                plugin.server,
                preflight.gameplay
                    .chainTargetPolicy.value,
                towerGeometry
            )
        val plannerProvider=
            RecommendedTowerAttackPlannerProvider(
                context.rng,
                areaTargets,
                chainTargets
            )

        val composition=
            NormalArenaRuntimeCompositionFactory
                .create(
                    NormalArenaRuntimeDependencies(
                        economyLedger=ledger,
                        runtimeState=runtimeState,
                        routeAssignment=
                            preflight.routePolicy,
                        mobSpawnPort=
                            mobSpawnPort,
                        movementRate=
                            RuntimeFallbackBindings
                                .mobMovementRate(
                                    fallback
                                ),
                        positionUpdate=
                            entityAdapter,
                        supportGeometry=
                            BukkitMobSupportGeometryProvider(
                                plugin.server
                            ),
                        towerGeometry=
                            towerGeometry,
                        towerConfigProvider=
                            BukkitTowerRuntimeConfigProvider(
                                fallback,
                                preflight.gameplay
                            ),
                        towerPlannerProvider=
                            plannerProvider,
                        towerAttackFeedback=
                            BukkitEngineeringTowerAttackFeedback(
                                plugin.server
                            ),
                        aoePotionFeedback=
                            BukkitEngineeringAoEPotionFeedback(
                                plugin.server
                            ),
                        summonCountResolver=
                            RuntimeFallbackBindings
                                .towerSummonCounts(
                                    fallback
                                ),
                        summonSpawnPort=
                            BukkitEngineeringTowerSummonPort(
                                world
                            ),
                        summonRemovalPort=
                            TowerSummonRemovalPort {
                                entityAdapter
                                    .remove(it)
                            },
                        guardGeometry=
                            BukkitGuardGeometryProvider(
                                plugin.server,
                                lineOfSight
                            ),
                        mobRemovalPort=
                            entityAdapter,
                        mobDeathFinalization=
                            deathFinalizer,
                        resolved=
                            preflight.gameplay
                    )
                )

        val bodyMutation=
            TowerBodyMutationService(
                BukkitBlockWorldAdapter(
                    world
                ),
                context.towerBodyLedger,
                RotationCache {
                    blockData,_ ->
                    blockData
                }
            )
        val armageddonRuntime=
            BukkitArenaArmageddonRuntime(
                plugin=plugin,
                fallback=fallback,
                bodyMutation=bodyMutation,
                routeAssignment=
                    preflight.routePolicy,
                mobSpawn=mobSpawnPort,
                mobPosition=
                    MobPositionUpdatePort {
                        uuid,pos ->
                        entityAdapter.move(
                            uuid,pos
                        )
                    }
            )
        val teardown=
            ArenaTeardownService(
                bodyMutation,
                entityAdapter,
                MatchPlayerRestorePort {
                    playerUuid ->
                    if(
                        playerUuid in
                            departedPlayers &&
                        playerUuid !in
                            recovery.pending()
                    ) {
                        true
                    } else {
                        recovery
                            .restoreIfPossible(
                                playerUuid
                            )
                    }
                },
                entityAdapter
            )
        val end=
            MatchEndCoordinator(
                stats,teardown
            )

        val progressionService=
            TroopProgressionService(
                ledger
            )

        val matchClock=
            NormalMatchClockRuntime(
                startGameTick=
                    context.gameTick,
                selection=
                    resolvedSelection
                        ?: armageddonVote
                            .snapshot()
                            .selection,
                tiePolicy=
                    TimeoutTiePolicy.DRAW,
                armageddonPort=
                    armageddonRuntime
            )

        val handle=
            BukkitLiveArenaHandle(
                context=context,
                ledger=ledger,
                runtimeState=runtimeState,
                composition=composition,
                stats=stats,
                endCoordinator=end,
                mobKillRewards=rewards,
                matchClock=matchClock,
                armageddonVote=
                    armageddonVote,
                allowLiveArmageddonVoting=
                    allowLiveArmageddonVoting,
                armageddonRuntime=
                    armageddonRuntime,
                progressionService=
                    progressionService,
                aoeCooldowns=aoeCooldowns,
                liveTickProfiler=
                    ArenaTickProfiler(),
                departedPlayers=
                    departedPlayers,
                nextTransactionId=
                    nextTransactionId
            )
        val reservation=
            ArenaSpatialReservation
                .fromMap(
                    arenaId,
                    world.uid,
                    setOf(
                        redPlayer,
                        bluePlayer
                    ),
                    preflight.mapRuntime
                )
        val reservationResult=
            isolationRegistry
                .reserve(
                    reservation
                )
        check(
            reservationResult is
                ArenaReservationResult
                    .Accepted
        ) {
            val rejected=
                reservationResult as
                    ArenaReservationResult
                        .Rejected
            "Arena isolation blocked: " +
                rejected.summary()
        }

        try {
            arenaService.register(
                context
            )
            handles[arenaId]=handle

            GuardBootstrapService()
                .bootstrap(
                    context,
                    BukkitGuardAnchorSpawnPort(
                        world
                    )
                )

            val preset=
                MatchRulePreset(
                    mode=
                        when(pricingMode) {
                            PricingMode.NORMAL ->
                                MatchMode.NORMAL
                            PricingMode.DOUBLE_INCOME ->
                                MatchMode.DOUBLE_INCOME
                            PricingMode.QUICK_START ->
                                MatchMode.QUICK_START
                        },
                    pricingMode=
                        pricingMode,
                    goldmineIncomeMode=
                        if(
                            pricingMode==
                                PricingMode.DOUBLE_INCOME
                        )
                            GoldmineIncomeMode
                                .DOUBLE_INCOME
                        else
                            GoldmineIncomeMode
                                .NORMAL,
                    progressionMode=
                        ProgressionMode
                            .CLASSIC_PROGRESSION,
                    startingBalance=
                        if(
                            pricingMode==
                                PricingMode.QUICK_START
                        )
                            MatchStartingBalance(
                                RecommendedMaturePricing
                                    .QUICK_START_COINS,
                                RecommendedMaturePricing
                                    .QUICK_START_EXP
                            )
                        else
                            preflight.gameplay
                                .startingBalance,
                    mobKillCoinMultiplier=
                        if(
                            pricingMode==
                                PricingMode.DOUBLE_INCOME
                        ) 2L else 1L,
                    sentMobExpMultiplier=
                        if(
                            pricingMode==
                                PricingMode.DOUBLE_INCOME
                        ) 2L else 1L
                )

            val bootstrap=
                MatchBootstrapService(
                    recovery,
                    ledger,
                    progressionService
                )

            val bootstrapReport=
                bootstrap.prepare(
                context,
                runtimeState,
                MatchBootstrapRequest(
                    redPlayers=
                        setOf(redPlayer),
                    bluePlayers=
                        setOf(bluePlayer),
                    preset=preset,
                    firstGoldmineIncomeTick=
                        context.gameTick +
                            preflight.gameplay
                                .goldmineFirstIncomeDelayTicks
                                .value,
                    economyTransactionBaseId=
                        1_000L
                )
            )

            bootstrapReport.session
                .players
                .values
                .forEach { state ->
                    hotbarPreferences
                        .load(
                            state.playerUuid
                        )
                        ?.let { saved ->
                            state.interaction
                                .hotbarLayout=
                                saved
                        }
                }

            handle.session=
                bootstrapReport.session
            handle.menuRouter=
                MatchMenuActionRouter(
                    context=context,
                    runtime=runtimeState,
                    session=
                        bootstrapReport
                            .session,
                    ledger=ledger,
                    progression=
                        progressionService,
                    initialSequence=
                        100_000L
                )

            val towerLifecycle=
                TowerLifecycleService(
                    context=context,
                    economy=ledger,
                    bodyMutation=
                        bodyMutation,
                    interactionPolicy=
                        DepartedOwnerTeamTowerInteractionPolicy(
                            context,
                            departedPlayers
                        )
                )
            val towerWorldActions=
                MatchTowerWorldActionService(
                    context=context,
                    session=
                        bootstrapReport.session,
                    lifecycle=
                        towerLifecycle,
                    bodies=
                        EngineeringPlaceholderTowerBodyProvider,
                    rotation=
                        EngineeringFixedR0RotationPolicy,
                    initialSequence=
                        5_000_000L
                )
            handle.towerWorldActions=
                towerWorldActions
            handle.towerManagement=
                TowerManagementActionService(
                    context,
                    towerWorldActions,
                    bootstrapReport.session
                )
            handle.towerPlacement=
                TowerPlacementInteractionService(
                    bootstrapReport.session,
                    towerWorldActions
                )

            val placement=
                BukkitMatchPlayerPlacementService(
                    plugin.server,
                    world
                ).placeTeams(
                    context.mapRuntime,
                    setOf(redPlayer),
                    setOf(bluePlayer)
                )
            check(placement.success) {
                "Player placement failed: " +
                    placement.failures
                        .joinToString {
                            "${it.playerUuid}:${it.reason}"
                        }
            }

            val loadout=
                BukkitMatchLoadoutService(
                    plugin.server
                )
            bootstrapReport.session
                .players
                .values
                .forEach { state ->
                    loadout.apply(
                        state.playerUuid,
                        state
                    )
                }

            bootstrap.start(context)

            val task=
                BukkitSchedulerAdapter(
                    plugin
                ).repeat(
                    delayTicks=1L,
                    periodTicks=1L
                ) {
                    runTick(arenaId)
                }
            context.taskGroup.register(task)

            return arenaId
        } catch(t:Throwable) {
            val cleanupReport=
                runCatching {
                    end.finish(
                        context,
                        MatchOutcome.Tie(
                            TimeoutTiePolicy
                                .ENGINEERING_CUSTOM,
                            "stage4_start_failure"
                        )
                    )
                }.getOrElse {
                    cleanupFailure ->
                    t.addSuppressed(
                        cleanupFailure
                    )
                    null
                }

            if(cleanupReport!=null) {
                runCatching {
                    farmReuseGate
                        .record(
                            cleanupReport
                                .teardown
                        )
                }.onFailure {
                    gateFailure ->
                    t.addSuppressed(
                        gateFailure
                    )
                    runCatching {
                        farmReuseGate
                            .markWorldIntegrityUnknown()
                    }.onFailure {
                        t.addSuppressed(it)
                    }
                }

                if(
                    !cleanupReport
                        .teardown
                        .fullyCleanNow
                ) {
                    plugin.logger.warning(
                        "Arena " +
                            arenaId.value +
                            " failed during start and teardown reported residue/pending recovery: " +
                            "towerConflicts=" +
                            cleanupReport
                                .teardown
                                .towerBodyConflicts
                                .size +
                            " failedEntityRemovals=" +
                            cleanupReport
                                .teardown
                                .failedEntityRemovals
                                .size +
                            " survivingTrackedEntities=" +
                            cleanupReport
                                .teardown
                                .survivingTrackedEntities
                                .size +
                            " pendingPlayerRestores=" +
                            cleanupReport
                                .teardown
                                .pendingPlayerRestores
                                .size
                    )
                }
            } else {
                runCatching {
                    farmReuseGate
                        .markWorldIntegrityUnknown()
                }.onFailure {
                    t.addSuppressed(it)
                }
                plugin.logger.severe(
                    "Arena " +
                        arenaId.value +
                        " failed during start and teardown itself failed. Farm world integrity is now UNKNOWN and reuse remains hard-blocked until verified reset."
                )
            }

            runCatching {
                arenaService.close(
                    arenaId
                ) {}
            }.onFailure {
                t.addSuppressed(it)
            }
            handles.remove(arenaId)
            runCatching {
                isolationRegistry.release(
                    arenaId
                )
            }.onFailure {
                t.addSuppressed(it)
            }
            throw t
        }
    }

    private fun runTick(
        arenaId: ArenaId
    ) {
        val handle=handles[arenaId]
            ?: return
        val context=handle.context
        if(
            context.state !=
                ArenaState.RUNNING
        ) return

        val liveTickStart=
            System.nanoTime()
        try {
            handle.composition.engine
                .tick(context)

            val clockReport=
                handle.matchClock.tick(
                    context,
                    context.entityIndex
                        .guardsByUuid
                        .values
                )

            if(
                clockReport.outcome !is
                    MatchOutcome.Continue
            ) {
                stop(
                    arenaId.value,
                    clockReport.outcome
                )
                return
            }

            if(
                handle.matchClock
                    .isArmageddonStarted()
            ) {
                handle.armageddonRuntime
                    .tick(context)

                val postArmageddonOutcome=
                    MatchOutcomeResolver
                        .byCastleDestruction(
                            context.castles
                                .getValue(
                                    TeamId.RED
                                ),
                            context.castles
                                .getValue(
                                    TeamId.BLUE
                                )
                        )
                if(
                    postArmageddonOutcome !is
                        MatchOutcome.Continue
                ) {
                    stop(
                        arenaId.value,
                        postArmageddonOutcome
                    )
                    return
                }
            }
            if(
                context.gameTick % 10L == 0L
            ) {
                val session=handle.session
                if(session!=null) {
                    matchHud.push(
                        context,
                        handle.ledger,
                        session,
                        handle.matchClock
                    )
                }
            }
        } catch(t:Throwable) {
            plugin.logger.severe(
                "Arena ${arenaId.value} tick failed: " +
                    t.stackTraceToString()
            )
            stop(
                arenaId.value,
                MatchOutcome.Tie(
                    TimeoutTiePolicy
                        .ENGINEERING_CUSTOM,
                    "stage4_runtime_failure"
                )
            )
        } finally {
            handle.liveTickProfiler
                .recordTick(
                    System.nanoTime() -
                        liveTickStart
                )
        }
    }




    fun canBeginTowerPlacement(
        playerUuid: UUID,
        clickedBlock: dev.cubecrafttd.map.BlockPos
    ): Boolean {
        val handle=handleForPlayer(playerUuid)
            ?: return false
        val team=when {
            playerUuid in handle.context.redTeam.players -> TeamId.RED
            playerUuid in handle.context.blueTeam.players -> TeamId.BLUE
            else -> return false
        }
        val map=handle.context.mapRuntime
        val region=map.placementRegions[team]
        if(clickedBlock in (region?.legalBaseBlocks ?: emptySet()))
            return true
        return map.explicitTowerSpots
            .asSequence()
            .filter { it.team==team }
            .any { clickedBlock in ExplicitSpotPlacementResolver.footprintCells(it) }
    }

    fun beginTowerPlacement(
        playerUuid: UUID,
        clickedBlock: dev.cubecrafttd.map.BlockPos
    ): TowerPlacementInteractionResult {
        val handle=handleForPlayer(playerUuid)
            ?: error("Player is not in an active live arena")
        check(canBeginTowerPlacement(playerUuid,clickedBlock)) {
            "Clicked block is not a legal tower placement surface"
        }
        return handle.towerPlacement
            ?.beginRegular(playerUuid,clickedBlock)
            ?: error("Tower placement service is not initialized")
    }

    fun quickPlaceTower(
        playerUuid: UUID,
        clickedBlock: dev.cubecrafttd.map.BlockPos
    ): TowerPlacementInteractionResult {
        val handle=handleForPlayer(playerUuid)
            ?: error("Player is not in an active live arena")
        check(canBeginTowerPlacement(playerUuid,clickedBlock)) {
            "Clicked block is not a legal tower placement surface"
        }
        return handle.towerPlacement
            ?.quickPlace(playerUuid,clickedBlock)
            ?: error("Tower placement service is not initialized")
    }

    fun dynamicMenuForPlayer(
        playerUuid: UUID,
        kind: String
    ): MenuDefinition {
        val handle=
            handleForPlayer(playerUuid)
                ?: error(
                    "Player is not in an active live arena"
                )
        val session=
            handle.session
                ?: error(
                    "Arena session is not initialized"
                )
        val player=
            session.players[playerUuid]
                ?: error(
                    "Player match session is missing"
                )

        return when(kind.lowercase()) {
            "summoner" ->
                DynamicMatchMenus
                    .summoner(player)
            "progression" ->
                DynamicMatchMenus
                    .progression(player)
            "bazaar" ->
                DynamicMatchMenus
                    .bazaar(player)
            "settings" ->
                DynamicMatchMenus
                    .settings(player)
            "armageddon" ->
                DynamicMatchMenus
                    .armageddonVote(
                        playerUuid,
                        handle.armageddonVote.snapshot(),
                        !handle.allowLiveArmageddonVoting ||
                            handle.matchClock.isArmageddonStarted()
                    )
            "hotbar" ->
                DynamicMatchMenus
                    .hotbarEditor(player)
            else ->
                error(
                    "Unknown dynamic menu $kind"
                )
        }
    }


    fun hotbarActionAt(
        playerUuid: UUID,
        zeroBasedSlot: Int
    ): HotbarAction? {
        val handle=
            handleForPlayer(playerUuid)
                ?: return null
        val session=
            handle.session
                ?: return null
        val player=
            session.players[playerUuid]
                ?: return null

        return player.interaction
            .hotbarLayout
            .slots
            .entries
            .firstOrNull {
                it.value==zeroBasedSlot
            }?.key
    }

    fun visibleRangefinderTowers(
        playerUuid: UUID,
        hoveredTower: TowerInstanceId?,
        sneaking: Boolean,
        playerX: Double,
        playerY: Double,
        playerZ: Double
    ): Set<TowerInstanceId> {
        val handle=handleForPlayer(playerUuid)
            ?: return emptySet()
        val session=handle.session ?: return emptySet()
        val player=session.players[playerUuid] ?: return emptySet()
        val distances=handle.context.entityIndex
            .towersByInstanceId
            .values
            .associate { tower ->
                val p=tower.geometry.rangeOrigin
                val dx=p.x-playerX
                val dy=p.y-playerY
                val dz=p.z-playerZ
                tower.identity.instanceId to
                    (dx*dx+dy*dy+dz*dz)
            }
        return TowerRangefinderRuntime.visibleTowerIds(
            TowerRangefinderQuery(
                hoveredTower=hoveredTower,
                sneaking=sneaking,
                distanceSquaredByTower=distances,
                pinnedTowerIds=player.interaction
                    .pinnedRangefinderTowers
                    .mapTo(linkedSetOf()) { TowerInstanceId(it) }
            )
        )
    }

    fun rangefinderViewsForPlayer(
        playerUuid: UUID,
        hoveredTower: TowerInstanceId?,
        sneaking: Boolean,
        playerX: Double,
        playerY: Double,
        playerZ: Double
    ): List<BukkitTowerRangefinderView> {
        val handle=
            handleForPlayer(playerUuid)
                ?: return emptyList()
        val visible=
            visibleRangefinderTowers(
                playerUuid,
                hoveredTower,
                sneaking,
                playerX,
                playerY,
                playerZ
            )

        return visible.mapNotNull { id ->
            val tower=
                handle.context.entityIndex
                    .towersByInstanceId[id]
                    ?: return@mapNotNull null
            val path=
                tower.upgrade.path
                    ?: return@mapNotNull null
            val stage=
                TowerStageResolver.resolve(
                    RecommendedMatureTowerDefinitions
                        .get(
                            tower.identity.towerId
                        ),
                    path,
                    tower.upgrade.level
                )
            val radius=
                stage.stats.rangeBlocks
                    ?: return@mapNotNull null

            BukkitTowerRangefinderView(
                towerInstanceId=id,
                centre=
                    tower.geometry.rangeOrigin,
                radiusBlocks=radius
            )
        }
    }

    fun towerAt(
        playerUuid: UUID,
        clickedBlock:
            dev.cubecrafttd.map.BlockPos
    ): TowerInstanceId? {
        val handle=
            handleForPlayer(playerUuid)
                ?: return null
        return TowerBodyInteractionLookup(
            handle.context
        ).towerAt(
            clickedBlock
        )?.identity?.instanceId
    }

    fun towerManagementMenu(
        playerUuid: UUID,
        towerId: TowerInstanceId
    ): MenuDefinition {
        val handle=
            handleForPlayer(playerUuid)
                ?: error(
                    "Player is not in an active live arena"
                )
        check(
            towerId in
                handle.context.entityIndex
                    .towersByInstanceId
        ) {
            "Unknown tower ${towerId.value}"
        }
        return TowerManagementMenus
            .engineering(
                towerId.value
            )
    }

    fun quickUpgradeTower(
        playerUuid: UUID,
        towerId: TowerInstanceId
    ): TowerUpgradeReceipt {
        val handle=
            handleForPlayer(playerUuid)
                ?: error(
                    "Player is not in an active live arena"
                )
        return handle.towerWorldActions
            ?.quickUpgrade(
                playerUuid,towerId
            )
            ?: error(
                "Tower world action service is not initialized"
            )
    }

    fun hasArmedAoEPotion(
        playerUuid: UUID
    ): Boolean =
        handleForPlayer(playerUuid)
            ?.session
            ?.players
            ?.get(playerUuid)
            ?.interaction
            ?.armedAoEPotion != null

    fun commitArmedAoEPotion(
        playerUuid: UUID,
        targetX: Double,
        targetY: Double,
        targetZ: Double
    ): BukkitAoEPotionCommitReport {
        val handle=
            handleForPlayer(playerUuid)
                ?: error(
                    "Player is not in an active live arena"
                )
        val session=
            handle.session
                ?: error(
                    "Arena session is not initialized"
                )
        val playerState=
            session.players[playerUuid]
                ?: error(
                    "Player match session is missing"
                )
        val token=
            playerState.interaction
                .armedAoEPotion
                ?: error(
                    "No AoE potion is armed"
                )
        check(
            token.buyerUuid==
                playerUuid
        ) {
            "Armed potion owner mismatch"
        }

        val team=
            when {
                playerUuid in
                    handle.context
                        .redTeam.players ->
                    TeamId.RED
                playerUuid in
                    handle.context
                        .blueTeam.players ->
                    TeamId.BLUE
                else ->
                    error(
                        "Player is not on an arena team"
                    )
            }

        val length=
            RuntimeFallbackBindings
                .aoeEffectLengthBlocks(
                    fallback,
                    token.definition
                )
        val radius=
            AoEEngineeringWorldGeometry
                .radiusFromEffectLength(
                    length.value
                )
        check(radius>0.0)

        val candidates=
            handle.context.entityIndex
                .mobsByUuid
                .values
                .mapNotNull { mob ->
                    val live=
                        plugin.server
                            .getEntity(
                                mob.identity
                                    .entityUuid
                            )
                            ?: return@mapNotNull null
                    if(
                        live.world.uid !=
                            handle.context
                                .worldId
                    ) return@mapNotNull null

                    val p=live.location
                    val inside=
                        AoEEngineeringWorldGeometry
                            .inside(
                                targetX,
                                targetY,
                                targetZ,
                                p.x,
                                p.y,
                                p.z,
                                radius
                            )

                    AoEPotionTargetCandidate(
                        mobUuid=
                            mob.identity
                                .entityUuid,
                        relation=
                            if(
                                mob.identity
                                    .attackedTeam==
                                    team
                            ) {
                                AoEPotionTargetRelation
                                    .ENEMY_TROOPS
                            } else {
                                AoEPotionTargetRelation
                                    .FRIENDLY_TROOPS
                            },
                        insideEffectArea=
                            inside
                    )
                }

        val service=
            AoEPotionUseService(
                AoEPotionCooldownGate(
                    handle.aoeCooldowns
                )
            )
        val receipt=
            service.commit(
                definition=
                    token.definition,
                config=
                    RuntimeFallbackBindings
                        .aoePotionConfig(
                            fallback,
                            token.potionId
                        ),
                ownerUuid=
                    playerUuid,
                gameTick=
                    handle.context
                        .gameTick,
                candidates=
                    candidates,
                index=
                    handle.context
                        .entityIndex
            )

        handle.runtimeState
            .aoePulses
            .schedule(
                receipt.actionPlan,
                handle.context.gameTick,
                RuntimeFallbackBindings
                    .aoeKillAwardsCoins(
                        fallback
                    ).value
            )
        playerState.interaction
            .armedAoEPotion=null

        return BukkitAoEPotionCommitReport(
            potionId=
                token.potionId,
            radiusBlocks=
                radius,
            candidateCount=
                candidates.count {
                    it.insideEffectArea
                },
            scheduledPulseCount=
                receipt.actionPlan
                    .pulses.size,
            cooldownReadyAtTick=
                receipt.cooldownReadyAtTick
        )
    }

    fun performanceSnapshots(
        arenaIdText: String? = null
    ): List<BukkitArenaPerformanceSnapshot> {
        val selected=
            if(arenaIdText==null) {
                handles.values
                    .toList()
            } else {
                handles[
                    ArenaId(
                        arenaIdText
                    )
                ]?.let(::listOf)
                    ?: emptyList()
            }

        return selected
            .sortedBy {
                it.context
                    .arenaId.value
            }
            .map { handle ->
                val index=
                    handle.context
                        .entityIndex
                BukkitArenaPerformanceSnapshot(
                    arenaId=
                        handle.context
                            .arenaId.value,
                    gameTick=
                        handle.context
                            .gameTick,
                    towerCount=
                        index.towersByInstanceId
                            .size,
                    mobCount=
                        index.mobsByUuid
                            .size,
                    guardCount=
                        index.guardsByUuid
                            .size,
                    transientDisplayCount=
                        index.transientDisplays
                            .size,
                    projectileCount=
                        index.projectiles
                            .size,
                    liveTick=
                        handle.liveTickProfiler
                            .snapshot(),
                    coreTick=
                        handle.composition
                            .engine
                            .profileSnapshot()
                )
            }
    }

    fun resetPerformance(
        arenaIdText: String? = null
    ): Int {
        val selected=
            if(arenaIdText==null) {
                handles.values
                    .toList()
            } else {
                handles[
                    ArenaId(
                        arenaIdText
                    )
                ]?.let(::listOf)
                    ?: emptyList()
            }

        selected.forEach { handle ->
            handle.liveTickProfiler
                .reset()
            handle.composition
                .engine
                .resetProfile()
        }
        return selected.size
    }

    fun isActivePlayer(
        playerUuid: UUID
    ): Boolean =
        handleForPlayer(playerUuid) != null

    fun markPlayerDeparted(
        playerUuid: UUID
    ): BukkitPlayerDepartureReport? {
        val handle=
            rawHandleForPlayer(
                playerUuid
            ) ?: return null
        val session=
            handle.session
                ?: return null

        val departure=
            MatchParticipantDepartureService(
                handle.context,
                session,
                handle.departedPlayers
            ).depart(playerUuid)
                ?: return null

        if(departure.newlyDeparted) {
            isolationRegistry.releasePlayer(
                handle.context.arenaId,
                playerUuid
            )

            if(
                handle.allowLiveArmageddonVoting &&
                !handle.matchClock
                    .isArmageddonStarted()
            ) {
                val vote=
                    handle.armageddonVote
                        .withdraw(playerUuid)
                handle.matchClock
                    .replaceSelectionBeforeArmageddon(
                        vote.selection
                    )
            }
        }

        val releasedTowerCount=
            handle.context.entityIndex
                .towersByInstanceId
                .values
                .count {
                    it.identity.ownerUuid==
                        playerUuid
                }

        return BukkitPlayerDepartureReport(
            arenaId=
                handle.context.arenaId
                    .value,
            playerUuid=playerUuid,
            team=departure.team,
            newlyDeparted=
                departure.newlyDeparted,
            activePlayersRemaining=
                departure.activePlayersRemaining,
            releasedTowerCount=
                releasedTowerCount,
            allPlayersDeparted=
                departure.activePlayersRemaining==
                    0
        )
    }

    fun leaveActivePlayer(
        playerUuid: UUID
    ): BukkitVoluntaryLeaveReport? {
        if(!isActivePlayer(playerUuid)) {
            return null
        }

        val departure=
            markPlayerDeparted(
                playerUuid
            ) ?: return null

        matchEndFeedback.beforeRestore(
            setOf(playerUuid)
        )
        val restored=
            recovery.restoreIfPossible(
                playerUuid
            )

        val cleaned=
            if(
                departure
                    .allPlayersDeparted
            ) {
                stopIfNoActivePlayers(
                    departure.arenaId
                ) != null
            } else {
                false
            }

        return BukkitVoluntaryLeaveReport(
            departure=departure,
            restoredNow=restored,
            arenaCleaned=cleaned
        )
    }

    fun stopIfNoActivePlayers(
        arenaIdText: String
    ): MatchEndReport? {
        val arenaId=
            ArenaId(arenaIdText)
        val handle=
            handles[arenaId]
                ?: return null
        val session=
            handle.session
                ?: return null
        if(session.players.isNotEmpty()) {
            return null
        }

        return stop(
            arenaIdText,
            MatchOutcome.Tie(
                TimeoutTiePolicy
                    .ENGINEERING_CUSTOM,
                "all_players_departed_engineering_cleanup"
            )
        )
    }

    private fun rawHandleForPlayer(
        playerUuid: UUID
    ): BukkitLiveArenaHandle? =
        handles.values.firstOrNull {
            playerUuid in
                it.context.redTeam.players ||
            playerUuid in
                it.context.blueTeam.players
        }

    private fun handleForPlayer(
        playerUuid: UUID
    ): BukkitLiveArenaHandle? =
        rawHandleForPlayer(
            playerUuid
        )?.takeIf {
            playerUuid !in
                it.departedPlayers &&
            (
                it.session==null ||
                playerUuid in
                    it.session!!.players
            )
        }

    fun handleMenuAction(
        invocation:
            dev.cubecrafttd.ui
                .MenuActionInvocation
    ): Any {
        val handle=
            handleForPlayer(
                invocation.playerUuid
            ) ?: error(
                "Player is not in an active live arena"
            )

        val playerState=handle.session
            ?.players
            ?.get(invocation.playerUuid)
        val placementPending=
            playerState?.interaction
                ?.towerPlacement
                ?.pending != null

        if(
            invocation.actionId.startsWith(
                "armageddon:vote:"
            )
        ) {
            check(handle.allowLiveArmageddonVoting) {
                "Armageddon was resolved during pregame and is locked for this match"
            }
            check(!handle.matchClock.isArmageddonStarted()) {
                "Armageddon vote is locked after activation"
            }
            val type=ArmageddonType.valueOf(
                invocation.actionId.substringAfterLast(':').uppercase()
            )
            val receipt=handle.armageddonVote.cast(
                invocation.playerUuid,type
            )
            handle.matchClock.replaceSelectionBeforeArmageddon(
                receipt.snapshot.selection
            )
            return receipt
        }

        if(
            invocation.actionId
                .startsWith(
                    "bazaar:potion:use:"
                )
        ) {
            val state=
                playerState
                    ?: error(
                        "Player match session is missing"
                    )
            check(
                state.interaction
                    .armedAoEPotion==null
            ) {
                "Throw the currently armed AoE potion first"
            }
            val cooldown=
                AoEPotionCooldownGate(
                    handle.aoeCooldowns
                )
            check(
                cooldown.isReady(
                    invocation.playerUuid,
                    handle.context.gameTick
                )
            ) {
                "AoE potion cooldown active for " +
                    cooldown.remainingTicks(
                        invocation.playerUuid,
                        handle.context.gameTick
                    ) +
                    " more ticks"
            }
        }

        if(placementPending && invocation.actionId.startsWith("tower:")) {
            val towerId=invocation.actionId.substringAfter("tower:")
            return handle.towerPlacement
                ?.chooseTower(invocation.playerUuid,towerId)
                ?: error("Tower placement service is not initialized")
        }
        if(placementPending && invocation.actionId.startsWith("path:")) {
            val path=when(invocation.actionId.substringAfter("path:")) {
                "top" -> dev.cubecrafttd.tower.visual.TowerPath.TOP
                "bottom" -> dev.cubecrafttd.tower.visual.TowerPath.BOTTOM
                else -> error("Unknown tower path action")
            }
            return handle.towerPlacement
                ?.choosePathAndPlace(invocation.playerUuid,path)
                ?: error("Tower placement service is not initialized")
        }

        if(
            invocation.actionId
                .startsWith(
                    "tower-manage:"
                )
        ) {
            val service=
                handle.towerManagement
                    ?: error(
                        "Tower management service is not initialized"
                    )

            val actionParts=
                invocation.actionId
                    .split(':')
            check(actionParts.size==3) {
                "Invalid tower management action id"
            }
            val towerInstanceId=
                TowerInstanceId(
                    actionParts[1]
                        .toLong()
                )
            val towerBefore=
                handle.context.entityIndex
                    .towersByInstanceId[
                        towerInstanceId
                    ] ?: error(
                    "Unknown tower " +
                        towerInstanceId.value
                )
            val departedOwnerTakeover=
                towerBefore.identity
                    .ownerUuid !=
                    invocation.playerUuid &&
                towerBefore.identity
                    .ownerUuid in
                    handle.departedPlayers

            val result=
                service.handle(
                    invocation.playerUuid,
                    invocation.actionId
                )

            if(
                departedOwnerTakeover &&
                (
                    result is
                        TowerManagementActionResult
                            .Upgraded ||
                    result is
                        TowerManagementActionResult
                            .Sold
                )
            ) {
                runCatching {
                    stage4Gate
                        .recordDepartedOwnerTeammateTakeover()
                }.onFailure {
                    plugin.logger.warning(
                        "Could not persist departed-owner teammate tower takeover evidence: " +
                            it.javaClass.simpleName +
                            ": " +
                            it.message
                    )
                }
            }

            return result
        }

        val router=
            handle.menuRouter
                ?: error(
                    "Arena menu router is not initialized"
                )

        val result=
            router.handle(
                invocation
            )

        if(
            result is
                MatchMenuActionResult
                    .PotionUsePurchased
        ) {
            val state=
                handle.session
                    ?.players
                    ?.get(
                        invocation.playerUuid
                    ) ?: error(
                    "Player match session is missing"
                )
            state.interaction
                .armedAoEPotion=result.token
        }

        if(
            result is
                MatchMenuActionResult
                    .HotbarLayoutChanged
        ) {
            val state=
                handle.session
                    ?.players
                    ?.get(
                        invocation.playerUuid
                    ) ?: error(
                    "Player match session is missing"
                )
            hotbarPreferences.save(
                invocation.playerUuid,
                result.layout
            )
            BukkitMatchLoadoutService(
                plugin.server
            ).apply(
                invocation.playerUuid,
                state
            )
        }

        if(
            result is
                MatchMenuActionResult
                    .WeaponTierUpgraded
        ) {
            val state=
                handle.session
                    ?.players
                    ?.get(
                        invocation
                            .playerUuid
                    ) ?: error(
                    "Player match session is missing"
                )
            BukkitMatchLoadoutService(
                plugin.server
            ).apply(
                invocation.playerUuid,
                state
            )
        }

        return result
    }

    fun handlePlayerHit(
        hit: dev.cubecrafttd.paper
            .LivePlayerWeaponHit
    ) {
        val handle=
            handles.values.firstOrNull {
                hit.targetEntityUuid in
                    it.context.entityIndex
                        .mobsByUuid
            } ?: return

        val service=
            PlayerWeaponCombatService(
                handle.context.entityIndex,
                handle.composition
                    .combatBindings
                    .lethalResolver
            )
        val result=
            service.hit(
                PlayerWeaponHitCommand(
                    playerUuid=
                        hit.playerUuid,
                    targetMobUuid=
                        hit.targetEntityUuid,
                    weapon=
                        when(
                            hit.weaponKind.uppercase()
                        ) {
                            "SWORD" ->
                                PlayerWeaponAttackKind
                                    .SWORD
                            "BOW" ->
                                PlayerWeaponAttackKind
                                    .BOW
                            else -> return
                        },
                    actualDamage=
                        hit.finalDamage
                )
            )

        if(
            result.killed &&
            result.attribution!=null
        ) {
            val mob=
                handle.context.entityIndex
                    .mobsByUuid[
                        hit.targetEntityUuid
                    ] ?: return
            if(
                mob.identity.mobId=="wither"
            ) {
                // Wither is an Armageddon boss, not one of the ten player-sent
                // troop families. No ordinary mob.killCoins lookup is allowed.
                plugin.logger.info(
                    "Player ${hit.playerUuid} defeated Wither boss " +
                        "${mob.identity.entityUuid}; standard troop kill reward intentionally not applied."
                )
            } else {
                val awarded=
                    handle.mobKillRewards
                        .award(
                            mob,
                            result.attribution,
                            handle.context.gameTick,
                            handle.nextTransactionId
                                .getAndIncrement()
                        )
                handle.stats
                    .recordTroopKill(
                        hit.playerUuid
                    )
                if(awarded>0L) {
                    handle.stats
                        .recordCoinsEarned(
                            hit.playerUuid,
                            awarded
                        )
                }
            }
        }
    }

    fun stop(
        arenaIdText: String,
        outcome: MatchOutcome =
            MatchOutcome.Tie(
                TimeoutTiePolicy
                    .ENGINEERING_CUSTOM,
                "stage4_manual_stop"
            )
    ): MatchEndReport? {
        val arenaId=ArenaId(
            arenaIdText
        )
        val handle=
            handles.remove(arenaId)
                ?: return null

        val activeTeamByPlayer=
            handle.session
                ?.players
                ?.keys
                ?.associateWith { uuid ->
                    when {
                        uuid in
                            handle.context
                                .redTeam.players ->
                            TeamId.RED
                        uuid in
                            handle.context
                                .blueTeam.players ->
                            TeamId.BLUE
                        else ->
                            error(
                                "Active session player is not on an arena team"
                            )
                    }
                } ?: emptyMap()

        matchEndFeedback
            .beforeRestore(
                activeTeamByPlayer
                    .keys
            )

        return try {
            val report=
                handle.endCoordinator
                    .finish(
                        handle.context,
                        outcome
                    )

            farmReuseGate
                .record(
                    report.teardown
                )

            stage4Gate
                .recordArenaRoundTrip(
                    report.teardown
                )
            matchEndFeedback.present(
                report.outcome,
                activeTeamByPlayer
            )
            report
        } finally {
            arenaService.close(
                arenaId
            ) {}
            isolationRegistry.release(
                arenaId
            )
        }
    }

    fun stopAll() {
        handles.keys.toList()
            .forEach {
                stop(it.value)
            }
    }
}
