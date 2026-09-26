package dev.cubecrafttd

import dev.cubecrafttd.arena.ArenaService
import dev.cubecrafttd.recovery.FilePlayerRecoveryJournal
import dev.cubecrafttd.recovery.JournaledPlayerRecoveryOrchestrator
import dev.cubecrafttd.player.PlayerSnapshotStore
import dev.cubecrafttd.player.PlayerSnapshotRoundTripService
import dev.cubecrafttd.testing.*
import dev.cubecrafttd.admin.*
import dev.cubecrafttd.truth.*
import dev.cubecrafttd.match.ArmageddonType
import dev.cubecrafttd.match.HistoricalPregameArmageddonVoteOption
import dev.cubecrafttd.match.HistoricalPregamePricingVoteOption
import dev.cubecrafttd.paper.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class CubeCraftTowerDefencePlugin : JavaPlugin() {
    private val arenaService = ArenaService()
    private lateinit var recoveryJournal: FilePlayerRecoveryJournal
    private lateinit var fallbackConfig: RuntimeFallbackConfigV1
    private lateinit var livePlayerState: BukkitPlayerStateAdapter
    private lateinit var recoveryCoordinator: JournaledPlayerRecoveryOrchestrator
    private lateinit var recoveryListener: BukkitRecoveryListener
    private lateinit var mapBindingConfig: PaperMapBindingConfig
    private lateinit var mapOperations: FarmMapOperationManager
    private lateinit var readinessService: PaperGameplayReadinessService
    private lateinit var stage4Gate: PaperStage4GateStore
    private lateinit var liveArenaController: BukkitNormalArenaController
    private lateinit var hotbarPreferences: BukkitHotbarPreferenceStore
    private lateinit var trackedDamageListener: BukkitTrackedMobDamageListener
    private lateinit var trackedMobEnvironmentListener: BukkitTrackedMobEnvironmentListener
    private lateinit var menuBridge: BukkitMenuBridge
    private lateinit var pregameVoteMenuBridge: BukkitMenuBridge
    private lateinit var aoePotionTargetListener: BukkitAoEPotionTargetListener
    private lateinit var towerInteractionListener: BukkitTowerInteractionListener
    private lateinit var matchHotbarListener: BukkitMatchHotbarListener
    private lateinit var towerPlacementListener: BukkitTowerPlacementListener
    private lateinit var matchSafetyListener: BukkitMatchSafetyListener
    private lateinit var matchDepartureListener: BukkitMatchDepartureListener
    private lateinit var oneVsOneQueue: BukkitOneVsOneQueueService
    private lateinit var rangefinderService: BukkitEngineeringRangefinderService
    private var fallbackMissing: List<MissingFallback> = emptyList()

    override fun onEnable() {
        dataFolder.mkdirs()
        saveDefaultConfig()
        stage4Gate = PaperStage4GateStore(this)
        fallbackConfig = BukkitRuntimeFallbackLoader.load(config)
        fallbackMissing = RuntimeFallbackValidator.validateForNormal(fallbackConfig)
        mapBindingConfig = BukkitMapBindingConfigLoader.load(config)
        File(dataFolder, "maps").mkdirs()
        val recoveryDirectory = dataFolder.toPath().resolve("recovery")
        recoveryJournal = FilePlayerRecoveryJournal(recoveryDirectory)
        livePlayerState = BukkitPlayerStateAdapter(server)
        recoveryCoordinator = JournaledPlayerRecoveryOrchestrator(
            livePlayerState,
            PlayerSnapshotStore(),
            recoveryJournal
        )
        recoveryListener =
            BukkitRecoveryListener(
                this,
                recoveryCoordinator,
                livePlayerState
            ) { uuid,comparison ->
                val passed=
                    comparison?.passed==true
                stage4Gate
                    .recordRestartRecovery(
                        passed
                    )
                if(passed) {
                    logger.info(
                        "Cross-restart recovery PASS for " +
                            uuid
                    )
                } else {
                    logger.warning(
                        "Cross-restart recovery FAIL for " +
                            uuid +
                            ": " +
                            (
                                comparison
                                    ?.mismatches
                                    ?.joinToString()
                                    ?: "live recapture error"
                            )
                    )
                }
            }
        recoveryListener.recoverAlreadyOnline()
        val recoveryJournalLoadFailures=
            recoveryJournal.loadFailures()
        mapOperations = FarmMapOperationManager(
            this,mapBindingConfig
        )
        readinessService = PaperGameplayReadinessService(
            this,
            fallbackConfig,
            mapBindingConfig,
            { recoveryListener.pendingCount() },
            { recoveryJournalLoadFailures.size },
            stage4Gate
        )
        hotbarPreferences =
            BukkitHotbarPreferenceStore(
                File(
                    dataFolder,
                    "player-hotbars.yml"
                )
            )
        liveArenaController = BukkitNormalArenaController(
            this,
            arenaService,
            recoveryCoordinator,
            mapBindingConfig,
            fallbackConfig,
            stage4Gate,
            hotbarPreferences
        )
        if(
            stage4Gate.status()
                .previousBootWasUnclean
        ) {
            liveArenaController
                .blockFarmReuseAfterUncleanRestart()
            logger.severe(
                "Previous TD process was not cleanly shut down. Farm reuse is hard-blocked until /ctdresetfarm completes a verified tagged-entity cleanup + schematic reset."
            )
        }
        oneVsOneQueue =
            BukkitOneVsOneQueueService(
                this,
                liveArenaController,
                readinessService,
                recoveryCoordinator
            )
        trackedDamageListener = BukkitTrackedMobDamageListener(
            this,
            TrackedMobPredicate {
                liveArenaController.trackedMob(it)
            },
            dev.cubecrafttd.paper.LivePlayerWeaponHitPort {
                liveArenaController.handlePlayerHit(it)
            }
        )
        trackedMobEnvironmentListener =
            BukkitTrackedMobEnvironmentListener(
                this,
                TrackedMobPredicate {
                    liveArenaController
                        .trackedMob(it)
                },
                liveArenaController::isActivePlayer
            )
        menuBridge = BukkitMenuBridge(
            this,
            BukkitEngineeringMenuRenderer(),
            BukkitMenuActionSink {
                invocation ->
                val navKind=
                    invocation.actionId
                        .takeIf {
                            it.startsWith(
                                "nav:"
                            )
                        }
                        ?.substringAfter(
                            "nav:"
                        )

                if(navKind!=null) {
                    runCatching {
                        liveArenaController
                            .dynamicMenuForPlayer(
                                invocation.playerUuid,
                                navKind
                            )
                    }.onSuccess { menu ->
                        menuBridge.open(
                            invocation.playerUuid,
                            menu.toLiveView()
                        )
                    }.onFailure { error ->
                        server.getPlayer(
                            invocation.playerUuid
                        )?.sendMessage(
                            "TD navigation ERROR: " +
                                "${error.javaClass.simpleName}: ${error.message}"
                        )
                    }
                } else {
                    runCatching {
                        liveArenaController
                            .handleMenuAction(
                                invocation
                            )
                    }.onSuccess { result ->
                        val player=
                            server.getPlayer(
                                invocation.playerUuid
                            )
                        when(result) {
                            is dev.cubecrafttd.tower.lifecycle.TowerPlacementInteractionResult.OpenBuilder ->
                                menuBridge.open(
                                    invocation.playerUuid,
                                    result.menu.toLiveView()
                                )
                            is dev.cubecrafttd.tower.lifecycle.TowerPlacementInteractionResult.OpenPathSelector ->
                                menuBridge.open(
                                    invocation.playerUuid,
                                    result.menu.toLiveView()
                                )
                            is dev.cubecrafttd.tower.lifecycle.TowerPlacementInteractionResult.Placed ->
                                player?.sendMessage(
                                    "Tower placed: ${result.result.selection.towerId}"
                                )
                            is dev.cubecrafttd.ui.MatchMenuActionResult.PotionUsePurchased -> {
                                player?.closeInventory()
                                player?.sendMessage(
                                    "AoE " +
                                        result.token.potionId +
                                        " armed. Right-click the world to choose its target area."
                                )
                            }
                            else -> {
                                val refreshKind=
                                    when {
                                        invocation.actionId
                                            .startsWith(
                                                "summoner:"
                                            ) ->
                                            "summoner"
                                        invocation.actionId
                                            .startsWith(
                                                "progression:"
                                            ) ->
                                            "progression"
                                        invocation.actionId
                                            .startsWith(
                                                "bazaar:"
                                            ) ->
                                            "bazaar"
                                        invocation.actionId
                                            .startsWith(
                                                "settings:"
                                            ) ->
                                            "settings"
                                        invocation.actionId
                                            .startsWith(
                                                "hotbar:"
                                            ) ->
                                            "hotbar"
                                        invocation.actionId
                                            .startsWith(
                                                "armageddon:"
                                            ) ->
                                            "armageddon"
                                        else -> null
                                    }

                                if(refreshKind!=null) {
                                    runCatching {
                                        liveArenaController
                                            .dynamicMenuForPlayer(
                                                invocation.playerUuid,
                                                refreshKind
                                            )
                                    }.onSuccess { menu ->
                                        menuBridge.open(
                                            invocation.playerUuid,
                                            menu.toLiveView()
                                        )
                                    }
                                }
                                player?.sendMessage(
                                    if(
                                        result is
                                            dev.cubecrafttd.match
                                                .EngineeringArmageddonVoteReceipt
                                    ) {
                                        "Armageddon vote: " +
                                            result.newVote +
                                            "; resolved=" +
                                            result.snapshot.selection.type +
                                            "; rule=" +
                                            result.snapshot.resolution +
                                            " (Engineering)"
                                    } else {
                                        "TD action: $result"
                                    }
                                )
                            }
                        }
                    }.onFailure {
                        error ->
                        server.getPlayer(
                            invocation.playerUuid
                        )?.sendMessage(
                            "TD action ERROR: " +
                                "${error.javaClass.simpleName}: ${error.message}"
                        )
                    }
                }
            }
        )
        pregameVoteMenuBridge =
            BukkitMenuBridge(
                this,
                BukkitEngineeringMenuRenderer(),
                BukkitMenuActionSink {
                    invocation ->
                    runCatching {
                        oneVsOneQueue
                            .handlePregameMenuAction(
                                invocation
                            )
                    }.onSuccess { result ->
                        val player=
                            server.getPlayer(
                                invocation.playerUuid
                            )
                        when(result) {
                            is BukkitPregameArmageddonVoteReport ->
                                player?.sendMessage(
                                    "Pregame Armageddon vote: " +
                                        result.option
                                )

                            is BukkitPregamePricingVoteReport ->
                                player?.sendMessage(
                                    "Pregame Pricing vote: " +
                                        result.option
                                )
                        }

                        runCatching {
                            oneVsOneQueue
                                .pregameVoteMenu(
                                    invocation.playerUuid
                                )
                        }.onSuccess { menu ->
                            pregameVoteMenuBridge
                                .open(
                                    invocation.playerUuid,
                                    menu.toLiveView()
                                )
                        }
                    }.onFailure { error ->
                        server.getPlayer(
                            invocation.playerUuid
                        )?.sendMessage(
                            "Pregame vote ERROR: " +
                                (
                                    error.message
                                        ?: error.javaClass
                                            .simpleName
                                )
                        )
                    }
                }
            )
        aoePotionTargetListener =
            BukkitAoEPotionTargetListener(
                this,
                liveArenaController
            )
        towerInteractionListener =
            BukkitTowerInteractionListener(
                this,
                liveArenaController,
                menuBridge
            )
        matchHotbarListener =
            BukkitMatchHotbarListener(
                this,
                liveArenaController,
                menuBridge
            )
        towerPlacementListener =
            BukkitTowerPlacementListener(
                this,
                liveArenaController,
                menuBridge
            )
        matchSafetyListener =
            BukkitMatchSafetyListener(
                this,
                liveArenaController
            )
        matchDepartureListener =
            BukkitMatchDepartureListener(
                this,
                liveArenaController
            )
        rangefinderService =
            BukkitEngineeringRangefinderService(
                this,
                liveArenaController
            )

        val domain = DomainFixtureSuite.runAll()
        val failed = domain.filterNot { it.passed }
        check(failed.isEmpty()) {
            "Pure-domain fixture gate failed: ${failed.joinToString { it.id }}"
        }
        stage4Gate.recordDomainFixtures(true)

        val adapterSmokeFailures =
            stage4Gate.runAndRecordAdapterSmoke()
        check(adapterSmokeFailures.isEmpty()) {
            "Paper adapter smoke gate failed: " +
                adapterSmokeFailures.joinToString()
        }

        val pendingRecovery =
            recoveryListener.pendingCount()
        val readiness = readinessService.inspect()
        logger.info(
            "CubeCraftTowerDefence shell v65 enabled; " +
                "domainFixtures=${domain.size}; " +
                "pendingRecoverySnapshots=${recoveryListener.pendingCount()}; " +
                "activeArenas=${arenaService.contexts().size}; " +
                "fallbackMissing=${fallbackMissing.size}; " +
                "readiness=${readiness.summary()}; " +
                "paperAdapters=LIVE_ADAPTER_SMOKE_PASSED"
        )

        if (fallbackMissing.isNotEmpty()) {
            val blocking = fallbackMissing.count {
                it.level == FallbackRequirementLevel.BLOCKS_MECHANIC
            }
            logger.warning(
                "Runtime fallback report: ${fallbackMissing.size} unresolved field group(s); " +
                    "$blocking currently block affected gameplay mechanics. " +
                    "No hidden defaults were injected."
            )
        }

        if (pendingRecovery > 0) {
            logger.warning(
                "$pendingRecovery player recovery snapshot(s) are pending. " +
                    "Shell stage preserves them; Paper player-state serializer/restore binding " +
                    "must be validated before production gameplay activation."
            )
        }

        if(recoveryJournalLoadFailures.isNotEmpty()) {
            logger.severe(
                "Recovery journal corruption gate is BLOCKING new TD matches. " +
                    "Unreadable files were preserved: " +
                    recoveryJournalLoadFailures
                        .joinToString {
                            it.fileName +
                                " (" +
                                it.reason +
                                ")"
                        }
            )
        }
    }

    override fun onDisable() {
        if (::oneVsOneQueue.isInitialized) {
            oneVsOneQueue.close()
        }
        if (::liveArenaController.isInitialized) {
            liveArenaController.stopAll()
        }
        if (::mapOperations.isInitialized) {
            mapOperations.cancelActivePaste()
        }
        arenaService.closeAll { context ->
            check(context.towerBodyLedger.isEmpty()) {
                "Shell disable found unexpected tower-body ledger state in ${context.arenaId.value}"
            }
        }
        val clean=
            arenaService.contexts().isEmpty() &&
                !mapOperations.hasActivePaste()
        if(::stage4Gate.isInitialized) {
            stage4Gate.markCleanShutdown(clean)
        }
        logger.info(
            "CubeCraftTowerDefence shell v65 disabled; " +
                "clean=$clean all arena contexts closed"
        )
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean = when (command.name.lowercase()) {
        "ctdjoin" -> {
            runPlayerQueueJoin(
                sender,args.toList()
            )
            true
        }

        "ctdleave" -> {
            runPlayerQueueLeave(
                sender,args.toList()
            )
            true
        }

        "ctdvote" -> {
            runPlayerPregameVote(
                sender,args.toList()
            )
            true
        }

        "ctdstatus" -> {
            sender.sendMessage(
                "CubeCraft TD: stage=engineering-playtest-shell-v65, " +
                    "enabled=$isEnabled, activeArenas=${arenaService.contexts().size}, " +
                    "queuedPlayers=${if(::oneVsOneQueue.isInitialized) oneVsOneQueue.queuedPlayerCount() else 0}, " +
                    "reuse=" +
                    (if(::liveArenaController.isInitialized)
                        liveArenaController.farmReuseStatus().summary()
                    else
                        "uninitialized") +
                    ", fallbackMissing=${fallbackMissing.size}, " +
                    "readiness=${readinessService.inspect().summary()}, " +
                    "stage4=${stage4Gate.status().summary()}"
            )
            true
        }

        "ctdfixtures" -> {
            val results = DomainFixtureSuite.runAll()
            stage4Gate.recordDomainFixtures(
                results.all { it.passed }
            )
            report(sender, "domain", results)
            true
        }

        "ctdmapcheck" -> {
            runFarmMapCheck(sender)
            true
        }

        "ctdready" -> {
            reportReadiness(sender)
            true
        }

        "ctdlivegate" -> {
            sender.sendMessage(
                "CubeCraft TD Stage-4: " +
                    stage4Gate.status().summary()
            )
            true
        }

        "ctdreuse" -> {
            if(
                args.size!=1 ||
                !args[0].equals(
                    "status",
                    ignoreCase=true
                )
            ) {
                sender.sendMessage(
                    "Usage: /ctdreuse status"
                )
            } else {
                val reuse=
                    liveArenaController
                        .farmReuseStatus()
                sender.sendMessage(
                    "Farm reuse gate: " +
                        reuse.summary()
                )
                if(
                    reuse.hardTowerConflictKeys
                        .isNotEmpty()
                ) {
                    sender.sendMessage(
                        "Hard tower-body residue requires a verified map repair/reset before this gate may be cleared."
                    )
                }
                if(
                    reuse.uncleanRestartSuspectedResidue
                ) {
                    sender.sendMessage(
                        "Unclean restart residue is suspected. Run the verified Farm reset; it will remove persistently-tagged TD entities in the Farm volume and then verify the schematic before reuse is unlocked."
                    )
                }
                if(
                    reuse.liveTrackedEntityResidue
                        .isNotEmpty()
                ) {
                    sender.sendMessage(
                        "Live tracked-entity residue UUIDs: " +
                            reuse.liveTrackedEntityResidue
                                .joinToString()
                    )
                }
            }
            true
        }

        "ctdsnapshotcheck" -> {
            runPlayerSnapshotRoundTripCheck(
                sender,
                args.toList()
            )
            true
        }

        "ctdperf" -> {
            runPerformanceReport(
                sender,
                args.toList()
            )
            true
        }

        "ctdmapplan" -> {
            runFarmMapPlan(sender)
            true
        }

        "ctdpreflight" -> {
            runLiveCompositionPreflight(sender)
            true
        }

        "ctdfallbacks" -> {
            reportFallbackCompleteness(sender)
            true
        }

        "ctdarmageddonfallbacks" -> {
            reportArmageddonFallbacks(
                sender,args.toList()
            )
            true
        }

        "ctdpastefarm" -> {
            runFarmPaste(sender,args.toList())
            true
        }

        "ctdresetfarm" -> {
            runFarmReset(
                sender,
                args.toList()
            )
            true
        }

        "ctdtest" -> {
            runAdminTest(sender,args.toList())
            true
        }

        "ctdlivetest" -> {
            runLiveArenaTest(sender,args.toList())
            true
        }

        "ctdmenu" -> {
            runLiveMenuTest(
                sender,args.toList()
            )
            true
        }

        "ctdplaytestsetup" -> {
            runEngineeringPlaytestSetup(
                sender,args.toList()
            )
            true
        }

        else -> false
    }

    private fun runPlayerPregameVote(
        sender: CommandSender,
        args: List<String>
    ) {
        val player=
            sender as?
                org.bukkit.entity.Player
                ?: run {
                    sender.sendMessage(
                        "ctdvote requires a player"
                    )
                    return
                }

        if(args.isEmpty()) {
            runCatching {
                oneVsOneQueue
                    .pregameVoteMenu(
                        player.uniqueId
                    )
            }.onSuccess { menu ->
                pregameVoteMenuBridge
                    .open(
                        player.uniqueId,
                        menu.toLiveView()
                    )
            }.onFailure { error ->
                sender.sendMessage(
                    "ctdvote ERROR: " +
                        (
                            error.message
                                ?: error.javaClass
                                    .simpleName
                        )
                )
            }
            return
        }

        if(args.size!=2) {
            sender.sendMessage(
                "Usage: /ctdvote [<armageddon|pricing> <option>]"
            )
            sender.sendMessage(
                "No arguments opens the safe pregame voting GUI."
            )
            sender.sendMessage(
                "Armageddon: random|wither|lightning|horde"
            )
            sender.sendMessage(
                "Pricing: normal|double|quick"
            )
            return
        }

        when(args[0].lowercase()) {
            "armageddon" -> {
                val option=
                    runCatching {
                        HistoricalPregameArmageddonVoteOption
                            .valueOf(
                                args[1].uppercase()
                            )
                    }.getOrElse {
                        sender.sendMessage(
                            "Armageddon vote must be random/wither/lightning/horde"
                        )
                        return
                    }

                runCatching {
                    oneVsOneQueue
                        .castArmageddonVote(
                            player.uniqueId,
                            option
                        )
                }.onSuccess { report ->
                    sender.sendMessage(
                        "Pregame Armageddon vote: " +
                            report.option +
                            if(report.countdownRunning)
                                " (start countdown is running)"
                            else
                                " (queue position #" +
                                    report.waitingPosition +
                                    ")"
                    )
                }.onFailure { error ->
                    sender.sendMessage(
                        "ctdvote ERROR: " +
                            (
                                error.message
                                    ?: error.javaClass
                                        .simpleName
                            )
                    )
                }
            }

            "pricing" -> {
                val option=
                    when(
                        args[1]
                            .lowercase()
                    ) {
                        "normal" ->
                            HistoricalPregamePricingVoteOption
                                .NORMAL
                        "double",
                        "double_income",
                        "doubleincome" ->
                            HistoricalPregamePricingVoteOption
                                .DOUBLE_INCOME
                        "quick",
                        "quick_start",
                        "quickstart" ->
                            HistoricalPregamePricingVoteOption
                                .QUICK_START
                        else -> {
                            sender.sendMessage(
                                "Pricing vote must be normal/double/quick"
                            )
                            return
                        }
                    }

                runCatching {
                    oneVsOneQueue
                        .castPricingVote(
                            player.uniqueId,
                            option
                        )
                }.onSuccess { report ->
                    sender.sendMessage(
                        "Pregame Pricing vote: " +
                            report.option +
                            if(report.countdownRunning)
                                " (start countdown is running)"
                            else
                                " (queue position #" +
                                    report.waitingPosition +
                                    ")"
                    )
                }.onFailure { error ->
                    sender.sendMessage(
                        "ctdvote ERROR: " +
                            (
                                error.message
                                    ?: error.javaClass
                                        .simpleName
                            )
                    )
                }
            }

            else -> {
                sender.sendMessage(
                    "Vote category must be armageddon or pricing"
                )
            }
        }
    }

    private fun runPlayerQueueJoin(
        sender: CommandSender,
        args: List<String>
    ) {
        if(args.isNotEmpty()) {
            sender.sendMessage(
                "Usage: /ctdjoin"
            )
            return
        }

        val player=
            sender as?
                org.bukkit.entity.Player
                ?: run {
                    sender.sendMessage(
                        "ctdjoin requires a player"
                    )
                    return
                }

        runCatching {
            oneVsOneQueue.join(
                player.uniqueId
            )
        }.onSuccess { report ->
            when {
                !report.added ->
                    sender.sendMessage(
                        "Already waiting for TD 1v1; position #" +
                            report.position
                    )

                report.blockingCode!=null ->
                    sender.sendMessage(
                        "Joined TD 1v1 queue at position #" +
                            report.position +
                            ". Start is currently blocked by " +
                            report.blockingCode +
                            "."
                    )

                else ->
                    sender.sendMessage(
                        "Joined TD 1v1 queue at position #" +
                            report.position +
                            ". Waiting players=" +
                            report.waitingCount +
                            ". Use /ctdvote to open the pregame voting menu."
                    )
            }
        }.onFailure { error ->
            sender.sendMessage(
                "ctdjoin ERROR: " +
                    (
                        error.message
                            ?: error.javaClass
                                .simpleName
                    )
            )
        }
    }

    private fun runPlayerQueueLeave(
        sender: CommandSender,
        args: List<String>
    ) {
        if(args.isNotEmpty()) {
            sender.sendMessage(
                "Usage: /ctdleave"
            )
            return
        }

        val player=
            sender as?
                org.bukkit.entity.Player
                ?: run {
                    sender.sendMessage(
                        "ctdleave requires a player"
                    )
                    return
                }

        runCatching {
            oneVsOneQueue.leave(
                player.uniqueId
            )
        }.onSuccess { report ->
            when {
                report.removedFromWaiting ->
                    sender.sendMessage(
                        "Left the TD 1v1 waiting queue."
                    )

                report.cancelledStartCountdown ->
                    sender.sendMessage(
                        "Left the TD start countdown; the other player returned to the front of the queue."
                    )

                report.leftActiveMatch ->
                    sender.sendMessage(
                        "Left the TD match; restoredNow=" +
                            report.restoredNow +
                            ", arenaCleaned=" +
                            report.arenaCleaned
                    )

                else ->
                    sender.sendMessage(
                        "You are not waiting for or playing a TD match."
                    )
            }
        }.onFailure { error ->
            sender.sendMessage(
                "ctdleave ERROR: " +
                    (
                        error.message
                            ?: error.javaClass
                                .simpleName
                    )
            )
        }
    }

    private fun runFarmMapCheck(sender: CommandSender) {
        val file = File(dataFolder, "maps/ImprovedFarm.schem")
        if (!file.isFile) {
            sender.sendMessage(
                "Missing ${file.path}. Put the verified ImprovedFarm.schem there; " +
                    "the plugin does not bundle or silently substitute a map asset."
            )
            return
        }

        try {
            val bytes = file.readBytes()
            val results = buildList {
                addAll(FarmSchematicDecoderFixture.run(bytes))
                addAll(FarmSchematicClassifierFixture.run(bytes))
                addAll(FarmRawSchematicEndToEndFixture.run(bytes))
                addAll(FarmMobMovementFixture.run(bytes))
                addAll(FarmAdvancedPlacementFixture.run(bytes))
                addAll(FarmBlockEntitySafetyFixture.run(bytes))
            }
            val passed=
                results.all { it.passed }
            stage4Gate
                .recordFarmMapCheck(passed)
            report(
                sender,
                "Farm raw-schematic",
                results
            )
        } catch (t: Throwable) {
            sender.sendMessage("Farm map check ERROR: ${t.javaClass.simpleName}: ${t.message}")
            logger.warning("Farm map check failed: ${t.stackTraceToString()}")
        }
    }



    private fun runPerformanceReport(
        sender: CommandSender,
        args: List<String>
    ) {
        if(
            args.firstOrNull()
                ?.equals(
                    "reset",
                    ignoreCase=true
                ) == true
        ) {
            if(args.size>2) {
                sender.sendMessage(
                    "Usage: /ctdperf reset [arenaId]"
                )
                return
            }
            val arenaId=
                args.getOrNull(1)
            val count=
                liveArenaController
                    .resetPerformance(
                        arenaId
                    )
            sender.sendMessage(
                "CubeCraft TD profiler reset: arenas=" +
                    count
            )
            return
        }

        if(args.size>1) {
            sender.sendMessage(
                "Usage: /ctdperf [arenaId|reset [arenaId]]"
            )
            return
        }

        val snapshots=
            liveArenaController
                .performanceSnapshots(
                    args.firstOrNull()
                )
        if(snapshots.isEmpty()) {
            sender.sendMessage(
                "CubeCraft TD profiler: no matching active arena."
            )
            return
        }

        fun micros(
            nanos: Long
        ): Long =
            nanos / 1_000L

        snapshots.forEach { snapshot ->
            val live=
                snapshot.liveTick
            val core=
                snapshot.coreTick
            sender.sendMessage(
                "TD perf " +
                    snapshot.arenaId +
                    ": tick=" +
                    snapshot.gameTick +
                    " towers=" +
                    snapshot.towerCount +
                    " mobs=" +
                    snapshot.mobCount +
                    " guards=" +
                    snapshot.guardCount +
                    " displays=" +
                    snapshot.transientDisplayCount +
                    " projectiles=" +
                    snapshot.projectileCount
            )
            sender.sendMessage(
                " live us last/avg/max=" +
                    micros(live.lastNanos) +
                    "/" +
                    micros(live.averageNanos) +
                    "/" +
                    micros(live.maxNanos) +
                    " slow>=50ms=" +
                    live.slowTicksOver50ms +
                    "/" +
                    live.ticks
            )
            sender.sendMessage(
                " core us last/avg/max=" +
                    micros(core.lastNanos) +
                    "/" +
                    micros(core.averageNanos) +
                    "/" +
                    micros(core.maxNanos) +
                    " slow>=50ms=" +
                    core.slowTicksOver50ms +
                    "/" +
                    core.ticks
            )
            core.phases.forEach {
                (id,phase) ->
                sender.sendMessage(
                    "  " + id +
                        " us last/avg/max=" +
                        micros(
                            phase.lastNanos
                        ) +
                        "/" +
                        micros(
                            phase.averageNanos
                        ) +
                        "/" +
                        micros(
                            phase.maxNanos
                        ) +
                        " calls=" +
                        phase.calls
                )
            }
        }
    }

    private fun runPlayerSnapshotRoundTripCheck(
        sender: CommandSender,
        args: List<String>
    ) {
        if(args.size>1) {
            sender.sendMessage(
                "Usage: /ctdsnapshotcheck [restart-arm|restart-status]"
            )
            return
        }

        val mode=
            args.firstOrNull()
                ?.lowercase()
                ?: "roundtrip"

        if(mode=="restart-status") {
            val status=
                stage4Gate.status()
            sender.sendMessage(
                "Restart recovery evidence: passed=" +
                    status.restartRecoveryPassed +
                    ", pendingFromPreviousProcess=" +
                    recoveryListener
                        .pendingFromPreviousProcessCount() +
                    ", pendingTotal=" +
                    recoveryListener
                        .pendingCount()
            )
            return
        }

        if(
            mode!="roundtrip" &&
            mode!="restart-arm"
        ) {
            sender.sendMessage(
                "Usage: /ctdsnapshotcheck [restart-arm|restart-status]"
            )
            return
        }

        val player=
            sender as?
                org.bukkit.entity.Player
        if(player==null) {
            sender.sendMessage(
                "This live snapshot check must be run by an online player."
            )
            return
        }

        val uuid=
            player.uniqueId
        if(
            ::liveArenaController
                .isInitialized &&
            liveArenaController
                .isActivePlayer(uuid)
        ) {
            sender.sendMessage(
                "Snapshot check refused while you are in an active TD arena."
            )
            return
        }
        if(
            uuid in
                recoveryCoordinator
                    .pending()
        ) {
            sender.sendMessage(
                "Snapshot check refused because you already have a pending recovery snapshot."
            )
            return
        }
        if(
            player.isDead ||
            player.isInsideVehicle ||
            player.isSleeping
        ) {
            sender.sendMessage(
                "Snapshot check requires a living, awake player who is not inside a vehicle."
            )
            return
        }

        if(mode=="restart-arm") {
            runCatching {
                recoveryCoordinator
                    .captureBeforeMatch(
                        uuid,
                        47L
                    )
            }.onSuccess {
                stage4Gate
                    .beginRestartRecoveryProbe()
                sender.sendMessage(
                    "Restart recovery probe ARMED. Your exact state is durable in the TD recovery journal and you are now in temporary match-prepared state."
                )
                sender.sendMessage(
                    "Restart the server now, then reconnect. The plugin restores and compares your state before deleting the journal. Check with /ctdsnapshotcheck restart-status."
                )
            }.onFailure { error ->
                sender.sendMessage(
                    "Restart recovery arm ERROR: " +
                        error.javaClass.simpleName +
                        ": " +
                        error.message
                )
            }
            return
        }

        runCatching {
            PlayerSnapshotRoundTripService(
                livePlayerState
            ).run(
                uuid,
                0L
            )
        }.onSuccess { report ->
            stage4Gate
                .recordPlayerSnapshotRoundTrip(
                    report.passed
                )
            if(report.passed) {
                sender.sendMessage(
                    "Player snapshot round-trip PASS: all captured fields restored losslessly."
                )
            } else {
                sender.sendMessage(
                    "Player snapshot round-trip FAIL: " +
                        report.mismatches
                            .joinToString()
                )
            }
        }.onFailure { error ->
            stage4Gate
                .recordPlayerSnapshotRoundTrip(
                    false
                )
            sender.sendMessage(
                "Player snapshot round-trip ERROR: " +
                    error.javaClass.simpleName +
                    ": " +
                    error.message
            )
            logger.warning(
                "Player snapshot round-trip check failed: " +
                    error.stackTraceToString()
            )
        }
    }

    private fun reportReadiness(
        sender: CommandSender
    ) {
        val report=readinessService.inspect()
        sender.sendMessage(
            "CubeCraft TD readiness: ${report.summary()}"
        )
        report.issues.forEach {
            sender.sendMessage(
                "${it.severity}: ${it.code} — ${it.detail}"
            )
        }
    }

    private fun runFarmMapPlan(
        sender: CommandSender
    ) {
        try {
            val p=mapOperations.plan()
            sender.sendMessage(
                "Farm plan PASS: world=${p.worldName} " +
                    "uid=${p.worldUid} origin=${p.origin} " +
                    "dims=${p.dimensions} " +
                    "blockEntities=${p.blockEntityCount} " +
                    "entities=${p.schematicEntityCount} " +
                    "routes=${p.routeIds} " +
                    "guardAnchors=${p.guardAnchorCounts} " +
                    "sha256=${p.sha256}"
            )
        } catch(t:Throwable) {
            sender.sendMessage(
                "Farm plan ERROR: ${t.javaClass.simpleName}: ${t.message}"
            )
        }
    }




    private fun reportArmageddonFallbacks(
        sender: CommandSender,
        args: List<String>
    ) {
        if(args.size!=1) {
            sender.sendMessage(
                "Usage: /ctdarmageddonfallbacks <wither|lightning|horde>"
            )
            return
        }

        val type: ArmageddonType =
            try {
                ArmageddonType.valueOf(
                    args[0].uppercase()
                )
            } catch(_: IllegalArgumentException) {
                sender.sendMessage(
                    "Armageddon type must be wither/lightning/horde"
                )
                return
            }

        val missing=
            ArmageddonFallbackValidator
                .validate(
                    type,fallbackConfig
                )
        sender.sendMessage(
            "$type Armageddon fallback: missing=${missing.size}"
        )
        missing.forEach {
            sender.sendMessage(
                "BLOCKING ${it.key} — ${it.reason}"
            )
        }
    }

    private fun reportFallbackCompleteness(
        sender: CommandSender
    ) {
        val missing=
            GameplayFallbackCompletenessValidator
                .validate(fallbackConfig)
        val blocking=
            missing.filter {
                it.level ==
                    FallbackRequirementLevel
                        .BLOCKS_MECHANIC
            }
        val warnings=
            missing.filter {
                it.level !=
                    FallbackRequirementLevel
                        .BLOCKS_MECHANIC
            }

        sender.sendMessage(
            "CubeCraft TD fallback completeness: " +
                "blocking=${blocking.size}, " +
                "warnings=${warnings.size}"
        )

        blocking.forEach {
            sender.sendMessage(
                "BLOCKING ${it.key} — ${it.reason}"
            )
        }
        warnings.forEach {
            sender.sendMessage(
                "WARNING ${it.key} — ${it.reason}"
            )
        }
    }

    private fun runLiveCompositionPreflight(
        sender: CommandSender
    ) {
        val readiness=readinessService.inspect()
        val nonLiveBlockers=
            readiness.blockers.filterNot {
                it.code ==
                    "PAPER_LIVE_GATE_NOT_CERTIFIED"
            }
        if(nonLiveBlockers.isNotEmpty()) {
            sender.sendMessage(
                "Composition preflight BLOCKED before construction:"
            )
            nonLiveBlockers.forEach {
                sender.sendMessage(
                    "${it.code}: ${it.detail}"
                )
            }
            return
        }

        try {
            val r=
                BukkitLiveCompositionPreflight(
                    this,
                    mapBindingConfig,
                    fallbackConfig
                ).run()
            sender.sendMessage(
                "Composition preflight PASS: " +
                    "world=${r.worldName}, " +
                    "map=${r.mapRuntime.mapId}, " +
                    "routes=${r.mapRuntime.routesById.keys.sorted()}, " +
                    "normalStart=${r.gameplay.startingBalance.coins}C/" +
                    "${r.gameplay.startingBalance.exp}EXP, " +
                    "spawnCadence=${r.gameplay.troopSpawnCadence.intervalTicks}t, " +
                    "goldmineFirstDelay=${r.gameplay.goldmineFirstIncomeDelayTicks.value}t, " +
                    "castleFirst=${r.gameplay.castleAttack.firstHitDelayTicks.value}t, " +
                    "castleRepeat=${r.gameplay.castleAttack.ordinaryAttackIntervalTicks.value}t, " +
                    "witchMode=${r.gameplay.witchHeal.mode.value}, " +
                    "witchValue=${r.gameplay.witchHeal.healValue.value}, " +
                    "creeperRegen=${r.gameplay.creeperRegen.healthPerSecond.value}/s, " +
                    "giantRegen=${r.gameplay.giantRegen.healthPerSecond.value}/s, " +
                    "giantRunX=${r.gameplay.giantRunSpeedMultiplier.value}, " +
                    "iceSlowX=${r.gameplay.iceSlowMovementMultiplier?.value}, " +
                    "slimeMaxPhase=${r.gameplay.slimeLethal.maxShrinkPhaseIndex.value}, " +
                    "chainPolicy=${r.gameplay.chainTargetPolicy.value}, " +
                    "towerPriority=${r.gameplay.towerDefaultTargetPriority.value}"
            )
            sender.sendMessage(
                "Paper live certification is still separate: " +
                    stage4Gate.status().summary()
            )
        } catch(t:Throwable) {
            sender.sendMessage(
                "Composition preflight ERROR: " +
                    "${t.javaClass.simpleName}: ${t.message}"
            )
        }
    }

    private fun runFarmPaste(
        sender: CommandSender,
        args: List<String>
    ) {
        val expectedPrefix=
            PaperGameplayReadinessService
                .EXPECTED_FARM_SHA256
                .take(12)
        if(
            args.size!=1 ||
            args[0].lowercase()!=
                expectedPrefix
        ) {
            sender.sendMessage(
                "Safe paste requires the verified SHA prefix: " +
                    "/ctdpastefarm $expectedPrefix"
            )
            return
        }
        if(arenaService.contexts().isNotEmpty()) {
            sender.sendMessage(
                "Refusing map paste while any arena context exists."
            )
            return
        }
        try {
            mapOperations.startSafePaste(
                onProgress={ p ->
                    if(
                        p.phase=="COMPLETE" ||
                        p.phase=="FAILED"
                    ) {
                        logger.info(
                            "Farm paste ${p.phase}: " +
                                "${p.writtenBlocks}/${p.totalBlocks}"
                        )
                    }
                },
                onComplete={
                    logger.info(
                        "Farm safe paste complete"
                    )
                },
                onFailure={ t ->
                    logger.warning(
                        "Farm paste failed and rollback was attempted: " +
                            "${t.javaClass.simpleName}: ${t.message}"
                    )
                }
            )
            sender.sendMessage(
                "Farm safe paste started. Destination must be entirely AIR; " +
                    "any preflight conflict aborts before mutation."
            )
        } catch(t:Throwable) {
            sender.sendMessage(
                "Farm paste ERROR: ${t.javaClass.simpleName}: ${t.message}"
            )
        }
    }

    private fun runFarmReset(
        sender: CommandSender,
        args: List<String>
    ) {
        val expectedPrefix=
            PaperGameplayReadinessService
                .EXPECTED_FARM_SHA256
                .take(12)

        if(
            args.size!=1 ||
            args[0].lowercase()!=
                expectedPrefix
        ) {
            sender.sendMessage(
                "Verified reset requires the Farm SHA prefix: /ctdresetfarm " +
                    expectedPrefix
            )
            return
        }

        if(
            arenaService.contexts()
                .isNotEmpty()
        ) {
            sender.sendMessage(
                "Refusing Farm reset while any TD arena context exists."
            )
            return
        }

        if(
            mapOperations
                .hasActivePaste()
        ) {
            sender.sendMessage(
                "Refusing Farm reset while another Farm map operation is active."
            )
            return
        }

        val before=
            liveArenaController
                .farmReuseStatus()

        if(!before.blocked) {
            sender.sendMessage(
                "Farm reuse gate is already clean; refusing an unnecessary overwrite reset."
            )
            return
        }

        if(
            before.liveTrackedEntityResidue
                .isNotEmpty()
        ) {
            sender.sendMessage(
                "Farm reset cannot clear live tracked entities. Wait for/remove residue first; remaining=" +
                    before.liveTrackedEntityResidue.size
            )
            return
        }

        val wasAlreadyResetLocked=
            before.verifiedResetInProgress

        liveArenaController
            .beginFarmVerifiedReset()

        try {
            mapOperations
                .startVerifiedReset(
                    onProgress={ progress ->
                        if(
                            progress.phase=="COMPLETE" ||
                            progress.phase=="FAILED"
                        ) {
                            logger.info(
                                "Farm verified reset " +
                                    progress.phase +
                                    ": scanned=" +
                                    progress.scannedBlocks +
                                    " written=" +
                                    progress.writtenBlocks +
                                    "/" +
                                    progress.totalBlocks
                            )
                        }
                    },
                    onComplete={
                        liveArenaController
                            .clearFarmReuseAfterVerifiedWorldReset()
                        logger.info(
                            "Farm verified reset complete; reuse gate cleared"
                        )
                        sender.sendMessage(
                            "Farm verified reset COMPLETE; reuse gate is clean."
                        )
                    },
                    onFailure={ error ->
                        logger.warning(
                            "Farm verified reset FAILED; rollback was attempted and the persistent reuse gate remains locked: " +
                                error.javaClass.simpleName +
                                ": " +
                                error.message
                        )
                        sender.sendMessage(
                            "Farm verified reset FAILED; reuse gate remains locked. Retry /ctdresetfarm after checking the server log."
                        )
                    }
                )

            sender.sendMessage(
                "Farm verified reset started: full scan -> apply differences -> full verify. " +
                    "The persistent reuse gate stays locked until verification completes."
            )
        } catch(t:Throwable) {
            if(!wasAlreadyResetLocked) {
                liveArenaController
                    .abortFarmVerifiedResetBeforeMutation()
            }
            sender.sendMessage(
                "Farm reset ERROR before scheduling: " +
                    t.javaClass.simpleName +
                    ": " +
                    t.message
            )
        }
    }

    private fun runAdminTest(
        sender: CommandSender,
        args: List<String>
    ) {
        if (args.isEmpty()) {
            sender.sendMessage(
                "Usage: /ctdtest <fixtures|mapcheck|invariants|spawnmob|tower|armageddon|forceend> ..."
            )
            return
        }
        try {
            when (val intent = AdminTestCommandParser.parse(args)) {
                AdminTestIntent.RunFixtures ->
                    report(sender,"domain",DomainFixtureSuite.runAll())
                AdminTestIntent.MapCheck -> runFarmMapCheck(sender)
                AdminTestIntent.Invariants -> {
                    val contexts = arenaService.contexts()
                    if (contexts.isEmpty()) {
                        sender.sendMessage("No active arenas; invariant report has nothing to inspect.")
                    } else {
                        contexts.forEach { context ->
                            val r = dev.cubecrafttd.arena.ArenaRuntimeInvariantValidator.validate(context)
                            sender.sendMessage(
                                "arena=${context.arenaId.value} invariants=${if(r.valid) "PASS" else "FAIL"} issues=${r.issues.size}"
                            )
                            r.issues.forEach { sender.sendMessage("${it.code}: ${it.detail}") }
                        }
                    }
                }
                else -> sender.sendMessage(
                    "Parsed ${intent::class.simpleName}, but mutating test execution requires an active Paper arena composition root. " +
                        "The parser/binding is ready; live execution stays disabled until Stage-4 Java25/Paper26.2 gate passes."
                )
            }
        } catch (t: Throwable) {
            sender.sendMessage("ctdtest ERROR: ${t.message}")
        }
    }



    private fun runEngineeringPlaytestSetup(
        sender: CommandSender,
        args: List<String>
    ) {
        if(
            args.size!=1 ||
            args[0].lowercase()!="apply"
        ) {
            sender.sendMessage(
                "Usage: /ctdplaytestsetup apply"
            )
            sender.sendMessage(
                "This writes explicit ENGINEERING values to config.yml; they are not original CubeCraft truth."
            )
            return
        }

        val player=
            sender as?
                org.bukkit.entity.Player
                ?: run {
                    sender.sendMessage(
                        "ctdplaytestsetup must be run by a player standing at the intended map minimum corner."
                    )
                    return
                }

        if(arenaService.contexts().isNotEmpty()) {
            sender.sendMessage(
                "Stop all TD arenas before changing the playtest profile."
            )
            return
        }

        try {
            val r=
                BukkitEngineeringPlaytestConfigurator(
                    this
                ).apply(player)
            sender.sendMessage(
                "Engineering playtest profile saved: " +
                    r.profileId
            )
            sender.sendMessage(
                "world=" + r.worldName +
                    " origin=" + r.origin +
                    " redRoute=" + r.redRoute +
                    " blueRoute=" + r.blueRoute
            )
            r.configBackup?.let {
                sender.sendMessage(
                    "Strict config backup: " + it
                )
            }
            sender.sendMessage(
                "RESTART THE SERVER now. Then run /ctdmapcheck, /ctdpastefarm 28d24136afe8, /ctdpreflight, and /ctdlivetest start <arenaId> <redPlayer> <bluePlayer> wither."
            )
        } catch(t:Throwable) {
            sender.sendMessage(
                "ctdplaytestsetup ERROR: " +
                    t.javaClass.simpleName +
                    ": " + t.message
            )
            logger.warning(
                "Engineering playtest setup failed: " +
                    t.stackTraceToString()
            )
        }
    }

    private fun runLiveMenuTest(
        sender: CommandSender,
        args: List<String>
    ) {
        val player=
            sender as?
                org.bukkit.entity.Player
                ?: run {
                    sender.sendMessage(
                        "ctdmenu requires a player"
                    )
                    return
                }

        if(args.size!=1) {
            sender.sendMessage(
                "Usage: /ctdmenu <builder3|builder5|summoner|progression|bazaar|settings|armageddon>"
            )
            return
        }

        val definition=
            when(args[0].lowercase()) {
                "builder3" ->
                    dev.cubecrafttd.ui
                        .TowerBuilderMenus
                        .threeByThree2021
                "builder5" ->
                    dev.cubecrafttd.ui
                        .TowerBuilderMenus
                        .fiveByFiveEngineering
                "summoner",
                "progression",
                "bazaar",
                "settings",
                "armageddon" ->
                    runCatching {
                        liveArenaController
                            .dynamicMenuForPlayer(
                                player.uniqueId,
                                args[0]
                            )
                    }.getOrElse {
                        sender.sendMessage(
                            "Dynamic menu ERROR: ${it.message}"
                        )
                        return
                    }
                else -> {
                    sender.sendMessage(
                        "Unknown menu. Use builder3, builder5, summoner, progression, bazaar, settings, or armageddon."
                    )
                    return
                }
            }

        menuBridge.open(
            player.uniqueId,
            definition.toLiveView()
        )
    }

    private fun runLiveArenaTest(
        sender: CommandSender,
        args: List<String>
    ) {
        if(args.isEmpty()) {
            sender.sendMessage(
                "Usage: /ctdlivetest <start <arenaId> <redPlayer> <bluePlayer> <wither|lightning|horde>|stop <arenaId>|status>"
            )
            return
        }

        try {
            when(args[0].lowercase()) {
                "status" -> {
                    sender.sendMessage(
                        "Live test arenas: " +
                            liveArenaController
                                .activeArenaIds()
                    )
                }

                "start" -> {
                    check(args.size==5) {
                        "start <arenaId> <redPlayer> <bluePlayer> <wither|lightning|horde>"
                    }
                    val red=server.getPlayerExact(
                        args[2]
                    ) ?: error(
                        "Red player must be online"
                    )
                    val blue=server.getPlayerExact(
                        args[3]
                    ) ?: error(
                        "Blue player must be online"
                    )

                    val blockers=
                        readinessService.inspect()
                            .blockers
                            .filterNot {
                                it.code ==
                                    "PAPER_LIVE_GATE_NOT_CERTIFIED"
                            }
                    check(blockers.isEmpty()) {
                        "Non-live readiness blockers: " +
                            blockers.joinToString {
                                it.code
                            }
                    }

                    val id=
                        liveArenaController
                            .startOneVsOneTest(
                                args[1],
                                red.uniqueId,
                                blue.uniqueId,
                                ArmageddonType.valueOf(
                                    args[4].uppercase()
                                )
                            )
                    sender.sendMessage(
                        "Stage-4 live test arena started: ${id.value}. " +
                            "${args[4].uppercase()} is the Engineering default; players can vote " +
                            "for a runnable concrete Armageddon in Settings -> Armageddon vote. " +
                            "This is an engineering test controller, not production matchmaking."
                    )
                }

                "stop" -> {
                    check(args.size==2) {
                        "stop <arenaId>"
                    }
                    val report=
                        liveArenaController
                            .stop(args[1])
                    sender.sendMessage(
                        if(report==null)
                            "No live test arena named ${args[1]}"
                        else
                            "Arena ${args[1]} stopped; clean=${report.teardown.fullyCleanNow}, " +
                                "failedEntityRemovals=${report.teardown.failedEntityRemovals.size}, " +
                                "pendingPlayerRestores=${report.teardown.pendingPlayerRestores.size}"
                    )
                }

                else ->
                    error(
                        "Unknown live-test action ${args[0]}"
                    )
            }
        } catch(t:Throwable) {
            sender.sendMessage(
                "ctdlivetest ERROR: " +
                    "${t.javaClass.simpleName}: ${t.message}"
            )
            logger.warning(
                "Live arena test command failed: " +
                    t.stackTraceToString()
            )
        }
    }

    private fun report(
        sender: CommandSender,
        name: String,
        results: List<FixtureResult>
    ) {
        val failed = results.filterNot { it.passed }
        sender.sendMessage(
            "CubeCraft TD $name fixtures: ${results.size - failed.size}/${results.size} PASS"
        )
        failed.forEach {
            sender.sendMessage(
                "FAIL: ${it.id}" +
                    if (it.details.isEmpty()) "" else " — ${it.details.joinToString(" | ")}"
            )
        }
    }
}
