package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.castle.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.match.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.truth.*
import dev.cubecrafttd.troop.*
import java.util.UUID

object NormalRuntimeCompositionFixture {
    fun run(): List<FixtureResult> {
        val config =
            CompleteFallbackFixtureFactory
                .create()
        val resolved =
            NormalGameplayConfigResolver
                .resolve(config)

        val runtime=
            NormalArenaRuntimeState
                .withCadence(
                    resolved.troopSpawnCadence
                )

        val ledger=EconomyLedger()
        val composition =
            NormalArenaRuntimeCompositionFactory
                .create(
                    NormalArenaRuntimeDependencies(
                        economyLedger=ledger,
                        runtimeState=runtime,
                        routeAssignment=
                            ExplicitRouteAssignmentPolicy(
                                "red-route"
                            ),
                        mobSpawnPort=
                            MobEntitySpawnPort {
                                UUID.nameUUIDFromBytes(
                                    "spawn-${it.instanceId.value}"
                                        .toByteArray()
                                )
                            },
                        movementRate=
                            RuntimeFallbackBindings
                                .mobMovementRate(
                                    config
                                ),
                        positionUpdate=
                            MobPositionUpdatePort {
                                _,_ -> Unit
                            },
                        supportGeometry=
                            MobSupportGeometryProvider {
                                _,_ -> 2.0
                            },
                        towerGeometry=
                            MobGeometryProvider {
                                _,_ ->
                                MobGeometryView(
                                    100.0,true
                                )
                            },
                        towerConfigProvider=
                            TowerRuntimeConfigProvider {
                                _,tower,engine ->
                                val definition=
                                    RecommendedMatureTowerDefinitions
                                        .get(
                                            tower.identity
                                                .towerId
                                        )
                                val stage=
                                    TowerStageResolver.resolve(
                                        definition,
                                        tower.upgrade.path
                                            ?: dev.cubecrafttd.tower.visual
                                                .TowerPath.TOP,
                                        tower.upgrade.level
                                    )
                                engine.resolveConfig(
                                    tower,
                                    TargetPriorityPolicy.FIRST,
                                    true,
                                    RuntimeFallbackBindings
                                        .towerAttackInterval(
                                            config,
                                            tower.identity
                                                .towerId,
                                            stage
                                        )
                                )
                            },
                        towerPlannerProvider=
                            TowerAttackPlannerProvider {
                                _,tower ->
                                when(
                                    tower.identity.towerId
                                ) {
                                    "archer" ->
                                        DirectTowerAttackPlanners
                                            .archer()
                                    "mage" ->
                                        DirectTowerAttackPlanners
                                            .mage()
                                    "ice" ->
                                        DirectTowerAttackPlanners
                                            .ice(
                                                ArenaDeterministicRng(
                                                    1
                                                )
                                            )
                                    "poison" ->
                                        DirectTowerAttackPlanners
                                            .poison()
                                    else ->
                                        TowerAttackPlanner {
                                            _,target,ctx ->
                                            dev.cubecrafttd.combat
                                                .AttackBatch(
                                                    target.entityUuid,
                                                    emptyList(),
                                                    mapOf(
                                                        "fixture" to
                                                            ctx.towerInstanceId
                                                                .toString()
                                                    )
                                                )
                                        }
                                }
                            },
                        summonCountResolver=
                            RuntimeFallbackBindings
                                .towerSummonCounts(
                                    config
                                ),
                        summonSpawnPort=
                            TowerSummonSpawnPort {
                                UUID.nameUUIDFromBytes(
                                    "summon-${it.tower.identity.instanceId.value}-${it.ordinal}"
                                        .toByteArray()
                                )
                            },
                        summonRemovalPort=
                            TowerSummonRemovalPort {
                                true
                            },
                        guardGeometry=
                            GuardGeometryProvider {
                                _,_ ->
                                GuardGeometryView(
                                    100.0,true
                                )
                            },
                        mobRemovalPort=
                            TrackedEntityRemovalPort {
                                true
                            },
                        resolved=resolved
                    )
                )

        val slime=MobRuntimeState(
            MobIdentity(
                MobInstanceId(9),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000018009"
                ),
                "slime",
                UUID.fromString(
                    "00000000-0000-0000-0000-000000018001"
                ),
                TeamId.RED,
                1
            ),
            MobRouteState(
                "red-route",0,0.0,0.0
            ),
            MobCombatState(
                0.0,800.0,
                MobLifecycleState.MOVING
            )
        )
        val lethal =
            composition
                .combatBindings
                .lethalResolver
                .resolve(
                    slime,
                    DamageSourceIdentity
                        .CastleGuard(
                            TeamId.RED
                        )
                )

        return listOf(
            FixtureResult(
                "normal-composition-seven-phases",
                composition.phaseIds() ==
                    listOf(
                        "goldmine",
                        "troop-spawn",
                        "mob-movement",
                        "mob-support",
                        "leach-charge",
                        "tower-summons",
                        "tower-combat",
                        "castle-guards",
                        "castle-attacks",
                        "mob-cleanup"
                    )
            ),
            FixtureResult(
                "normal-composition-shared-slime-lethal",
                lethal.kind ==
                    MobLethalOutcomeKind.SHRUNK &&
                    slime.combat.health ==
                        600.0
            )
        )
    }
}
