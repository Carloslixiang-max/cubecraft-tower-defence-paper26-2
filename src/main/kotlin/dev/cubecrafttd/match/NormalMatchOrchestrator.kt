package dev.cubecrafttd.match

import dev.cubecrafttd.arena.ArenaContext
import dev.cubecrafttd.castle.GuardRuntime

data class NormalMatchTickReport(
    val phase: MatchPhase,
    val armageddonActivated:
        ArmageddonActivationReport? = null,
    val outcome: MatchOutcome =
        MatchOutcome.Continue
)

fun interface ArmageddonStartPort {
    fun start(
        type: ArmageddonType,
        context: ArenaContext,
        gameTick: Long
    )
}

class NormalMatchOrchestrator(
    private val timing: NormalMatchTiming =
        NormalMatchTiming(),
    private val armageddonType:
        ArmageddonType,
    private val armageddonPort:
        ArmageddonStartPort
) {
    private var armageddonStarted = false

    fun tick(
        context: ArenaContext,
        elapsedTicks: Long,
        guards: Collection<GuardRuntime>,
        timeoutTiePolicy:
            TimeoutTiePolicy
    ): NormalMatchTickReport {
        val red =
            context.castles.getValue(
                dev.cubecrafttd.arena.TeamId.RED
            )
        val blue =
            context.castles.getValue(
                dev.cubecrafttd.arena.TeamId.BLUE
            )

        val immediate =
            MatchOutcomeResolver
                .byCastleDestruction(
                    red,blue
                )
        if (immediate !is
            MatchOutcome.Continue
        ) {
            return NormalMatchTickReport(
                MatchPhase.FINISHED,
                outcome=immediate
            )
        }

        var activation:
            ArmageddonActivationReport? =
            null

        if (
            !armageddonStarted &&
            elapsedTicks >=
                timing.armageddonStartTick
        ) {
            activation =
                ArmageddonService.activate(
                    armageddonType,
                    context.castles,
                    guards
                )
            armageddonPort.start(
                armageddonType,
                context,
                context.gameTick
            )
            armageddonStarted = true
        }

        if (
            elapsedTicks >=
                timing.hardEndTick
        ) {
            return NormalMatchTickReport(
                MatchPhase.FINISHED,
                activation,
                MatchOutcomeResolver
                    .atHardTimeout(
                        red,blue,
                        timeoutTiePolicy
                    )
            )
        }

        return NormalMatchTickReport(
            if (armageddonStarted)
                MatchPhase.ARMAGEDDON
            else
                MatchPhase.PRE_ARMAGEDDON,
            activation
        )
    }

    fun isArmageddonStarted(): Boolean =
        armageddonStarted
}
