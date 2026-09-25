package dev.cubecrafttd.truth

import dev.cubecrafttd.castle.GuardTargetPriorityPolicy
import dev.cubecrafttd.mob.WitchHealMode
import dev.cubecrafttd.tower.TowerChainTargetPolicy
import dev.cubecrafttd.match.WitherTowerTargetPolicy

data class RuntimeFallbackConfigV1(
    val normalStartingCoins: Long? = null,
    val normalStartingExp: Long? = null,
    val goldmineFirstIncomeDelayTicks: Long? = null,

    val troopSpawnCadenceTicks: Long? = null,
    val mobSpeedBlocksPerTick:
        Map<String,Double> = emptyMap(),

    val castleFirstHitDelayTicks:
        Long? = null,
    val castleOrdinaryAttackIntervalTicks:
        Long? = null,

    val guardDamagePerArrow:
        Double? = null,
    val guardFireIntervalTicks:
        Long? = null,
    val guardRangeBlocks:
        Double? = null,
    val guardTargetPriority:
        GuardTargetPriorityPolicy? = null,

    val witchHealAmount:
        Double? = null,
    val witchHealMode:
        WitchHealMode? = null,
    val witchHealTargetCap:
        Int? = null,
    val witchHealIntervalTicks:
        Long? = null,
    val witchHealRadiusBlocks:
        Double? = null,

    val creeperRegenHealthPerSecond:
        Double? = null,
    val giantRegenHealthPerSecond:
        Double? = null,
    val giantRunSpeedMultiplier:
        Double? = null,
    val iceSlowMovementMultiplier:
        Double? = null,
    val burnIntervalTicks:
        Long? = null,
    val slimeMaxShrinkPhaseIndex:
        Int? = null,

    val leachMaxCharge:
        Double? = null,
    val leachChargePerTargetTick:
        Double? = null,

    val towerAttackIntervalSeconds:
        Map<String,Double> = emptyMap(),
    val towerSummonActiveCounts:
        Map<String,Int> = emptyMap(),
    val towerChainTargetPolicy:
        TowerChainTargetPolicy? = null,
    val towerDefaultTargetPriority:
        dev.cubecrafttd.tower.TargetPriorityPolicy? = null,
    val towerRequiresLineOfSight:
        Map<String,Boolean> = emptyMap(),

    val mobKillCoins:
        Map<String,Long> = emptyMap(),

    val weaponAbilityRequiredHits:
        Map<String,Int> = emptyMap(),

    val aoeNumericFallbacks:
        Map<String,Number> = emptyMap(),
    val aoeKillAwardsCoins:
        Boolean? = null,

    val lightningFirstStrikeDelayTicks:
        Long? = null,
    val lightningStrikeIntervalTicks:
        Long? = null,
    val lightningTowersPerTeamPerStrike:
        Int? = null,

    val witherSpeedBlocksPerTick:
        Double? = null,
    val witherRegenerationHealthPerSecond:
        Double? = null,

    val witherFirstTowerSkullDelayTicks:
        Long? = null,
    val witherTowerSkullIntervalTicks:
        Long? = null,
    val witherTowerSkullRangeBlocks:
        Double? = null,
    val witherTowerTargetPolicy:
        WitherTowerTargetPolicy? = null,

    val witherFirstTrailSkeletonDelayTicks:
        Long? = null,
    val witherTrailSkeletonIntervalTicks:
        Long? = null,
    val witherTrailSkeletonsPerEvent:
        Int? = null,
    val witherTrailSkeletonLevel:
        Int? = null,

    val hordeFirstWaveDelayTicks:
        Long? = null,
    val hordeWaveIntervalTicks:
        Long? = null,
    val hordeWavePlan:
        List<ArmageddonHordeFallbackWave> =
        emptyList()
) {
    init {
        normalStartingCoins?.let {
            require(it >= 0L)
        }
        normalStartingExp?.let {
            require(it >= 0L)
        }
        goldmineFirstIncomeDelayTicks?.let {
            require(it >= 0L)
        }

        troopSpawnCadenceTicks?.let {
            require(it > 0L)
        }
        mobSpeedBlocksPerTick.values
            .forEach { require(it >= 0.0) }
        castleFirstHitDelayTicks?.let {
            require(it >= 0L)
        }
        castleOrdinaryAttackIntervalTicks
            ?.let { require(it > 0L) }
        guardDamagePerArrow?.let {
            require(it >= 0.0)
        }
        guardFireIntervalTicks?.let {
            require(it > 0L)
        }
        guardRangeBlocks?.let {
            require(it >= 0.0)
        }
        witchHealAmount?.let {
            require(it >= 0.0)
        }
        witchHealTargetCap?.let {
            require(it > 0)
        }
        witchHealIntervalTicks?.let {
            require(it > 0L)
        }
        witchHealRadiusBlocks?.let {
            require(it >= 0.0)
        }
        creeperRegenHealthPerSecond?.let {
            require(it >= 0.0)
        }
        giantRegenHealthPerSecond?.let {
            require(it >= 0.0)
        }
        giantRunSpeedMultiplier?.let {
            require(it >= 1.0)
        }
        iceSlowMovementMultiplier?.let {
            require(it in 0.0..1.0)
        }
        burnIntervalTicks?.let {
            require(it > 0L)
        }
        slimeMaxShrinkPhaseIndex?.let {
            require(it >= 0)
        }
        leachMaxCharge?.let {
            require(it > 0.0)
        }
        leachChargePerTargetTick?.let {
            require(it >= 0.0)
        }
        towerAttackIntervalSeconds.values
            .forEach { require(it > 0.0) }
        towerSummonActiveCounts.values
            .forEach { require(it > 0) }
        mobKillCoins.values
            .forEach { require(it >= 0L) }
        weaponAbilityRequiredHits.values
            .forEach { require(it > 0) }

        lightningFirstStrikeDelayTicks?.let {
            require(it >= 0L)
        }
        lightningStrikeIntervalTicks?.let {
            require(it > 0L)
        }
        lightningTowersPerTeamPerStrike?.let {
            require(it > 0)
        }
        witherSpeedBlocksPerTick?.let {
            require(it >= 0.0)
        }
        witherRegenerationHealthPerSecond
            ?.let { require(it >= 0.0) }
        witherFirstTowerSkullDelayTicks?.let {
            require(it >= 0L)
        }
        witherTowerSkullIntervalTicks?.let {
            require(it > 0L)
        }
        witherTowerSkullRangeBlocks?.let {
            require(it >= 0.0)
        }
        witherFirstTrailSkeletonDelayTicks?.let {
            require(it >= 0L)
        }
        witherTrailSkeletonIntervalTicks
            ?.let { require(it > 0L) }
        witherTrailSkeletonsPerEvent?.let {
            require(it > 0)
        }
        witherTrailSkeletonLevel?.let {
            require(it in 4..5)
        }
        hordeFirstWaveDelayTicks?.let {
            require(it >= 0L)
        }
        hordeWaveIntervalTicks?.let {
            require(it > 0L)
        }
    }

    fun toEngineeringFallbackConfig():
        EngineeringFallbackConfig {
        val out =
            linkedMapOf<String,Any>()

        normalStartingCoins?.let {
            out["match.normalStartingCoins"] = it
        }
        normalStartingExp?.let {
            out["match.normalStartingExp"] = it
        }
        goldmineFirstIncomeDelayTicks?.let {
            out["goldmine.firstIncomeDelayTicks"] = it
        }

        troopSpawnCadenceTicks?.let {
            out["troop.spawnCadenceTicks"] = it
        }
        mobSpeedBlocksPerTick.forEach {
            (key,value) ->
            out["mob.speedBlocksPerTick.$key"] =
                value
        }

        castleFirstHitDelayTicks?.let {
            out["castle.firstHitDelayTicks"] =
                it
        }
        castleOrdinaryAttackIntervalTicks?.let {
            out[
                "castle.ordinaryAttackIntervalTicks"
            ] = it
        }

        guardDamagePerArrow?.let {
            out["guard.damagePerArrow"] = it
        }
        guardFireIntervalTicks?.let {
            out["guard.fireIntervalTicks"] = it
        }
        guardRangeBlocks?.let {
            out["guard.rangeBlocks"] = it
        }
        guardTargetPriority?.let {
            out["guard.targetPriority"] = it
        }

        witchHealAmount?.let {
            out["witch.healAmount"] = it
        }
        witchHealMode?.let {
            out["witch.healMode"] = it
        }
        witchHealTargetCap?.let {
            out["witch.healTargetCap"] = it
        }
        witchHealIntervalTicks?.let {
            out["witch.healIntervalTicks"] = it
        }
        witchHealRadiusBlocks?.let {
            out["witch.healRadiusBlocks"] = it
        }

        creeperRegenHealthPerSecond?.let {
            out["creeper.regenHealthPerSecond"] =
                it
        }
        giantRegenHealthPerSecond?.let {
            out["giant.regenHealthPerSecond"] =
                it
        }
        giantRunSpeedMultiplier?.let {
            out["giant.runSpeedMultiplier"] =
                it
        }
        iceSlowMovementMultiplier?.let {
            out["status.iceSlowMovementMultiplier"] =
                it
        }
        burnIntervalTicks?.let {
            out["status.burnIntervalTicks"] =
                it
        }
        slimeMaxShrinkPhaseIndex?.let {
            out["slime.maxShrinkPhaseIndex"] =
                it
        }

        leachMaxCharge?.let {
            out["leach.maxCharge"] = it
        }
        leachChargePerTargetTick?.let {
            out["leach.chargePerTargetTick"] =
                it
        }

        towerAttackIntervalSeconds.forEach {
            (key,value) ->
            out[
                "tower.attackIntervalSeconds.$key"
            ] = value
        }
        towerSummonActiveCounts.forEach {
            (key,value) ->
            out[
                "tower.summonActiveCounts.$key"
            ] = value
        }
        towerChainTargetPolicy?.let {
            out["tower.chainTargetPolicy"] = it
        }
        towerDefaultTargetPriority?.let {
            out["tower.defaultTargetPriority"] = it
        }
        towerRequiresLineOfSight.forEach {
            (towerId,value) ->
            out[
                "tower.requiresLineOfSight.$towerId"
            ] = value
        }
        mobKillCoins.forEach {
            (key,value) ->
            out["mob.killCoins.$key"] = value
        }
        weaponAbilityRequiredHits.forEach {
            (key,value) ->
            out[
                "weapon.abilityRequiredHits.$key"
            ] = value
        }
        aoeNumericFallbacks.forEach {
            (key,value) ->
            out["aoe.$key"] = value
        }
        aoeKillAwardsCoins?.let {
            out["aoe.killAwardsCoins"] = it
        }

        lightningFirstStrikeDelayTicks?.let {
            out[
                "armageddon.lightning.firstStrikeDelayTicks"
            ]=it
        }
        lightningStrikeIntervalTicks?.let {
            out[
                "armageddon.lightning.strikeIntervalTicks"
            ]=it
        }
        lightningTowersPerTeamPerStrike?.let {
            out[
                "armageddon.lightning.towersPerTeamPerStrike"
            ]=it
        }
        witherSpeedBlocksPerTick?.let {
            out[
                "armageddon.wither.speedBlocksPerTick"
            ]=it
        }
        witherRegenerationHealthPerSecond
            ?.let {
                out[
                    "armageddon.wither.regenerationHealthPerSecond"
                ]=it
            }
        witherFirstTowerSkullDelayTicks?.let {
            out[
                "armageddon.wither.firstTowerSkullDelayTicks"
            ]=it
        }
        witherTowerSkullIntervalTicks?.let {
            out[
                "armageddon.wither.towerSkullIntervalTicks"
            ]=it
        }
        witherTowerSkullRangeBlocks?.let {
            out[
                "armageddon.wither.towerSkullRangeBlocks"
            ]=it
        }
        witherTowerTargetPolicy?.let {
            out[
                "armageddon.wither.towerTargetPolicy"
            ]=it
        }
        witherFirstTrailSkeletonDelayTicks?.let {
            out[
                "armageddon.wither.firstTrailSkeletonDelayTicks"
            ]=it
        }
        witherTrailSkeletonIntervalTicks
            ?.let {
                out[
                    "armageddon.wither.trailSkeletonIntervalTicks"
                ]=it
            }
        witherTrailSkeletonsPerEvent?.let {
            out[
                "armageddon.wither.trailSkeletonsPerEvent"
            ]=it
        }
        witherTrailSkeletonLevel?.let {
            out[
                "armageddon.wither.trailSkeletonLevel"
            ]=it
        }
        hordeFirstWaveDelayTicks?.let {
            out[
                "armageddon.horde.firstWaveDelayTicks"
            ]=it
        }
        hordeWaveIntervalTicks?.let {
            out[
                "armageddon.horde.waveIntervalTicks"
            ]=it
        }

        return EngineeringFallbackConfig(out)
    }
}

enum class FallbackRequirementLevel {
    BLOCKS_MECHANIC,
    NUMERICAL_FIDELITY,
    OPTIONAL_ENGINEERING
}

data class MissingFallback(
    val key: String,
    val level: FallbackRequirementLevel,
    val reason: String
)

object RuntimeFallbackValidator {
    fun validateForNormal(
        config: RuntimeFallbackConfigV1
    ): List<MissingFallback> {
        val missing =
            mutableListOf<MissingFallback>()

        fun requireValue(
            present: Boolean,
            key: String,
            level:
                FallbackRequirementLevel,
            reason: String
        ) {
            if(!present) {
                missing += MissingFallback(
                    key,level,reason
                )
            }
        }

        requireValue(
            config.normalStartingCoins!=null,
            "match.normalStartingCoins",
            FallbackRequirementLevel
                .BLOCKS_MECHANIC,
            "Normal pricing needs an explicit starting Coins value"
        )
        requireValue(
            config.normalStartingExp!=null,
            "match.normalStartingExp",
            FallbackRequirementLevel
                .BLOCKS_MECHANIC,
            "Normal pricing needs an explicit starting EXP value"
        )
        requireValue(
            config.goldmineFirstIncomeDelayTicks!=null,
            "goldmine.firstIncomeDelayTicks",
            FallbackRequirementLevel
                .BLOCKS_MECHANIC,
            "Goldmine first-income phase alignment is not recovered"
        )

        requireValue(
            config.troopSpawnCadenceTicks!=null,
            "troop.spawnCadenceTicks",
            FallbackRequirementLevel
                .BLOCKS_MECHANIC,
            "FIFO queue cannot spawn without cadence"
        )
        requireValue(
            config.castleFirstHitDelayTicks!=null,
            "castle.firstHitDelayTicks",
            FallbackRequirementLevel
                .BLOCKS_MECHANIC,
            "ordinary castle attack clock needs first hit delay"
        )
        requireValue(
            config.castleOrdinaryAttackIntervalTicks!=
                null,
            "castle.ordinaryAttackIntervalTicks",
            FallbackRequirementLevel
                .BLOCKS_MECHANIC,
            "ordinary castle attacks need repeat interval"
        )

        listOf(
            "guard.damagePerArrow" to
                (config.guardDamagePerArrow!=null),
            "guard.fireIntervalTicks" to
                (config.guardFireIntervalTicks!=null),
            "guard.rangeBlocks" to
                (config.guardRangeBlocks!=null),
            "guard.targetPriority" to
                (config.guardTargetPriority!=null)
        ).forEach { (key,present) ->
            requireValue(
                present,key,
                FallbackRequirementLevel
                    .BLOCKS_MECHANIC,
                "Castle Guard runtime requires exact/fallback config"
            )
        }

        requireValue(
            config.witchHealAmount!=null &&
                config.witchHealIntervalTicks!=null &&
                config.witchHealRadiusBlocks!=null &&
                config.witchHealMode!=null &&
                config.witchHealTargetCap!=null,
            "witch.heal.*",
            FallbackRequirementLevel.BLOCKS_MECHANIC,
            "Witch Mature heal percentage/interval/radius/cap require explicit values"
        )
        requireValue(
            config.creeperRegenHealthPerSecond!=null,
            "creeper.regenHealthPerSecond",
            FallbackRequirementLevel.BLOCKS_MECHANIC,
            "Creeper regeneration exists but exact rate remains unresolved"
        )
        requireValue(
            config.giantRegenHealthPerSecond!=null,
            "giant.regenHealthPerSecond",
            FallbackRequirementLevel.BLOCKS_MECHANIC,
            "Giant regeneration exact value unresolved"
        )
        requireValue(
            config.giantRunSpeedMultiplier!=null,
            "giant.runSpeedMultiplier",
            FallbackRequirementLevel.BLOCKS_MECHANIC,
            "Giant low-health run multiplier is not direct-official numeric truth"
        )
        requireValue(
            config.iceSlowMovementMultiplier!=null,
            "status.iceSlowMovementMultiplier",
            FallbackRequirementLevel.BLOCKS_MECHANIC,
            "Ice slow exists in live tower combat but its exact movement multiplier is unresolved"
        )
        requireValue(
            config.burnIntervalTicks!=null,
            "status.burnIntervalTicks",
            FallbackRequirementLevel.BLOCKS_MECHANIC,
            "Burn damage exists in live tower combat but its periodic cadence is unresolved"
        )
        requireValue(
            config.slimeMaxShrinkPhaseIndex!=null,
            "slime.maxShrinkPhaseIndex",
            FallbackRequirementLevel.BLOCKS_MECHANIC,
            "2021 confirms shrink-on-kill but not the exact number of continuing phases"
        )
        requireValue(
            config.leachMaxCharge!=null &&
                config.leachChargePerTargetTick!=
                    null,
            "leach.charge.*",
            FallbackRequirementLevel
                .BLOCKS_MECHANIC,
            "Leach death-ray runtime requires explicit charge threshold/gain"
        )

        requireValue(
            config.towerChainTargetPolicy!=null,
            "tower.chainTargetPolicy",
            FallbackRequirementLevel
                .BLOCKS_MECHANIC,
            "Zeus/Turret bounce next-target rule is unresolved and must be an explicit engineering policy"
        )
        requireValue(
            config.towerDefaultTargetPriority!=null,
            "tower.defaultTargetPriority",
            FallbackRequirementLevel
                .BLOCKS_MECHANIC,
            "Default tower target-priority rule is not directly recovered for the Mature build"
        )
        requireValue(
            config.aoeKillAwardsCoins!=null,
            "aoe.killAwardsCoins",
            FallbackRequirementLevel
                .BLOCKS_MECHANIC,
            "AoE potion kill-Coin credit is not recovered and requires an explicit playtest policy"
        )

        return missing
    }
}
