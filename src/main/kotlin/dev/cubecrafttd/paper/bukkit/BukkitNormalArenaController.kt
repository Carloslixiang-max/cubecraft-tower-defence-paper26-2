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
    val armageddonRuntime:
        BukkitArenaArmageddonRuntime,
    val progressionService:
        TroopProgressionService,
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
        PaperStage4GateStore
) {
    private val matchHud=
        BukkitMatchHudService(
            plugin.server
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

    fun trackedMob(
        entityUuid: UUID
    ): Boolean =
        handles.values.any {
            entityUuid in
                it.context.entityIndex
                    .mobsByUuid
        }

    fun startOneVsOneTest(
        arenaIdText: String,
        redPlayer: UUID,
        bluePlayer: UUID,
        armageddonType: ArmageddonType
    ): ArenaId {
        check(redPlayer!=bluePlayer)
        val arenaId=ArenaId(arenaIdText)
        check(arenaId !in handles) {
            "Arena already active: $arenaIdText"
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
        arenaService.register(context)

        val ledger=EconomyLedger()
        val runtimeState=
            NormalArenaRuntimeState
                .withCadence(
                    preflight.gameplay
                        .troopSpawnCadence
                )
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
                PricingMode.NORMAL
            )
        val sentExpFinalizer=
            PlayerSentMobDeathFinalizer(
                SentMobExpRewardService(
                    RecommendedMatureMobDefinitions,
                    ledger,
                    PricingMode.NORMAL
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
                    recovery
                        .restoreIfPossible(it)
                }
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
                    ResolvedArmageddonSelection(
                        armageddonType,
                        ArmageddonSelectionSource
                            .ENGINEERING_TEST
                    ),
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
                armageddonRuntime=
                    armageddonRuntime,
                progressionService=
                    progressionService,
                nextTransactionId=
                    nextTransactionId
            )
        handles[arenaId]=handle

        try {
            GuardBootstrapService()
                .bootstrap(
                    context,
                    BukkitGuardAnchorSpawnPort(
                        world
                    )
                )

            val preset=
                MatchRulePreset(
                    mode=MatchMode.NORMAL,
                    pricingMode=
                        PricingMode.NORMAL,
                    goldmineIncomeMode=
                        GoldmineIncomeMode.NORMAL,
                    progressionMode=
                        ProgressionMode
                            .CLASSIC_PROGRESSION,
                    startingBalance=
                        preflight.gameplay
                            .startingBalance,
                    mobKillCoinMultiplier=1L,
                    sentMobExpMultiplier=1L
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
                    context,
                    ledger,
                    bodyMutation
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
            runCatching {
                end.finish(
                    context,
                    MatchOutcome.Tie(
                        TimeoutTiePolicy
                            .ENGINEERING_CUSTOM,
                        "stage4_start_failure"
                    )
                )
            }
            arenaService.close(arenaId) {}
            handles.remove(arenaId)
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

    fun isActivePlayer(
        playerUuid: UUID
    ): Boolean =
        handleForPlayer(playerUuid) != null

    private fun handleForPlayer(
        playerUuid: UUID
    ): BukkitLiveArenaHandle? =
        handles.values.firstOrNull {
            playerUuid in
                it.context.redTeam.players ||
            playerUuid in
                it.context.blueTeam.players
        }

    fun handleMenuAction(
        invocation:
            dev.cubecrafttd.ui
                .MenuActionInvocation
    ): Any {
        val handle=
            handles.values.firstOrNull {
                invocation.playerUuid in
                    it.context.redTeam.players ||
                invocation.playerUuid in
                    it.context.blueTeam.players
            } ?: error(
                "Player is not in an active live arena"
            )

        val playerState=handle.session
            ?.players
            ?.get(invocation.playerUuid)
        val placementPending=
            playerState?.interaction
                ?.towerPlacement
                ?.pending != null

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
            return service.handle(
                invocation.playerUuid,
                invocation.actionId
            )
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

        val report=
            handle.endCoordinator
                .finish(
                    handle.context,
                    outcome
                )

        stage4Gate
            .recordArenaRoundTrip(
                report.teardown
            )

        arenaService.close(arenaId) {}
        return report
    }

    fun stopAll() {
        handles.keys.toList()
            .forEach {
                stop(it.value)
            }
    }
}
