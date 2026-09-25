package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.ArenaDeterministicRng
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.truth.*
import java.util.UUID

object DeterministicRngAndTowerClockFixture {
    fun run(): List<FixtureResult> {
        val rngA = ArenaDeterministicRng(123456789L)
        val rngB = ArenaDeterministicRng(123456789L)
        val sequenceA = List(8) { rngA.nextInt(1000) }
        val sequenceB = List(8) { rngB.nextInt(1000) }

        val candidates = (1..4).map { i ->
            TowerTargetCandidate(
                UUID.fromString(
                    "00000000-0000-0000-0000-%012d".format(i)
                ),
                routeProgress = i.toDouble(),
                combatProfile =
                    RecommendedMatureMobCombatProfiles.get("zombie"),
                distanceBlocks = 5.0,
                lineOfSight = true
            )
        }
        val query = TowerTargetQuery(
            TowerAttackCapability(
                "archer",
                setOf(TargetLayer.GROUND,TargetLayer.AIR),
                setOf(DamageKind.PHYSICAL)
            ),
            15.0,
            true,
            TargetPriorityPolicy.RANDOM
        )
        val random1 = TowerTargetSelector.select(
            candidates, query, ArenaDeterministicRng(77L)
        )
        val random2 = TowerTargetSelector.select(
            candidates, query, ArenaDeterministicRng(77L)
        )

        val turretStage =
            RecommendedMatureTowerDefinitions.get("turret")
                .stage(TowerPathOption.TOP,3)
        val turretTiming =
            TowerAttackTimingResolver.resolve(turretStage)
        val clock = TowerAttackClock(
            turretTiming,
            firstReadyTick = 100
        )

        val before = !clock.consume(99)
        val at = clock.consume(100)
        val next = clock.nextAttackTick == 104L

        val archerStage =
            RecommendedMatureTowerDefinitions.get("archer")
                .stage(TowerPathOption.TOP,4)
        val archerBlocked = try {
            TowerAttackTimingResolver.resolve(archerStage)
            false
        } catch (_: IllegalStateException) {
            true
        }
        val archerFallback =
            TowerAttackTimingResolver.resolve(
                archerStage,
                ResolvedTruth(
                    1.0,
                    ResolutionSource.ENGINEERING_FALLBACK
                )
            )

        return listOf(
            FixtureResult(
                "arena-rng-reproducible",
                sequenceA == sequenceB
            ),
            FixtureResult(
                "random-target-reproducible-with-arena-rng",
                random1?.entityUuid == random2?.entityUuid
            ),
            FixtureResult(
                "turret-0.2s-clock-four-ticks",
                before && at && next &&
                    turretTiming.intervalTicks == 4L
            ),
            FixtureResult(
                "archer-mature-unresolved-rate-hard-gate",
                archerBlocked
            ),
            FixtureResult(
                "archer-explicit-fallback-tagged",
                archerFallback.intervalTicks == 20L &&
                    archerFallback.source ==
                        ResolutionSource.ENGINEERING_FALLBACK
            )
        )
    }
}
