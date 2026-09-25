package dev.cubecrafttd.testing

import dev.cubecrafttd.ui.*

object TowerManagementMenuFixture {
    fun run():List<FixtureResult> {
        val menu=
            TowerManagementMenus
                .engineering(42)

        return listOf(
            FixtureResult(
                "tower-management-functional-actions",
                menu.actionAt(11)==
                    "tower-manage:42:stats" &&
                    menu.actionAt(13)==
                        "tower-manage:42:upgrade" &&
                    menu.actionAt(15)==
                        "tower-manage:42:sell"
            ),
            FixtureResult(
                "tower-management-slots-remain-engineering",
                menu.evidenceStatus==
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK &&
                    menu.slots.all {
                        it.evidenceStatus==
                            UiEvidenceStatus
                                .ENGINEERING_FALLBACK
                    }
            )
        )
    }
}
