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

/**
 * Runtime availability is deliberately separate from the historical Settings
 * surface. The 2017 changelog proves that an in-game purchase preference
 * existed, but the exact purchasable catalogue / profile-point transaction
 * backend has not been recovered in this recreation yet.
 */
object SettingsRuntimeCapabilities {
    const val IN_GAME_POINT_PURCHASES:
        Boolean = false
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
    val allowInGamePointPurchases: Boolean,
    val inGamePointPurchasesAvailable:
        Boolean
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
        settings.allowInGamePointPurchases,
        SettingsRuntimeCapabilities
            .IN_GAME_POINT_PURCHASES
    )
}
