package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.castle.*
import dev.cubecrafttd.mob.MobInstanceId
import dev.cubecrafttd.truth.*

object TickAndTruthGateFixture {
    fun run(): List<FixtureResult> {
        val unresolved = TruthField<Long>(
            key = "castle.ordinaryAttackIntervalTicks",
            observedOriginalValue = null,
            status = EvidenceStatus.VIDEO_REQUIRED
        )
        val unresolvedFirst = TruthField<Long>(
            key = "castle.firstHitDelayTicks",
            observedOriginalValue = null,
            status = EvidenceStatus.ENGINEERING_ONLY
        )

        val blockedWithoutFallback = try {
            TruthGate().resolveWithExplicitFallback(unresolved)
            false
        } catch (_: IllegalStateException) {
            true
        }

        val gate = TruthGate(
            EngineeringFallbackConfig(
                mapOf(
                    "castle.ordinaryAttackIntervalTicks" to 40L,
                    "castle.firstHitDelayTicks" to 20L
                )
            )
        )
        val interval = gate.resolveWithExplicitFallback(unresolved)
        val firstDelay = gate.resolveWithExplicitFallback(unresolvedFirst)
        val clock = CastleAttackClock(
            CastleAttackClockConfig(firstDelay, interval)
        )
        val mob = MobInstanceId(44)
        clock.begin(mob, contactTick = 100)

        val before = !clock.consumeHit(mob, 119)
        val first = clock.consumeHit(mob, 120)
        val next = clock.nextHitTick(mob) == 160L
        val second = clock.consumeHit(mob, 160)

        val phaseOrder = mutableListOf<String>()
        val phases = listOf(
            object : ArenaTickPhase {
                override val order = 20
                override val id = "movement"
                override fun tick(context: ArenaContext) { phaseOrder += id }
            },
            object : ArenaTickPhase {
                override val order = 10
                override val id = "queue"
                override fun tick(context: ArenaContext) { phaseOrder += id }
            },
            object : ArenaTickPhase {
                override val order = 30
                override val id = "castle"
                override fun tick(context: ArenaContext) { phaseOrder += id }
            }
        )

        return listOf(
            FixtureResult("truthgate-blocks-unresolved-castle-rate", blockedWithoutFallback),
            FixtureResult(
                "truthgate-fallback-source-is-explicit",
                interval.source == ResolutionSource.ENGINEERING_FALLBACK &&
                    firstDelay.source == ResolutionSource.ENGINEERING_FALLBACK
            ),
            FixtureResult(
                "castle-clock-explicit-first-delay-and-repeat",
                before && first && next && second
            ),
            FixtureResult(
                "arena-tick-phase-order-definition",
                ArenaTickEngine(phases).phaseIds() == listOf("queue","movement","castle")
            )
        )
    }
}
