package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.paper.LiveMenuView

object PaperUiLiveEvidenceClassifier {
    fun classify(
        menu: LiveMenuView
    ): PaperUiLiveEvidence? =
        when {
            menu.title==
                "Tower builder" ->
                PaperUiLiveEvidence
                    .TOWER_BUILDER

            menu.title==
                "Select an upgrade path" ->
                PaperUiLiveEvidence
                    .PATH_SELECTOR

            menu.slotActionIds
                .values
                .any {
                    it.startsWith(
                        "tower-manage:"
                    )
                } ->
                PaperUiLiveEvidence
                    .TOWER_MENU

            menu.title==
                "Settings" ->
                PaperUiLiveEvidence
                    .SETTINGS

            menu.title==
                "Change inventory layout" ->
                PaperUiLiveEvidence
                    .INVENTORY_LAYOUT

            else -> null
        }
}
