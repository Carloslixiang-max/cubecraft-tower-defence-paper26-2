package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.*
import dev.cubecrafttd.truth.*
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.security.MessageDigest

data class EngineeringPlaytestSetupReport(
    val profileId: String,
    val worldName: String,
    val origin: BlockPos,
    val redRoute: String,
    val blueRoute: String,
    val configBackup: String?,
    val restartRequired: Boolean = true
)

class BukkitEngineeringPlaytestConfigurator(
    private val plugin: JavaPlugin
) {
    fun apply(
        player: Player
    ): EngineeringPlaytestSetupReport {
        val farm=
            File(
                plugin.dataFolder,
                "maps/ImprovedFarm.schem"
            )
        check(farm.isFile) {
            "Missing " + farm.path
        }

        val bytes=farm.readBytes()
        val sha=sha256(bytes)
        check(
            sha ==
                PaperGameplayReadinessService
                    .EXPECTED_FARM_SHA256
        ) {
            "ImprovedFarm.schem SHA-256 mismatch: " + sha
        }

        val origin=
            BlockPos(
                player.location.blockX,
                player.location.blockY + 8,
                player.location.blockZ
            )

        val runtime=
            FarmPaperMapBinder()
                .prepare(
                    bytes,
                    player.world.uid,
                    origin
                )
                .runtime

        fun routeFor(
            team: TeamId
        ): String =
            runtime.routeOwnerById
                .filterValues { it==team }
                .keys
                .sorted()
                .firstOrNull()
                ?: error(
                    "No route found for " + team
                )

        fun playerSpawn(
            team: TeamId
        ): Vec3 {
            val mob=
                runtime.teamSpawns
                    .getValue(team)
                    .mobSpawn
                    ?: error(
                        "No mob spawn found for " + team
                    )
            return Vec3(
                mob.x,
                mob.y + 4.0,
                mob.z
            )
        }

        fun guardAnchors(
            team: TeamId
        ): List<Vec3> {
            val terminal=
                runtime.terminalCrossSections[
                    team
                ] ?: error(
                    "No terminal cross-section for " + team
                )
            val p=terminal.centerlineEndpoint
            val y=p.y + 2.0
            return when(
                terminal.headingIntoCastle
            ) {
                CardinalHeading.POS_Z ->
                    listOf(
                        Vec3(p.x-3.0,y,p.z+2.0),
                        Vec3(p.x+3.0,y,p.z+2.0)
                    )
                CardinalHeading.NEG_Z ->
                    listOf(
                        Vec3(p.x-3.0,y,p.z-2.0),
                        Vec3(p.x+3.0,y,p.z-2.0)
                    )
                CardinalHeading.POS_X ->
                    listOf(
                        Vec3(p.x+2.0,y,p.z-3.0),
                        Vec3(p.x+2.0,y,p.z+3.0)
                    )
                CardinalHeading.NEG_X ->
                    listOf(
                        Vec3(p.x-2.0,y,p.z-3.0),
                        Vec3(p.x-2.0,y,p.z+3.0)
                    )
            }
        }

        val redRoute=routeFor(TeamId.RED)
        val blueRoute=routeFor(TeamId.BLUE)
        val redSpawn=playerSpawn(TeamId.RED)
        val blueSpawn=playerSpawn(TeamId.BLUE)
        val redGuards=guardAnchors(TeamId.RED)
        val blueGuards=guardAnchors(TeamId.BLUE)
        val backup=backupStrictConfig()

        val c=plugin.config
        c.set(
            "engineering-playtest.enabled",
            true
        )
        c.set(
            "engineering-playtest.profile",
            EngineeringPlaytestProfile.ID
        )
        c.set(
            "engineering-playtest.provenance",
            "EXPLICIT_ENGINEERING_FALLBACK_NOT_ORIGINAL_CUBECRAFT"
        )
        c.set(
            "map-binding.world",
            player.world.name
        )
        c.set("map-binding.origin.x",origin.x)
        c.set("map-binding.origin.y",origin.y)
        c.set("map-binding.origin.z",origin.z)
        c.set(
            "map-binding.route-assignment.red",
            redRoute
        )
        c.set(
            "map-binding.route-assignment.blue",
            blueRoute
        )
        c.set(
            "map-binding.guard-anchors.red",
            redGuards.map(::serialize)
        )
        c.set(
            "map-binding.guard-anchors.blue",
            blueGuards.map(::serialize)
        )
        c.set(
            "map-binding.player-spawn.red",
            serialize(redSpawn)
        )
        c.set(
            "map-binding.player-spawn.blue",
            serialize(blueSpawn)
        )
        c.set(
            "map-binding.allow-farm-paste",
            true
        )

        writeFallback(
            c,
            EngineeringPlaytestProfile.create()
        )
        plugin.saveConfig()

        return EngineeringPlaytestSetupReport(
            profileId=
                EngineeringPlaytestProfile.ID,
            worldName=
                player.world.name,
            origin=origin,
            redRoute=redRoute,
            blueRoute=blueRoute,
            configBackup=
                backup?.name
        )
    }

    private fun backupStrictConfig():
        File? {
        val source=
            File(
                plugin.dataFolder,
                "config.yml"
            )
        if(!source.isFile) return null
        val target=
            File(
                plugin.dataFolder,
                "config.before-engineering-playtest.yml"
            )
        if(!target.exists()) {
            source.copyTo(
                target,
                overwrite=false
            )
        }
        return target
    }

    private fun serialize(
        v: Vec3
    ): String =
        v.x.toString() + "," +
            v.y.toString() + "," +
            v.z.toString()

    private fun writeFallback(
        c: FileConfiguration,
        f: RuntimeFallbackConfigV1
    ) {
        fun set(
            path: String,
            value: Any?
        )=c.set(
            "runtime-fallback." + path,
            value
        )

        set("match.normal-starting-coins",f.normalStartingCoins)
        set("match.normal-starting-exp",f.normalStartingExp)
        set("goldmine.first-income-delay-ticks",f.goldmineFirstIncomeDelayTicks)
        set("troop-spawn-cadence-ticks",f.troopSpawnCadenceTicks)
        f.mobSpeedBlocksPerTick.forEach { (key,value) ->
            set("mob-speed-blocks-per-tick." + key,value)
        }

        set("castle.first-hit-delay-ticks",f.castleFirstHitDelayTicks)
        set("castle.repeat-interval-ticks",f.castleOrdinaryAttackIntervalTicks)
        set("guard.damage-per-arrow",f.guardDamagePerArrow)
        set("guard.fire-interval-ticks",f.guardFireIntervalTicks)
        set("guard.range-blocks",f.guardRangeBlocks)
        set("guard.target-priority",f.guardTargetPriority?.name)

        set("witch.heal-amount",f.witchHealAmount)
        set("witch.heal-mode",f.witchHealMode?.name)
        set("witch.heal-target-cap",f.witchHealTargetCap)
        set("witch.heal-interval-ticks",f.witchHealIntervalTicks)
        set("witch.heal-radius-blocks",f.witchHealRadiusBlocks)

        set("creeper.regen-health-per-second",f.creeperRegenHealthPerSecond)
        set("giant.regen-health-per-second",f.giantRegenHealthPerSecond)
        set("giant.run-speed-multiplier",f.giantRunSpeedMultiplier)
        set("slime.max-shrink-phase-index",f.slimeMaxShrinkPhaseIndex)
        set("leach.max-charge",f.leachMaxCharge)
        set("leach.charge-per-target-tick",f.leachChargePerTargetTick)

        f.towerAttackIntervalSeconds.forEach { (key,value) ->
            set("tower-attack-interval-seconds." + key,value)
        }
        f.towerSummonActiveCounts.forEach { (key,value) ->
            set("tower-summon-active-counts." + key,value)
        }
        set("tower-chain-target-policy",f.towerChainTargetPolicy?.name)
        set("tower-default-target-priority",f.towerDefaultTargetPriority?.name)
        f.towerRequiresLineOfSight.forEach { (key,value) ->
            set("tower-requires-line-of-sight." + key,value)
        }
        f.mobKillCoins.forEach { (key,value) ->
            set("mob-kill-coins." + key,value)
        }

        set("armageddon.lightning.first-strike-delay-ticks",f.lightningFirstStrikeDelayTicks)
        set("armageddon.lightning.strike-interval-ticks",f.lightningStrikeIntervalTicks)
        set("armageddon.lightning.towers-per-team-per-strike",f.lightningTowersPerTeamPerStrike)

        set("armageddon.wither.speed-blocks-per-tick",f.witherSpeedBlocksPerTick)
        set("armageddon.wither.regeneration-health-per-second",f.witherRegenerationHealthPerSecond)
        set("armageddon.wither.first-tower-skull-delay-ticks",f.witherFirstTowerSkullDelayTicks)
        set("armageddon.wither.tower-skull-interval-ticks",f.witherTowerSkullIntervalTicks)
        set("armageddon.wither.tower-skull-range-blocks",f.witherTowerSkullRangeBlocks)
        set("armageddon.wither.tower-target-policy",f.witherTowerTargetPolicy?.name)
        set("armageddon.wither.first-trail-skeleton-delay-ticks",f.witherFirstTrailSkeletonDelayTicks)
        set("armageddon.wither.trail-skeleton-interval-ticks",f.witherTrailSkeletonIntervalTicks)
        set("armageddon.wither.trail-skeletons-per-event",f.witherTrailSkeletonsPerEvent)
        set("armageddon.wither.trail-skeleton-level",f.witherTrailSkeletonLevel)

        set("armageddon.horde.first-wave-delay-ticks",f.hordeFirstWaveDelayTicks)
        set("armageddon.horde.wave-interval-ticks",f.hordeWaveIntervalTicks)
        set(
            "armageddon.horde.waves",
            f.hordeWavePlan.map {
                mapOf(
                    "mob" to it.mobId,
                    "level" to it.level,
                    "quantity-per-team" to
                        it.quantityPerTeam
                )
            }
        )
    }

    private fun sha256(
        bytes: ByteArray
    ): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") {
                "%02x".format(it)
            }
}
