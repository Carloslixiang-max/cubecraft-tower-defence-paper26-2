package dev.cubecrafttd.match

import dev.cubecrafttd.arena.ArenaContext
import dev.cubecrafttd.castle.GuardRuntime

enum class ArmageddonSelectionSource {
    EXPLICIT_VOTE_RESULT,
    RANDOM_RESULT_ALREADY_RESOLVED,
    ENGINEERING_TEST
}

data class ResolvedArmageddonSelection(
    val type: ArmageddonType,
    val source: ArmageddonSelectionSource
)

/**
 * Owns match elapsed time without deciding how Armageddon voting was won.
 *
 * The original lobby offered Random / Wither / Lightning / Horde. This class
 * accepts the already-resolved final type and only applies the verified
 * 25-minute Armageddon / 40-minute hard-end timing.
 */
class NormalMatchClockRuntime(
    startGameTick: Long,
    selection: ResolvedArmageddonSelection,
    tiePolicy: TimeoutTiePolicy,
    armageddonPort: ArmageddonStartPort,
    timing: NormalMatchTiming =
        NormalMatchTiming()
) {
    private val startTick=startGameTick
    private val selected=selection
    private val timeoutTiePolicy=tiePolicy
    private val orchestrator=
        NormalMatchOrchestrator(
            timing=timing,
            armageddonType=
                selection.type,
            armageddonPort=
                armageddonPort
        )

    init {
        require(startGameTick>=0L)
    }

    fun tick(
        context: ArenaContext,
        guards: Collection<GuardRuntime>
    ): NormalMatchTickReport {
        require(
            context.gameTick>=startTick
        ) {
            "Arena gameTick moved before match start"
        }
        return orchestrator.tick(
            context=context,
            elapsedTicks=
                context.gameTick-startTick,
            guards=guards,
            timeoutTiePolicy=
                timeoutTiePolicy
        )
    }

    fun elapsedTicks(
        context: ArenaContext
    ): Long =
        context.gameTick-startTick

    fun selection():
        ResolvedArmageddonSelection =
        selected

    fun isArmageddonStarted():
        Boolean =
        orchestrator
            .isArmageddonStarted()
}
