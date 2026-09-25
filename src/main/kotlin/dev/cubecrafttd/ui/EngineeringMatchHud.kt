package dev.cubecrafttd.ui

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.match.ArmageddonType
import java.util.Locale

data class EngineeringMatchHudSnapshot(
    val team: TeamId,
    val ownCastleHealth: Double,
    val ownCastleMaxHealth: Double,
    val enemyCastleHealth: Double,
    val enemyCastleMaxHealth: Double,
    val coins: Long,
    val exp: Long,
    val elapsedTicks: Long,
    val armageddonType: ArmageddonType,
    val armageddonStarted: Boolean
) {
    init {
        require(ownCastleHealth >= 0.0)
        require(ownCastleMaxHealth > 0.0)
        require(enemyCastleHealth >= 0.0)
        require(enemyCastleMaxHealth > 0.0)
        require(elapsedTicks >= 0L)
    }
}

/**
 * Engineering-playtest HUD only.
 *
 * This projection makes the current runtime observable without claiming that
 * the wording, punctuation, or exact presentation matches original CubeCraft.
 */
object EngineeringMatchHudProjector {
    private const val ARMAGEDDON_START_TICKS =
        25L * 60L * 20L

    fun render(
        snapshot: EngineeringMatchHudSnapshot
    ): String {
        val armageddonText =
            if(snapshot.armageddonStarted) {
                "ARM ${snapshot.armageddonType.name}"
            } else {
                val remaining =
                    (ARMAGEDDON_START_TICKS -
                        snapshot.elapsedTicks)
                        .coerceAtLeast(0L)
                "ARM ${snapshot.armageddonType.name} in " +
                    formatClock(remaining)
            }

        return buildString {
            append("ENG ")
            append(snapshot.team.name)
            append(" | Castle ")
            append(formatHealth(snapshot.ownCastleHealth))
            append("/")
            append(formatHealth(snapshot.ownCastleMaxHealth))
            append(" | Enemy ")
            append(formatHealth(snapshot.enemyCastleHealth))
            append("/")
            append(formatHealth(snapshot.enemyCastleMaxHealth))
            append(" | ")
            append(snapshot.coins)
            append("C ")
            append(snapshot.exp)
            append("XP | ")
            append(formatClock(snapshot.elapsedTicks))
            append(" | ")
            append(armageddonText)
        }
    }

    private fun formatClock(
        ticks: Long
    ): String {
        val totalSeconds =
            (ticks.coerceAtLeast(0L) / 20L)
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(
            Locale.ROOT,
            "%02d:%02d",
            minutes,
            seconds
        )
    }

    private fun formatHealth(
        value: Double
    ): String =
        String.format(
            Locale.ROOT,
            "%.1f",
            value
        )
}
