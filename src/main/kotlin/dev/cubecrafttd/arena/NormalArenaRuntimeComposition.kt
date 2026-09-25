package dev.cubecrafttd.arena

import dev.cubecrafttd.castle.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.match.ResolvedNormalGameplayConfig
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.troop.*

data class NormalArenaRuntimeDependencies(
    val economyLedger: EconomyLedger,
    val runtimeState: NormalArenaRuntimeState,
    val routeAssignment: RouteAssignmentPolicy,
    val mobSpawnPort: MobEntitySpawnPort,
    val movementRate: MobMovementRateResolver,
    val positionUpdate: MobPositionUpdatePort,
    val supportGeometry: MobSupportGeometryProvider,
    val towerGeometry: MobGeometryProvider,
    val towerConfigProvider: TowerRuntimeConfigProvider,
    val towerPlannerProvider: TowerAttackPlannerProvider,
    val towerAttackFeedback:
        TowerAttackFeedbackPort =
        NoOpTowerAttackFeedbackPort,
    val summonCountResolver:
        TowerSummonCountResolver,
    val summonSpawnPort:
        TowerSummonSpawnPort,
    val summonRemovalPort:
        TowerSummonRemovalPort,
    val guardGeometry: GuardGeometryProvider,
    val mobRemovalPort: TrackedEntityRemovalPort,
    val mobDeathFinalization:
        MobDeathFinalizationPort =
        NoOpMobDeathFinalizationPort,
    val resolved: ResolvedNormalGameplayConfig,
    val mobIdAllocator: MobInstanceIdAllocator =
        MobInstanceIdAllocator()
)

data class NormalArenaRuntimeComposition(
    val engine: NormalArenaRuntimeEngine,
    val phases: List<ArenaTickPhase>,
    val combatBindings: MobCombatRuntimeBindings
) {
    fun phaseIds(): List<String> =
        engine.phaseIds()
}

object NormalArenaRuntimeCompositionFactory {
    fun create(
        deps: NormalArenaRuntimeDependencies
    ): NormalArenaRuntimeComposition {
        val combat =
            MobCombatRuntimeBindings
                .fromResolvedNormal(
                    deps.resolved
                        .slimeLethal
                )

        val phases =
            listOf<ArenaTickPhase>(
                GoldmineTickPhase(
                    deps.runtimeState,
                    GoldmineIncomeService(
                        deps.economyLedger
                    )
                ),
                TroopSpawnTickPhase(
                    runtime=deps.runtimeState,
                    routeAssignment=
                        deps.routeAssignment,
                    entitySpawn=
                        deps.mobSpawnPort,
                    idAllocator=
                        deps.mobIdAllocator
                ),
                MobMovementTickPhase(
                    movementRate=
                        deps.movementRate,
                    livePosition=
                        deps.positionUpdate,
                    giantRunSpeedMultiplier=
                        deps.resolved
                            .giantRunSpeedMultiplier,
                    iceSlowMovementMultiplier=
                        deps.resolved
                            .iceSlowMovementMultiplier
                ),
                MobSupportTickPhase(
                    witch=
                        deps.resolved.witchHeal,
                    creeperRegen=
                        deps.resolved
                            .creeperRegen,
                    giantRegen=
                        deps.resolved.giantRegen,
                    geometry=
                        deps.supportGeometry
                ),
                LeachChargeTickPhase(
                    geometry=
                        deps.towerGeometry,
                    config=
                        deps.resolved
                            .leachCharge,
                    lethalResolver=
                        combat.lethalResolver
                ),
                TowerSummonMaintenancePhase(
                    counts=
                        deps.summonCountResolver,
                    spawnPort=
                        deps.summonSpawnPort,
                    removalPort=
                        deps.summonRemovalPort
                ),
                TowerCombatTickPhase(
                    geometryProvider=
                        deps.towerGeometry,
                    configProvider=
                        deps.towerConfigProvider,
                    plannerProvider=
                        deps.towerPlannerProvider,
                    feedbackPort=
                        deps.towerAttackFeedback,
                    lethalResolver=
                        combat.lethalResolver
                ),
                GuardCombatTickPhase(
                    config=
                        deps.resolved.guardCombat,
                    geometryProvider=
                        deps.guardGeometry,
                    lethalResolver=
                        combat.lethalResolver
                ),
                CastleAttackTickPhase(
                    deps.resolved.castleAttack
                ),
                MobLifecycleCleanupTickPhase(
                    deps.mobRemovalPort,
                    deps.mobDeathFinalization
                )
            )

        val actual=
            phases.map {
                NormalArenaPhasePlanEntry(
                    it.id,it.order
                )
            }
        check(
            actual ==
                RecommendedNormalArenaPhasePlan
                    .entries
        ) {
            "Normal phase composition drift: $actual"
        }

        return NormalArenaRuntimeComposition(
            engine=
                NormalArenaRuntimeEngine(
                    phases
                ),
            phases=phases,
            combatBindings=combat
        )
    }
}
