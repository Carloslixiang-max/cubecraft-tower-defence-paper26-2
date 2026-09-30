package dev.cubecrafttd.ui

/**
 * Official 2021 Screenshot_173: torch at zero-based slot 11, green enabled
 * name, two gray description lines and gold click instruction. Disabled text
 * and the locked instruction are runtime projections, not recovered screenshots.
 * Source: https://www.cubecraft.net/attachments/screenshot_173-png.184956/
 */
object HistoricalSettingsControls {
    fun autoCentre(
        model: SettingsMenuModel,
        actionId: String
    ): MenuSlot = MenuSlot(
        slot = 11,
        actionId = actionId,
        evidenceStatus = if (model.autoCentreEnabled && model.autoCentreDisableUnlocked)
            UiEvidenceStatus.MATURE_DIRECT else UiEvidenceStatus.MATURE_CONTEXT,
        displayName = "Auto centre towers - " +
            if (model.autoCentreEnabled) "ENABLED" else "DISABLED",
        iconHint = "torch",
        lore = listOf(
            "Automatically places your tower",
            "in the centre of the selected area",
            when {
                !model.autoCentreEnabled -> "Click to enable"
                model.autoCentreDisableUnlocked -> "Click to disable"
                else -> "Disable locked: requires ${LifetimeWinProgress.AUTO_CENTRE_DISABLE_WINS} wins"
            }
        ),
        textStyle = MenuTextStyle(
            if (model.autoCentreEnabled) MenuTextColor.GREEN else MenuTextColor.RED,
            mapOf(0 to MenuTextColor.GRAY, 1 to MenuTextColor.GRAY, 2 to MenuTextColor.GOLD)
        )
    )
}
