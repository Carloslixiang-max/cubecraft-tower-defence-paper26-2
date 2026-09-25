package dev.cubecrafttd.testing

import dev.cubecrafttd.mob.WitchHealMode
import dev.cubecrafttd.admin.*
import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.castle.GuardTargetPriorityPolicy
import dev.cubecrafttd.match.ArmageddonType
import dev.cubecrafttd.truth.*

object FallbackAdminFixture {
    fun run(): List<FixtureResult> {
        val empty=
            RuntimeFallbackConfigV1()
        val missing=
            RuntimeFallbackValidator
                .validateForNormal(empty)

        val config=
            RuntimeFallbackConfigV1(
                troopSpawnCadenceTicks=5,
                castleFirstHitDelayTicks=20,
                castleOrdinaryAttackIntervalTicks=40,
                guardDamagePerArrow=5.0,
                guardFireIntervalTicks=20,
                guardRangeBlocks=10.0,
                guardTargetPriority=
                    GuardTargetPriorityPolicy.FIRST,
                witchHealAmount=10.0,
                witchHealIntervalTicks=40,
                witchHealRadiusBlocks=8.0,
                witchHealMode=
                    WitchHealMode.MAX_HEALTH_FRACTION,
                witchHealTargetCap=8,
                creeperRegenHealthPerSecond=5.0,
                giantRegenHealthPerSecond=5.0,
                giantRunSpeedMultiplier=1.2,
                slimeMaxShrinkPhaseIndex=2,
                leachMaxCharge=100.0,
                leachChargePerTargetTick=1.0,
                mobSpeedBlocksPerTick=
                    mapOf(
                        "zombie.l1" to 0.1
                    )
            )
        val engineering=
            config.toEngineeringFallbackConfig()
        val resolved=
            TruthGate(engineering)
                .resolveWithExplicitFallback(
                    TruthField<Long>(
                        "castle.ordinaryAttackIntervalTicks",
                        null,
                        EvidenceStatus.VIDEO_REQUIRED
                    )
                )

        val spawn=
            AdminTestCommandParser.parse(
                listOf(
                    "spawnmob",
                    "zombie","5","blue"
                )
            )
        val tower=
            AdminTestCommandParser.parse(
                listOf(
                    "tower","mage","4",
                    "top","red",
                    "1","2","3"
                )
            )
        val arm=
            AdminTestCommandParser.parse(
                listOf(
                    "armageddon","wither"
                )
            )

        return listOf(
            FixtureResult(
                "fallback-empty-reports-blocking-fields",
                missing.any{
                    it.key==
                        "troop.spawnCadenceTicks"
                } &&
                    missing.any{
                        it.key==
                            "guard.targetPriority"
                    }
            ),
            FixtureResult(
                "fallback-typed-config-adapts-to-truthgate",
                resolved.value==40L &&
                    resolved.source==
                        ResolutionSource
                            .ENGINEERING_FALLBACK
            ),
            FixtureResult(
                "admin-parser-spawnmob",
                (spawn as?
                    AdminTestIntent.SpawnMob)
                    ?.let{
                        it.mobId=="zombie" &&
                        it.level==5 &&
                        it.attackedTeam==
                            TeamId.BLUE
                    }==true
            ),
            FixtureResult(
                "admin-parser-tower",
                (tower as?
                    AdminTestIntent.BuildTower)
                    ?.let{
                        it.towerId=="mage" &&
                        it.level==4 &&
                        it.center.x==1
                    }==true
            ),
            FixtureResult(
                "admin-parser-armageddon",
                (arm as?
                    AdminTestIntent
                        .ForceArmageddon)
                    ?.type==
                    ArmageddonType.WITHER
            )
        )
    }
}
