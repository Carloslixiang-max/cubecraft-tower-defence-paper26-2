package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import java.util.UUID

private class SequenceNanoTimeSource(
    values: List<Long>
) : ArenaNanoTimeSource {
    private val values=
        ArrayDeque(values)

    override fun nowNanos(): Long =
        values.removeFirst()
}

object ArenaTickProfilerFixture {
    fun run(): List<FixtureResult> {
        val context=
            ArenaContext(
                ArenaId("profiler"),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000045001"
                ),
                TestingMapFactory.minimal()
            ).also {
                it.state=
                    ArenaState.RUNNING
            }

        val phaseA=
            object : ArenaTickPhase {
                override val order=10
                override val id="a"
                override fun tick(
                    context: ArenaContext
                ) = Unit
            }
        val phaseB=
            object : ArenaTickPhase {
                override val order=20
                override val id="b"
                override fun tick(
                    context: ArenaContext
                ) = Unit
            }

        val profiler=
            ArenaTickProfiler()
        val engine=
            ArenaTickEngine(
                listOf(
                    phaseA,phaseB
                ),
                nanoTimeSource=
                    SequenceNanoTimeSource(
                        listOf(
                            0L,
                            10L,15L,
                            20L,28L,
                            60_000_000L
                        )
                    ),
                profiler=profiler
            )

        engine.tick(context)
        val snapshot=
            engine.profileSnapshot()

        val timingPass=
            snapshot.phases["a"]
                ?.let {
                    it.calls==1L &&
                        it.lastNanos==5L &&
                        it.maxNanos==5L &&
                        it.averageNanos==5L
                } == true &&
                snapshot.phases["b"]
                    ?.let {
                        it.calls==1L &&
                            it.lastNanos==8L &&
                            it.maxNanos==8L
                    } == true

        val tickPass=
            snapshot.ticks==1L &&
                snapshot.lastNanos==
                    60_000_000L &&
                snapshot.averageNanos==
                    60_000_000L &&
                snapshot.maxNanos==
                    60_000_000L &&
                snapshot.slowTicksOver50ms==
                    1L

        engine.resetProfile()
        val reset=
            engine.profileSnapshot()

        return listOf(
            FixtureResult(
                "arena-profiler-records-phase-timing",
                timingPass
            ),
            FixtureResult(
                "arena-profiler-records-slow-tick-budget",
                tickPass
            ),
            FixtureResult(
                "arena-profiler-reset-clears-counters",
                reset.ticks==0L &&
                    reset.phases.isEmpty() &&
                    reset.slowTicksOver50ms==
                        0L
            )
        )
    }
}
