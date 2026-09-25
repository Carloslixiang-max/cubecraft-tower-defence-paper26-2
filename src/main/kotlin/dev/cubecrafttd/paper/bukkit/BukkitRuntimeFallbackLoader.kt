package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.castle.GuardTargetPriorityPolicy
import dev.cubecrafttd.truth.RuntimeFallbackConfigV1
import dev.cubecrafttd.truth.ArmageddonHordeFallbackWave
import dev.cubecrafttd.mob.WitchHealMode
import dev.cubecrafttd.match.WitherTowerTargetPolicy
import dev.cubecrafttd.tower.TowerChainTargetPolicy
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration

object BukkitRuntimeFallbackLoader {
    fun load(config: FileConfiguration): RuntimeFallbackConfigV1 =
        RuntimeFallbackConfigV1(
            normalStartingCoins = longOrNull(config,"runtime-fallback.match.normal-starting-coins"),
            normalStartingExp = longOrNull(config,"runtime-fallback.match.normal-starting-exp"),
            goldmineFirstIncomeDelayTicks = longOrNull(config,"runtime-fallback.goldmine.first-income-delay-ticks"),
            troopSpawnCadenceTicks = longOrNull(config,"runtime-fallback.troop-spawn-cadence-ticks"),
            mobSpeedBlocksPerTick = doubles(config.getConfigurationSection("runtime-fallback.mob-speed-blocks-per-tick")),
            castleFirstHitDelayTicks = longOrNull(config,"runtime-fallback.castle.first-hit-delay-ticks"),
            castleOrdinaryAttackIntervalTicks = longOrNull(config,"runtime-fallback.castle.repeat-interval-ticks"),
            guardDamagePerArrow = doubleOrNull(config,"runtime-fallback.guard.damage-per-arrow"),
            guardFireIntervalTicks = longOrNull(config,"runtime-fallback.guard.fire-interval-ticks"),
            guardRangeBlocks = doubleOrNull(config,"runtime-fallback.guard.range-blocks"),
            guardTargetPriority = stringOrNull(config,"runtime-fallback.guard.target-priority")
                ?.let { GuardTargetPriorityPolicy.valueOf(it.uppercase()) },
            witchHealAmount = doubleOrNull(config,"runtime-fallback.witch.heal-amount"),
            witchHealMode = stringOrNull(config,"runtime-fallback.witch.heal-mode")
                ?.let { WitchHealMode.valueOf(it.uppercase()) },
            witchHealTargetCap = intOrNull(config,"runtime-fallback.witch.heal-target-cap"),
            witchHealIntervalTicks = longOrNull(config,"runtime-fallback.witch.heal-interval-ticks"),
            witchHealRadiusBlocks = doubleOrNull(config,"runtime-fallback.witch.heal-radius-blocks"),
            creeperRegenHealthPerSecond = doubleOrNull(config,"runtime-fallback.creeper.regen-health-per-second"),
            giantRegenHealthPerSecond = doubleOrNull(config,"runtime-fallback.giant.regen-health-per-second"),
            giantRunSpeedMultiplier = doubleOrNull(config,"runtime-fallback.giant.run-speed-multiplier"),
            slimeMaxShrinkPhaseIndex = intOrNull(config,"runtime-fallback.slime.max-shrink-phase-index"),
            leachMaxCharge = doubleOrNull(config,"runtime-fallback.leach.max-charge"),
            leachChargePerTargetTick = doubleOrNull(config,"runtime-fallback.leach.charge-per-target-tick"),
            towerAttackIntervalSeconds = doubles(config.getConfigurationSection("runtime-fallback.tower-attack-interval-seconds")),
            towerSummonActiveCounts = ints(config.getConfigurationSection("runtime-fallback.tower-summon-active-counts")),
            towerChainTargetPolicy = stringOrNull(config,"runtime-fallback.tower-chain-target-policy")
                ?.let { TowerChainTargetPolicy.valueOf(it.uppercase()) },
            towerDefaultTargetPriority = stringOrNull(config,"runtime-fallback.tower-default-target-priority")
                ?.let { TargetPriorityPolicy.valueOf(it.uppercase()) },
            towerRequiresLineOfSight = booleans(config.getConfigurationSection("runtime-fallback.tower-requires-line-of-sight")),
            mobKillCoins = longs(config.getConfigurationSection("runtime-fallback.mob-kill-coins")),
            weaponAbilityRequiredHits = ints(config.getConfigurationSection("runtime-fallback.weapon-ability-required-hits")),
            aoeNumericFallbacks = numbers(config.getConfigurationSection("runtime-fallback.aoe")),

            lightningFirstStrikeDelayTicks = longOrNull(config,"runtime-fallback.armageddon.lightning.first-strike-delay-ticks"),
            lightningStrikeIntervalTicks = longOrNull(config,"runtime-fallback.armageddon.lightning.strike-interval-ticks"),
            lightningTowersPerTeamPerStrike = intOrNull(config,"runtime-fallback.armageddon.lightning.towers-per-team-per-strike"),

            witherSpeedBlocksPerTick = doubleOrNull(config,"runtime-fallback.armageddon.wither.speed-blocks-per-tick"),
            witherRegenerationHealthPerSecond = doubleOrNull(config,"runtime-fallback.armageddon.wither.regeneration-health-per-second"),

            witherFirstTowerSkullDelayTicks = longOrNull(config,"runtime-fallback.armageddon.wither.first-tower-skull-delay-ticks"),
            witherTowerSkullIntervalTicks = longOrNull(config,"runtime-fallback.armageddon.wither.tower-skull-interval-ticks"),
            witherTowerSkullRangeBlocks = doubleOrNull(config,"runtime-fallback.armageddon.wither.tower-skull-range-blocks"),
            witherTowerTargetPolicy = stringOrNull(config,"runtime-fallback.armageddon.wither.tower-target-policy")
                ?.let { WitherTowerTargetPolicy.valueOf(it.uppercase()) },

            witherFirstTrailSkeletonDelayTicks = longOrNull(config,"runtime-fallback.armageddon.wither.first-trail-skeleton-delay-ticks"),
            witherTrailSkeletonIntervalTicks = longOrNull(config,"runtime-fallback.armageddon.wither.trail-skeleton-interval-ticks"),
            witherTrailSkeletonsPerEvent = intOrNull(config,"runtime-fallback.armageddon.wither.trail-skeletons-per-event"),
            witherTrailSkeletonLevel = intOrNull(config,"runtime-fallback.armageddon.wither.trail-skeleton-level"),

            hordeFirstWaveDelayTicks = longOrNull(config,"runtime-fallback.armageddon.horde.first-wave-delay-ticks"),
            hordeWaveIntervalTicks = longOrNull(config,"runtime-fallback.armageddon.horde.wave-interval-ticks"),
            hordeWavePlan = config
                .getMapList("runtime-fallback.armageddon.horde.waves")
                .mapIndexed { index, row ->
                    val mob=(row["mob"] as? String)
                        ?.takeIf { it.isNotBlank() }
                        ?: error("Horde wave[$index].mob is missing")
                    val level=(row["level"] as? Number)
                        ?.toInt()
                        ?: error("Horde wave[$index].level is missing")
                    val quantity=(row["quantity-per-team"] as? Number)
                        ?.toInt()
                        ?: error("Horde wave[$index].quantity-per-team is missing")
                    ArmageddonHordeFallbackWave(
                        mob,level,quantity
                    )
                }
        )

    private fun stringOrNull(c:FileConfiguration,p:String):String? =
        if(c.isSet(p)) c.getString(p) else null
    private fun longOrNull(c:FileConfiguration,p:String):Long? =
        if(c.isSet(p) && c.get(p) is Number) c.getLong(p) else null
    private fun doubleOrNull(c:FileConfiguration,p:String):Double? =
        if(c.isSet(p) && c.get(p) is Number) c.getDouble(p) else null

    private fun intOrNull(c:FileConfiguration,p:String):Int? =
        if(c.isSet(p) && c.get(p) is Number) c.getInt(p) else null

    private fun doubles(s:ConfigurationSection?):Map<String,Double> =
        numericEntries(s).mapValues { it.value.toDouble() }

    private fun longs(s:ConfigurationSection?):Map<String,Long> =
        numericEntries(s).mapValues { it.value.toLong() }

    private fun ints(s:ConfigurationSection?):Map<String,Int> =
        numericEntries(s).mapValues { it.value.toInt() }

    private fun booleans(
        s: ConfigurationSection?
    ): Map<String,Boolean> =
        s?.getKeys(true)
            ?.asSequence()
            ?.filter { !s.isConfigurationSection(it) }
            ?.mapNotNull { key ->
                (s.get(key) as? Boolean)
                    ?.let { key to it }
            }
            ?.toMap()
            ?: emptyMap()

    private fun numericEntries(
        s: ConfigurationSection?
    ): Map<String,Number> =
        s?.getKeys(true)
            ?.asSequence()
            ?.filter { !s.isConfigurationSection(it) }
            ?.mapNotNull { key ->
                (s.get(key) as? Number)
                    ?.let { key to it }
            }
            ?.toMap()
            ?: emptyMap()
    private fun numbers(s:ConfigurationSection?):Map<String,Number> =
        s?.getKeys(true)?.filter { !s.isConfigurationSection(it) }
            ?.mapNotNull { key -> (s.get(key) as? Number)?.let { key to it } }
            ?.toMap() ?: emptyMap()
}
