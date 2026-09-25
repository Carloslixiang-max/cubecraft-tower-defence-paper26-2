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
     * The existence of statistics / upgrade / sell actions is historical
     * direct CubeCraft evidence. Exact Mature slot indices are not recovered,
     * so this functional Stage-4 layout stays ENGINEERING_FALLBACK.
     */
    fun engineering(
        towerInstanceId: Long
    ): MenuDefinition =
        MenuDefinition(
            title="Tower management",
            size=27,
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
                        .ENGINEERING_FALLBACK,
                    "Upgrade tower"
                ),
                MenuSlot(
                    15,
                    "tower-manage:$towerInstanceId:sell",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Sell tower"
                ),
                MenuSlot(
                    22,
                    "tower-manage:$towerInstanceId:rangefinder",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Toggle permanent rangefinder"
                )
            )
        )
}
