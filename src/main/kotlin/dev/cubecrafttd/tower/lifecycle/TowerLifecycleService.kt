package dev.cubecrafttd.tower.lifecycle

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.map.*
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.tower.visual.*
import java.util.UUID

data class TowerPlacementCommand(
    val towerInstanceId: TowerInstanceId,
    val towerId: String,
    val ownerUuid: UUID,
    val team: TeamId,
    val clickedBlock: BlockPos,
    val autoCentre: Boolean,
    val path: TowerPath?,
    val bodyDefinition: TowerBodyDefinition,
    val rotation: QuarterTurn,
    val transactionId: Long,
    val correlationId: String
)

data class TowerUpgradeCommand(
    val towerInstanceId: TowerInstanceId,
    val actorUuid: UUID,
    val newLevel: Int,
    val newPath: TowerPath?,
    val newBodyDefinition: TowerBodyDefinition,
    val rotation: QuarterTurn,
    val transactionId: Long,
    val correlationId: String
)

data class TowerSellCommand(
    val towerInstanceId: TowerInstanceId,
    val actorUuid: UUID,
    val refundRecipientUuid: UUID,
    val transactionId: Long,
    val correlationId: String
)

data class TowerPlacementReceipt(
    val towerInstanceId: TowerInstanceId,
    val cost: Long,
    val baseCenter: BlockPos
)

data class TowerUpgradeReceipt(
    val towerInstanceId: TowerInstanceId,
    val upgradeCost: Long,
    val totalInvested: Long
)

data class TowerSellReceipt(
    val towerInstanceId: TowerInstanceId,
    val refund: Long,
    val conflictReport: TowerBodyConflictReport
)

class TowerLifecycleService(
    private val context: ArenaContext,
    private val economy: EconomyLedger,
    private val bodyMutation: TowerBodyMutationService,
    private val definitions: TowerDefinitionRepository =
        RecommendedMatureTowerDefinitions,
    private val interactionPolicy: TowerInteractionPolicy =
        EngineeringOwnerOnlyTowerInteractionPolicy,
    private val sellPolicy: TowerSellRefundPolicy =
        TowerSellRefundPolicy.RECOMMENDED_MATURE,
    private val placementResolver: TowerPlacementResolver =
        RecommendedPlacementResolver,
    private val spacingPolicy:
        SameTowerSpacingPolicy =
        SameTowerSpacingPolicy()
) {
    fun place(command: TowerPlacementCommand): TowerPlacementReceipt {
        check(context.state == ArenaState.RUNNING ||
            context.state == ArenaState.PREPARED) {
            "Tower placement requires PREPARED/RUNNING arena"
        }
        check(context.entityIndex.towersByInstanceId[command.towerInstanceId] == null) {
            "Tower instance already exists"
        }
        check(!economy.hasAppliedCorrelation(command.correlationId)) {
            "Tower purchase correlation already applied"
        }

        val definition = definitions.get(command.towerId)
        enforceTeamLimit(definition, command.team)

        val selectedPath = TowerPathPreselection.resolve(command.path)
        validateBodyIdentity(
            command.bodyDefinition,
            command.towerId,
            level = 1,
            path = selectedPath
        )

        val stage = TowerStageResolver.initial(definition, selectedPath)
        val cost = stage.stats.cost
            ?: error("Tower build cost unresolved for ${command.towerId}")

        val footprint = when (definition.footprint) {
            TowerFootprint.THREE_BY_THREE -> FootprintSize.THREE_BY_THREE
            TowerFootprint.FIVE_BY_FIVE -> FootprintSize.FIVE_BY_FIVE
        }

        val placement = placementResolver.resolve(
            context.mapRuntime,
            PlacementRequest(
                command.team,
                command.clickedBlock,
                footprint,
                command.autoCentre
            )
        )
        val resolvedCenter =
            placement.centerOrNull()
                ?: error(
                    "Placement rejected: " +
                        (placement as PlacementResolution.Rejected).reason
                )

        spacingPolicy.requireLegal(
            context,
            command.towerId,
            command.team,
            resolvedCenter
        )

        // Body preflight is side-effect free and captures all target blocks.
        bodyMutation.preflightPlacement(
            resolvedCenter,
            command.bodyDefinition,
            command.rotation
        )

        val payer = EconomyAccount(
            command.team,
            command.ownerUuid,
            EconomyCurrency.MATCH_COINS
        )
        check(economy.balance(payer) >= cost) {
            "Insufficient MATCH_COINS for tower"
        }

        val debit = EconomyTransaction(
            command.transactionId,
            context.gameTick,
            payer,
            -cost,
            EconomyReason.TOWER_PURCHASE,
            command.correlationId,
            truthFieldKey =
                "towers.${command.towerId}.level1.cost"
        )
        check(economy.apply(debit)) {
            "Tower purchase correlation already applied"
        }

        var bodyPlaced = false
        try {
            bodyMutation.place(
                command.towerInstanceId.value,
                resolvedCenter,
                command.bodyDefinition,
                command.rotation,
                context.gameTick
            )
            bodyPlaced = true

            val base = Vec3(
                resolvedCenter.x.toDouble(),
                resolvedCenter.y.toDouble(),
                resolvedCenter.z.toDouble()
            )
            val runtime = TowerRuntimeState(
                identity = TowerIdentity(
                    command.towerInstanceId,
                    command.towerId,
                    command.ownerUuid,
                    command.team
                ),
                upgrade = TowerUpgradeState(1, selectedPath),
                geometry = TowerGeometryState(
                    baseOrigin = base,
                    rangeOrigin = base,
                    firingOrigins = listOf(base),
                    bodyRevision = command.bodyDefinition.bodyRevision
                ),
                investment = TowerInvestmentState(cost)
            )
            context.entityIndex.registerTower(runtime)

            return TowerPlacementReceipt(
                command.towerInstanceId,
                cost,
                resolvedCenter
            )
        } catch (t: Throwable) {
            if (bodyPlaced) {
                runCatching {
                    bodyMutation.remove(command.towerInstanceId.value)
                }
            }
            compensate(
                payer,
                cost,
                EconomyReason.TOWER_PURCHASE,
                "${command.correlationId}:rollback",
                "placement_rollback"
            )
            throw t
        }
    }

    fun upgrade(command: TowerUpgradeCommand): TowerUpgradeReceipt {
        val tower = context.entityIndex.towersByInstanceId[
            command.towerInstanceId
        ] ?: error("Unknown tower ${command.towerInstanceId.value}")

        check(interactionPolicy.mayUpgrade(command.actorUuid, tower)) {
            "Actor may not upgrade this tower under current policy"
        }
        check(!economy.hasAppliedCorrelation(command.correlationId)) {
            "Tower upgrade correlation already applied"
        }
        check(tower.lifecycle == TowerLifecycleState.ACTIVE) {
            "Tower is already mutating"
        }
        check(command.newLevel > tower.upgrade.level) {
            "Upgrade level must increase"
        }

        val selectedPath = command.newPath ?: tower.upgrade.path
            ?: TowerPathPreselection.resolve(null)
        tower.upgrade.path?.let { existingPath ->
            check(selectedPath == existingPath) {
                "2021 path is preselected before placement and cannot switch " +
                    "from $existingPath to $selectedPath"
            }
        }
        validateBodyIdentity(
            command.newBodyDefinition,
            tower.identity.towerId,
            command.newLevel,
            selectedPath
        )

        val definition = definitions.get(tower.identity.towerId)
        val stage = TowerStageResolver.resolve(definition, selectedPath, command.newLevel)
        val cost = stage.stats.cost
            ?: error("Upgrade cost unresolved")

        val payer = EconomyAccount(
            tower.identity.team,
            command.actorUuid,
            EconomyCurrency.MATCH_COINS
        )
        check(economy.balance(payer) >= cost) {
            "Insufficient MATCH_COINS for upgrade"
        }

        // No world mutation before debit.
        val base = BlockPos(
            tower.geometry.baseOrigin.x.toInt(),
            tower.geometry.baseOrigin.y.toInt(),
            tower.geometry.baseOrigin.z.toInt()
        )

        bodyMutation.preflightUpgrade(
            command.towerInstanceId.value,
            base,
            command.newBodyDefinition,
            command.rotation
        )

        val oldLevel = tower.upgrade.level
        val oldPath = tower.upgrade.path
        val oldGeometry = tower.geometry
        val oldInvested = tower.investment.currentGameCoinsInvested

        check(
            economy.apply(
                EconomyTransaction(
                    command.transactionId,
                    context.gameTick,
                    payer,
                    -cost,
                    EconomyReason.TOWER_UPGRADE,
                    command.correlationId,
                    truthFieldKey =
                        "towers.${tower.identity.towerId}." +
                            "${selectedPath.name.lowercase()}." +
                            "level${command.newLevel}.cost"
                )
            )
        )
        tower.lifecycle = TowerLifecycleState.MUTATING

        try {
            bodyMutation.upgrade(
                command.towerInstanceId.value,
                base,
                command.newBodyDefinition,
                command.rotation,
                context.gameTick
            )
            tower.upgrade.level = command.newLevel
            tower.upgrade.path = selectedPath
            tower.geometry = tower.geometry.copy(
                bodyRevision =
                    command.newBodyDefinition.bodyRevision
            )
            tower.investment.currentGameCoinsInvested += cost
            tower.lifecycle = TowerLifecycleState.ACTIVE

            return TowerUpgradeReceipt(
                command.towerInstanceId,
                cost,
                tower.investment.currentGameCoinsInvested
            )
        } catch (t: Throwable) {
            tower.upgrade.level = oldLevel
            tower.upgrade.path = oldPath
            tower.geometry = oldGeometry
            tower.investment.currentGameCoinsInvested = oldInvested
            tower.lifecycle = TowerLifecycleState.ACTIVE
            compensate(
                payer,
                cost,
                EconomyReason.TOWER_UPGRADE,
                "${command.correlationId}:rollback",
                "upgrade_rollback"
            )
            throw t
        }
    }

    fun sell(command: TowerSellCommand): TowerSellReceipt {
        val tower = context.entityIndex.towersByInstanceId[
            command.towerInstanceId
        ] ?: error("Unknown tower ${command.towerInstanceId.value}")

        check(interactionPolicy.maySell(command.actorUuid, tower)) {
            "Actor may not sell this tower under current policy"
        }
        check(!economy.hasAppliedCorrelation(command.correlationId)) {
            "Tower sell correlation already applied"
        }
        check(tower.lifecycle == TowerLifecycleState.ACTIVE) {
            "Tower is already mutating"
        }

        val refund = sellPolicy.refund(
            tower.investment.currentGameCoinsInvested
        )
        val account = EconomyAccount(
            tower.identity.team,
            command.refundRecipientUuid,
            EconomyCurrency.MATCH_COINS
        )

        // Controller/index remains until body removal has completed. If an
        // external edit conflict exists, the block is left untouched but
        // ownership is released and conflict is reported.
        tower.lifecycle = TowerLifecycleState.REMOVING
        val removal = try {
            bodyMutation.remove(command.towerInstanceId.value)
        } catch (t: Throwable) {
            tower.lifecycle = TowerLifecycleState.ACTIVE
            throw t
        }
        context.entityIndex.unregisterTower(
            command.towerInstanceId
        ) ?: error("Tower vanished during sell")

        if (refund > 0L) {
            check(
                economy.apply(
                    EconomyTransaction(
                        command.transactionId,
                        context.gameTick,
                        account,
                        refund,
                        EconomyReason.TOWER_SELL_REFUND,
                        command.correlationId,
                        truthFieldKey = "tower.sellRate",
                        metadata = mapOf(
                            "sellRate" to sellPolicy.rate.toString(),
                            "sellBase" to sellPolicy.base.name,
                            "evidenceStatus" to
                                sellPolicy.evidenceStatus
                        )
                    )
                )
            ) { "Sell correlation already applied" }
        }

        return TowerSellReceipt(
            command.towerInstanceId,
            refund,
            removal.conflictReport
        )
    }

    private fun validateBodyIdentity(
        body: TowerBodyDefinition,
        towerId: String,
        level: Int,
        path: TowerPath
    ) {
        check(body.towerId == towerId)
        check(body.level == level)
        if (body.path != null) {
            check(body.path == path)
        }
    }

    private fun enforceTeamLimit(
        definition: TowerDefinition,
        team: TeamId
    ) {
        val limit = definition.teamLimit ?: return
        val count = context.entityIndex.towersByTeam
            .getValue(team)
            .mapNotNull {
                context.entityIndex.towersByInstanceId[it]
            }
            .count { it.identity.towerId == definition.towerId }
        check(count < limit) {
            "Team $team reached ${definition.towerId} limit $limit"
        }
    }

    private fun compensate(
        account: EconomyAccount,
        amount: Long,
        originalReason: EconomyReason,
        correlationId: String,
        rollbackReason: String
    ) {
        if (amount <= 0L) return
        check(
            economy.apply(
                EconomyTransaction(
                    transactionId =
                        correlationId.hashCode().toLong(),
                    arenaTick = context.gameTick,
                    account = account,
                    delta = amount,
                    reason = EconomyReason.TRANSACTION_ROLLBACK,
                    correlationId = correlationId,
                    metadata = mapOf(
                        "compensates" to originalReason.name,
                        "rollbackReason" to rollbackReason
                    )
                )
            )
        )
    }
}
