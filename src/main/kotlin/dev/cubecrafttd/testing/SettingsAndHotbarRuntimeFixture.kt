package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.match.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.progression.*
import dev.cubecrafttd.ui.*
import java.util.UUID

object SettingsAndHotbarRuntimeFixture {
    fun run():List<FixtureResult> {
        val player=UUID.fromString(
            "00000000-0000-0000-0000-000000033001"
        )
        val context=ArenaContext(
            ArenaId("settings-hotbar"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000033100"
            ),
            TestingMapFactory.minimal()
        ).also {
            it.redTeam.players += player
            it.state=ArenaState.RUNNING
        }
        val ledger=EconomyLedger()
        val playerState=
            PlayerMatchSessionState(
                player,
                TroopProgressionState(),
                interaction=
                    PlayerMatchInteractionState(
                        settings=
                            PlayerMatchSettings(
                                lifetimeWins=20
                            )
                    )
            )
        val session=MatchSessionState(
            MatchRulePreset(
                MatchMode.NORMAL,
                PricingMode.NORMAL,
                GoldmineIncomeMode.NORMAL,
                ProgressionMode
                    .CLASSIC_PROGRESSION,
                MatchStartingBalance(0,0),
                1,1
            ),
            linkedMapOf(
                player to playerState
            )
        )
        val runtime=
            NormalArenaRuntimeState
                .withCadence(
                    dev.cubecrafttd.troop
                        .TroopSpawnCadence(
                            1L,
                            "fixture"
                        )
                )
        val router=
            MatchMenuActionRouter(
                context,runtime,session,
                ledger,
                TroopProgressionService(
                    ledger
                )
            )

        val centre=
            router.handle(
                MenuActionInvocation(
                    player,
                    "settings:auto-centre",
                    ClickKind.LEFT
                )
            ) as
                MatchMenuActionResult
                    .SettingsChanged

        val menu=
            DynamicMatchMenus
                .settings(playerState)

        return listOf(
            FixtureResult(
                "hotbar-default-2021-slots",
                playerState.interaction
                    .hotbarLayout
                    .slot(
                        HotbarAction.SUMMONER
                    )==2 &&
                    playerState.interaction
                        .hotbarLayout
                        .slot(
                            HotbarAction
                                .CASTLE_BAZAAR
                        )==3 &&
                    playerState.interaction
                        .hotbarLayout
                        .slot(
                            HotbarAction.SETTINGS
                        )==8
            ),
            FixtureResult(
                "settings-router-auto-centre-unlock",
                !centre.settings
                    .autoCentreEnabled
            ),
            FixtureResult(
                "settings-menu-remains-layout-fallback",
                menu.evidenceStatus==
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK
            )
        )
    }
}
