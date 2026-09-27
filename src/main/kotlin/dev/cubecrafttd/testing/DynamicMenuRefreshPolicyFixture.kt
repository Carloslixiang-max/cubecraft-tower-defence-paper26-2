package dev.cubecrafttd.testing

import dev.cubecrafttd.ui.DynamicMenuRefreshPolicy

object DynamicMenuRefreshPolicyFixture {
    fun run(): List<FixtureResult> =
        listOf(
            FixtureResult(
                "dynamic-menu-refresh-policy-recognizes-stable-live-menus",
                mapOf(
                    "Mob Summoner" to
                        "summoner",
                    "Troop upgrades" to
                        "progression",
                    "Bazaar" to
                        "bazaar",
                    "Settings" to
                        "settings",
                    "Hotbar editor" to
                        "hotbar"
                ).all {
                    (title,kind) ->
                    DynamicMenuRefreshPolicy
                        .kindForTitle(
                            title
                        )==kind
                }
            ),
            FixtureResult(
                "dynamic-menu-refresh-policy-excludes-volatile-or-unowned-menus",
                DynamicMenuRefreshPolicy
                    .kindForTitle(
                        "Armageddon vote · WITHER"
                    )==null &&
                    DynamicMenuRefreshPolicy
                        .kindForTitle(
                            "Tower builder"
                        )==null
            ),
            FixtureResult(
                "dynamic-menu-refresh-policy-cadence-is-bounded",
                DynamicMenuRefreshPolicy
                    .INTERVAL_TICKS in
                    1L..20L
            )
        )
}
