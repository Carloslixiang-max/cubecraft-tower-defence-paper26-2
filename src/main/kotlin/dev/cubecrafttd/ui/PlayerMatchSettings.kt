package dev.cubecrafttd.ui

data class PlayerMatchSettings(
    val lifetimeWins: Int,
    var autoCentreTowers: Boolean = true,
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
}

data class SettingsMenuModel(
    val autoCentreEnabled: Boolean,
    val autoCentreDisableUnlocked: Boolean,
    val allowInGamePointPurchases: Boolean
)

object SettingsMenuProjector {
    fun project(
        settings: PlayerMatchSettings
    ) = SettingsMenuModel(
        settings.autoCentreTowers,
        settings.canDisableAutoCentre,
        settings.allowInGamePointPurchases
    )
}
