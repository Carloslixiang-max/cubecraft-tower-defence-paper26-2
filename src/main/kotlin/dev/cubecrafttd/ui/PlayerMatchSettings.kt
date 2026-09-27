package dev.cubecrafttd.ui

enum class ParticleDensitySetting(
    val label: String
) {
    HIGH_500("500/sec"),
    REDUCED_100("100/sec"),
    MINIMUM("minimum");

    fun next(): ParticleDensitySetting =
        when(this) {
            HIGH_500 -> REDUCED_100
            REDUCED_100 -> MINIMUM
            MINIMUM -> HIGH_500
        }
}

data class PlayerMatchSettings(
    val lifetimeWins: Int,
    var autoCentreTowers: Boolean = true,
    var particleDensity:
        ParticleDensitySetting =
        ParticleDensitySetting.HIGH_500,
    var digitalMobHealth: Boolean = false,
    var damageIndicators: Boolean = false,
    var allowInGamePointPurchases: Boolean = false
) {
    init { require(lifetimeWins >= 0) }

    val canDisableAutoCentre: Boolean
        get() = lifetimeWins >= 20

    fun setAutoCentre(enabled: Boolean) {
        if (!enabled && !canDisableAutoCentre) {
            error(
                "Auto-centering disable unlocks after 20 wins"
            )
        }
        autoCentreTowers = enabled
    }

    fun cycleParticleDensity() {
        particleDensity=
            particleDensity.next()
    }
}

data class SettingsMenuModel(
    val autoCentreEnabled: Boolean,
    val autoCentreDisableUnlocked: Boolean,
    val particleDensity:
        ParticleDensitySetting,
    val digitalMobHealth: Boolean,
    val damageIndicators: Boolean,
    val allowInGamePointPurchases: Boolean
)

object SettingsMenuProjector {
    fun project(
        settings: PlayerMatchSettings
    ) = SettingsMenuModel(
        settings.autoCentreTowers,
        settings.canDisableAutoCentre,
        settings.particleDensity,
        settings.digitalMobHealth,
        settings.damageIndicators,
        settings.allowInGamePointPurchases
    )
}
