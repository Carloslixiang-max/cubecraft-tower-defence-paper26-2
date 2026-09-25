package dev.cubecrafttd.match

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.castle.*

enum class MatchPhase {
    PRE_ARMAGEDDON,
    ARMAGEDDON,
    FINISHED
}

enum class ArmageddonType {
    WITHER,
    LIGHTNING,
    HORDE
}

data class NormalMatchTiming(
    val armageddonStartTick: Long =
        25L * 60L * 20L,
    val hardEndTick: Long =
        40L * 60L * 20L
) {
    init {
        require(armageddonStartTick > 0L)
        require(hardEndTick > armageddonStartTick)
    }

    val armageddonDurationTicks: Long
        get() = hardEndTick - armageddonStartTick

    fun phaseAt(elapsedTicks: Long): MatchPhase =
        when {
            elapsedTicks >= hardEndTick ->
                MatchPhase.FINISHED
            elapsedTicks >= armageddonStartTick ->
                MatchPhase.ARMAGEDDON
            else -> MatchPhase.PRE_ARMAGEDDON
        }
}

data class ArmageddonActivationReport(
    val type: ArmageddonType,
    val castleHealthAfter: Map<TeamId,Double>,
    val disabledGuards: Int
)

object ArmageddonService {
    fun activate(
        type: ArmageddonType,
        castles: Map<TeamId,CastleRuntime>,
        guards: Collection<GuardRuntime>
    ): ArmageddonActivationReport {
        castles.values.forEach { castle ->
            val cap = castle.maxHealth * 0.25
            if (castle.health > cap) {
                castle.health = cap
            }
        }

        var disabled = 0
        guards.forEach { guard ->
            if (guard.lifecycle ==
                GuardLifecycleState.ACTIVE
            ) {
                guard.lifecycle =
                    GuardLifecycleState.DISABLED_ARMAGEDDON
                disabled++
            }
        }

        return ArmageddonActivationReport(
            type,
            castles.mapValues { it.value.health },
            disabled
        )
    }
}

enum class TimeoutTiePolicy {
    DRAW,
    BOTH_WIN,
    ENGINEERING_CUSTOM
}

sealed interface MatchOutcome {
    data class Winner(
        val team: TeamId,
        val reason: String
    ) : MatchOutcome
    data class Tie(
        val policy: TimeoutTiePolicy,
        val reason: String
    ) : MatchOutcome
    data object Continue : MatchOutcome
}

object MatchOutcomeResolver {
    fun byCastleDestruction(
        red: CastleRuntime,
        blue: CastleRuntime
    ): MatchOutcome {
        val redDead = red.state == CastleState.DESTROYED
        val blueDead = blue.state == CastleState.DESTROYED
        return when {
            redDead && !blueDead ->
                MatchOutcome.Winner(
                    TeamId.BLUE,
                    "red_castle_destroyed"
                )
            blueDead && !redDead ->
                MatchOutcome.Winner(
                    TeamId.RED,
                    "blue_castle_destroyed"
                )
            redDead && blueDead ->
                MatchOutcome.Tie(
                    TimeoutTiePolicy.ENGINEERING_CUSTOM,
                    "simultaneous_castle_destruction_policy_required"
                )
            else -> MatchOutcome.Continue
        }
    }

    fun atHardTimeout(
        red: CastleRuntime,
        blue: CastleRuntime,
        tiePolicy: TimeoutTiePolicy
    ): MatchOutcome = when {
        red.health > blue.health ->
            MatchOutcome.Winner(
                TeamId.RED,
                "higher_castle_health_at_timeout"
            )
        blue.health > red.health ->
            MatchOutcome.Winner(
                TeamId.BLUE,
                "higher_castle_health_at_timeout"
            )
        else ->
            MatchOutcome.Tie(
                tiePolicy,
                "equal_castle_health_at_timeout"
            )
    }
}
