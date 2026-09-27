package dev.cubecrafttd.ui

import java.util.Locale

/**
 * Semantic projection helpers for the recovered 2017 Settings options.
 *
 * The settings themselves are historical evidence. Exact original text
 * styling, colours, offsets and indicator lifetime remain unrecovered, so
 * Bukkit presentation keeps those details explicitly Engineering-only.
 */
object MobCombatVisualProjection {
    fun digitalHealthEnabled(
        settings: PlayerMatchSettings
    ): Boolean =
        settings.digitalMobHealth

    fun damageIndicatorsEnabled(
        settings: PlayerMatchSettings
    ): Boolean =
        settings.damageIndicators

    fun damageAmount(
        beforeHealth: Double,
        afterHealth: Double
    ): Double {
        require(beforeHealth >= 0.0)
        require(afterHealth >= 0.0)
        return (
            beforeHealth - afterHealth
        ).coerceAtLeast(0.0)
    }

    fun healthText(
        health: Double,
        maxHealth: Double
    ): String {
        require(health >= 0.0)
        require(maxHealth > 0.0)
        return "${format(health)} / ${format(maxHealth)}"
    }

    fun damageText(
        amount: Double
    ): String {
        require(amount >= 0.0)
        return "-${format(amount)}"
    }

    private fun format(
        value: Double
    ): String {
        val rounded=
            kotlin.math.round(value)
        return if(
            kotlin.math.abs(
                value-rounded
            ) < 1.0e-9
        ) {
            rounded.toLong()
                .toString()
        } else {
            String.format(
                Locale.ROOT,
                "%.1f",
                value
            )
        }
    }
}
