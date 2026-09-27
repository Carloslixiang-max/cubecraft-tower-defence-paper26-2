package dev.cubecrafttd.ui

enum class TowerManagementAction {
    SHOW_STATS,
    UPGRADE,
    SELL,
    TOGGLE_RANGEFINDER
}

data class TowerInteractionIntent(
    val openManagementMenu: Boolean,
    val quickUpgrade: Boolean
)

object TowerWorldInteractionSemantics {
    fun rightClick(sneaking: Boolean): TowerInteractionIntent =
        if (sneaking) {
            TowerInteractionIntent(
                openManagementMenu = false,
                quickUpgrade = true
            )
        } else {
            TowerInteractionIntent(
                openManagementMenu = true,
                quickUpgrade = false
            )
        }
}


object TowerManagementMenus {
    /**
     * Mixed-evidence 2021 tower menu shell.
     *
     * Direct official screenshot evidence recovers a 54-slot inventory, a
     * concrete title example ("Zeus Tower I"), and the rangefinder control in
     * zero-based slot 45. Statistics / upgrade / sell exist historically, but
     * their exact 2021 slots remain unrecovered, so those controls intentionally
     * retain the previous Engineering fallback positions.
     */
    fun mature2021(
        towerInstanceId: Long,
        towerDisplayName: String,
        level: Int,
        rangefinderPinned: Boolean
    ): MenuDefinition =
        MenuDefinition(
            title=
                towerDisplayName +
                    " " +
                    romanLevel(level),
            size=54,
            evidenceStatus=
                UiEvidenceStatus
                    .ENGINEERING_FALLBACK,
            slots=listOf(
                MenuSlot(
                    11,
                    "tower-manage:$towerInstanceId:stats",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Tower statistics"
                ),
                MenuSlot(
                    13,
                    "tower-manage:$towerInstanceId:upgrade",
                    UiEvidenceStatus
                        .HISTORICAL_DIRECT,
                    "Upgrade tower",
                    "anvil"
                ),
                MenuSlot(
                    15,
                    "tower-manage:$towerInstanceId:sell",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Sell tower"
                ),
                MenuSlot(
                    45,
                    "tower-manage:$towerInstanceId:rangefinder",
                    UiEvidenceStatus
                        .MATURE_DIRECT,
                    if(rangefinderPinned)
                        "Click to Disable this tower's rangefinder"
                    else
                        "Click to Enable this tower's rangefinder",
                    "stick"
                )
            )
        )

    /**
     * Compatibility entry point for older callers. Runtime Paper menus use
     * [mature2021] with the real tower name, level and pin state.
     */
    fun engineering(
        towerInstanceId: Long
    ): MenuDefinition =
        mature2021(
            towerInstanceId,
            "Tower",
            1,
            false
        )

    private fun romanLevel(
        level: Int
    ): String =
        when(level) {
            1 -> "I"
            2 -> "II"
            3 -> "III"
            4 -> "IV"
            else -> level.toString()
        }
}
