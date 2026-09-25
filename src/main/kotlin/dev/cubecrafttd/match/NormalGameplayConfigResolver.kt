package dev.cubecrafttd.match

import dev.cubecrafttd.castle.*
import dev.cubecrafttd.economy.MatchStartingBalance
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.troop.TroopSpawnCadence
import dev.cubecrafttd.truth.*

data class ResolvedNormalGameplayConfig(
    val startingBalance: MatchStartingBalance,
    val goldmineFirstIncomeDelayTicks: ResolvedTruth<Long>,
    val troopSpawnCadence: TroopSpawnCadence,
    val castleAttack: CastleAttackResolvedConfig,
    val guardCombat: GuardResolvedCombatConfig,
    val witchHeal: WitchHealConfig,
    val creeperRegen: PassiveRegenConfig,
    val giantRegen: GiantRegenConfig,
    val giantRunSpeedMultiplier: ResolvedTruth<Double>,
    val slimeLethal: SlimeLethalConfig,
    val leachCharge: dev.cubecrafttd.tower.LeachChargeConfig,
    val chainTargetPolicy:
        ResolvedTruth<dev.cubecrafttd.tower.TowerChainTargetPolicy>,
    val towerDefaultTargetPriority:
        ResolvedTruth<dev.cubecrafttd.tower.TargetPriorityPolicy>,
    val iceSlowMovementMultiplier:
        ResolvedTruth<Double>? = null
)

object NormalGameplayConfigResolver {
    fun resolve(
        config: RuntimeFallbackConfigV1
    ): ResolvedNormalGameplayConfig {
        val blockers =
            GameplayFallbackCompletenessValidator
                .validate(config)
                .filter {
                    it.level ==
                        FallbackRequirementLevel
                            .BLOCKS_MECHANIC
                }

        check(blockers.isEmpty()) {
            "Normal gameplay has blocking unresolved fields: " +
                blockers.joinToString {
                    it.key
                }
        }

        val gate =
            TruthGate(
                config
                    .toEngineeringFallbackConfig()
            )

        fun long(
            key: String,
            status: EvidenceStatus
        ): ResolvedTruth<Long> =
            gate.resolveWithExplicitFallback(
                TruthField(
                    key,
                    null,
                    status
                )
            )

        fun double(
            key: String,
            status: EvidenceStatus
        ): ResolvedTruth<Double> =
            gate.resolveWithExplicitFallback(
                TruthField(
                    key,
                    null,
                    status
                )
            )

        val coins =
            long(
                "match.normalStartingCoins",
                EvidenceStatus.VIDEO_REQUIRED
            )
        val exp =
            long(
                "match.normalStartingExp",
                EvidenceStatus.VIDEO_REQUIRED
            )
        val firstIncome =
            long(
                "goldmine.firstIncomeDelayTicks",
                EvidenceStatus.VIDEO_REQUIRED
            )
        val spawnCadence =
            long(
                "troop.spawnCadenceTicks",
                EvidenceStatus.VIDEO_REQUIRED
            )

        val guardPriority =
            config.guardTargetPriority
                ?: error(
                    "guard.targetPriority unresolved"
                )

        return ResolvedNormalGameplayConfig(
            startingBalance =
                MatchStartingBalance(
                    coins.value,
                    exp.value
                ),
            goldmineFirstIncomeDelayTicks =
                firstIncome,
            troopSpawnCadence =
                TroopSpawnCadence(
                    spawnCadence.value,
                    "ENGINEERING_FALLBACK"
                ),
            castleAttack =
                CastleAttackResolvedConfig(
                    firstHitDelayTicks =
                        long(
                            "castle.firstHitDelayTicks",
                            EvidenceStatus
                                .VIDEO_REQUIRED
                        ),
                    ordinaryAttackIntervalTicks =
                        long(
                            "castle.ordinaryAttackIntervalTicks",
                            EvidenceStatus
                                .VIDEO_REQUIRED
                        )
                ),
            guardCombat =
                GuardResolvedCombatConfig(
                    damagePerArrow =
                        double(
                            "guard.damagePerArrow",
                            EvidenceStatus
                                .VIDEO_REQUIRED
                        ),
                    fireIntervalTicks =
                        long(
                            "guard.fireIntervalTicks",
                            EvidenceStatus
                                .VIDEO_REQUIRED
                        ),
                    rangeBlocks =
                        double(
                            "guard.rangeBlocks",
                            EvidenceStatus
                                .VIDEO_REQUIRED
                        ),
                    targetPriority =
                        ResolvedTruth(
                            guardPriority,
                            ResolutionSource
                                .ENGINEERING_FALLBACK
                        )
                ),
            witchHeal =
                RuntimeFallbackBindings
                    .witchHeal(config),
            creeperRegen =
                RuntimeFallbackBindings
                    .creeperRegen(config),
            giantRegen =
                RuntimeFallbackBindings
                    .giantRegen(config),
            giantRunSpeedMultiplier =
                RuntimeFallbackBindings
                    .giantRunMultiplier(config),
            slimeLethal =
                RuntimeFallbackBindings
                    .slimeLethal(config),
            leachCharge =
                RuntimeFallbackBindings
                    .leachCharge(config),
            chainTargetPolicy =
                RuntimeFallbackBindings
                    .towerChainTargetPolicy(config),
            towerDefaultTargetPriority =
                RuntimeFallbackBindings
                    .towerDefaultTargetPriority(config),
            iceSlowMovementMultiplier =
                RuntimeFallbackBindings
                    .iceSlowMovementMultiplier(config)
        )
    }
}
