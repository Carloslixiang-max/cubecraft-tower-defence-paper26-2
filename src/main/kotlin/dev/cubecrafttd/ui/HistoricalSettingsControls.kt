package dev.cubecrafttd.ui

/**
 * Official 2021 Screenshot_173: torch at zero-based slot 11, green enabled
 * name, two gray description lines and gold click instruction. Disabled text
 * and the locked instruction are runtime projections, not recovered screenshots.
 * Source: https://www.cubecraft.net/attachments/screenshot_173-png.184956/
 */
object HistoricalSettingsControls {
    // These item positions are visible, but their names/actions are not exposed by the hover.
    fun unresolvedVisuals(): List<MenuSlot> = listOf(
        MenuSlot(4, "noop:settings:2021-powder-unresolved", UiEvidenceStatus.MATURE_CONTEXT,
            "2021 control (action unresolved)", "blaze-powder"),
        MenuSlot(10, "noop:settings:2021-feather-unresolved", UiEvidenceStatus.MATURE_CONTEXT,
            "2021 control (action unresolved)", "feather"),
        MenuSlot(19, "noop:settings:2021-red-pane-unresolved", UiEvidenceStatus.MATURE_CONTEXT,
            "2021 pane (action unresolved)", "glass-pane-red"),
        MenuSlot(20, "noop:settings:2021-green-pane-unresolved", UiEvidenceStatus.MATURE_CONTEXT,
            "2021 pane (action unresolved)", "glass-pane-lime")
    )

    fun inventoryLayoutArrow(): MenuSlot = MenuSlot(
        8, "noop:hotbar:2021-arrow-unresolved", UiEvidenceStatus.MATURE_CONTEXT,
        "2021 control (action unresolved)", "arrow")

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
            mapOf(0 to MenuTextColor.GRAY, 1 to MenuTextColor.GRAY, 2 to MenuTextColor.GOLD),
            // Screenshot_173 bolds only ENABLED. Disabled is still an adaptation.
            nameBoldSpans = if (model.autoCentreEnabled)
                listOf(MenuBoldSpan(21, 28)) else emptyList()
        )
    )
}
