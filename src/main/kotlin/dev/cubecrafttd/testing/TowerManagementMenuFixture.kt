package dev.cubecrafttd.testing

import dev.cubecrafttd.ui.*

object TowerManagementMenuFixture {
    fun run():List<FixtureResult> {
        val menu=
            TowerManagementMenus
                .mature2021(
                    42,
                    "Zeus Tower",
                    1,
                    false
                )
        val pinned=
            TowerManagementMenus
                .mature2021(
                    42,
                    "Zeus Tower",
                    4,
                    true
                )

        return listOf(
            FixtureResult(
                "tower-management-functional-actions-remain-available",
                menu.actionAt(11)==
                    "tower-manage:42:stats" &&
                    menu.actionAt(13)==
                        "tower-manage:42:upgrade" &&
                    menu.actionAt(15)==
                        "tower-manage:42:sell"
            ),
            FixtureResult(
                "tower-management-2021-shell-recovers-size-title-and-rangefinder-slot",
                menu.size==45 &&
                    menu.title=="Zeus Tower I" &&
                    menu.actionAt(36)==
                        "tower-manage:42:rangefinder" &&
                    menu.slots
                        .first {
                            it.slot==36
                        }
                        .let {
                            it.evidenceStatus==
                                UiEvidenceStatus
                                    .MATURE_DIRECT &&
                            it.iconHint=="stick" &&
                            it.displayName==
                                "Click to Enable this tower's rangefinder"
                        }
            ),
            FixtureResult(
                "tower-management-2021-bottom-row-direct-visuals-are-preserved-with-unresolved-actions",
                menu.actionAt(40)==
                    "noop:tower-manage:42:2021-book-unresolved" &&
                    menu.actionAt(44)==
                        "noop:tower-manage:42:2021-barrier-unresolved" &&
                    menu.slots
                        .first { it.slot==40 }
                        .let {
                            it.evidenceStatus==
                                UiEvidenceStatus.MATURE_DIRECT &&
                            it.iconHint=="book"
                        } &&
                    menu.slots
                        .first { it.slot==44 }
                        .let {
                            it.evidenceStatus==
                                UiEvidenceStatus.MATURE_DIRECT &&
                            it.iconHint=="barrier"
                        }
            ),
            FixtureResult(
                "tower-management-rangefinder-state-and-roman-level-project",
                pinned.title==
                    "Zeus Tower IV" &&
                    pinned.slots
                        .first {
                            it.slot==36
                        }
                        .displayName==
                    "Click to Disable this tower's rangefinder"
            ),
            FixtureResult(
                "tower-management-unrecovered-controls-stay-weaker-evidence",
                menu.evidenceStatus==
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK &&
                    menu.slots
                        .first {
                            it.actionId==
                                "tower-manage:42:upgrade"
                        }
                        .let {
                            it.evidenceStatus==
                                UiEvidenceStatus
                                    .HISTORICAL_DIRECT &&
                            it.iconHint=="anvil"
                        } &&
                    menu.slots
                        .filter {
                            it.actionId.endsWith(
                                ":stats"
                            ) ||
                            it.actionId.endsWith(
                                ":sell"
                            )
                        }
                        .all {
                            it.evidenceStatus==
                                UiEvidenceStatus
                                    .ENGINEERING_FALLBACK
                        }
            )
        )
    }
}
