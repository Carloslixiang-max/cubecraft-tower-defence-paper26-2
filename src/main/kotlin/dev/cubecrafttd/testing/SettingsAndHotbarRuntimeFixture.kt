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

        val defaultLayoutValid=
            playerState.interaction
                .hotbarLayout
                .let { layout ->
                    layout.slot(
                        HotbarAction.SUMMONER
                    )==2 &&
                    layout.slot(
                        HotbarAction
                            .CASTLE_BAZAAR
                    )==3 &&
                    layout.slot(
                        HotbarAction.SETTINGS
                    )==8
                }

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

        val particle=
            router.handle(
                MenuActionInvocation(
                    player,
                    "settings:particle-density",
                    ClickKind.LEFT
                )
            ) as
                MatchMenuActionResult
                    .SettingsChanged

        val digitalHealth=
            router.handle(
                MenuActionInvocation(
                    player,
                    "settings:digital-mob-health",
                    ClickKind.LEFT
                )
            ) as
                MatchMenuActionResult
                    .SettingsChanged

        val damageIndicators=
            router.handle(
                MenuActionInvocation(
                    player,
                    "settings:damage-indicators",
                    ClickKind.LEFT
                )
            ) as
                MatchMenuActionResult
                    .SettingsChanged

        val menu=
            DynamicMatchMenus
                .settings(playerState)

        val hotbarChange=
            router.handle(
                MenuActionInvocation(
                    player,
                    "hotbar:move:SUMMONER:8",
                    ClickKind.LEFT
                )
            ) as
                MatchMenuActionResult
                    .HotbarLayoutChanged

        val hotbarMenu=
            DynamicMatchMenus
                .hotbarEditor(
                    playerState
                )
        val roundTrip=
            HotbarLayoutPersistenceCodec
                .decode(
                    HotbarLayoutPersistenceCodec
                        .encode(
                            hotbarChange.layout
                        )
                )

        return listOf(
            FixtureResult(
                "hotbar-default-2021-slots",
                defaultLayoutValid
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
            ),
            FixtureResult(
                "settings-router-particle-density-cycles-official-three-state-model",
                particle.settings
                    .particleDensity==
                    ParticleDensitySetting
                        .REDUCED_100 &&
                    ParticleDensitySetting
                        .HIGH_500.next()==
                    ParticleDensitySetting
                        .REDUCED_100 &&
                    ParticleDensitySetting
                        .REDUCED_100.next()==
                    ParticleDensitySetting
                        .MINIMUM &&
                    ParticleDensitySetting
                        .MINIMUM.next()==
                    ParticleDensitySetting
                        .HIGH_500
            ),
            FixtureResult(
                "settings-router-digital-mob-health-toggle",
                digitalHealth.settings
                    .digitalMobHealth
            ),
            FixtureResult(
                "settings-router-damage-indicators-toggle",
                damageIndicators.settings
                    .damageIndicators
            ),
            FixtureResult(
                "settings-menu-projects-official-2017-options",
                menu.slots.any {
                    it.actionId==
                        "settings:particle-density" &&
                    it.displayName==
                        "Particles: 100/sec"
                } &&
                    menu.slots.any {
                        it.actionId==
                            "settings:digital-mob-health" &&
                        it.displayName==
                            "Digital mob health: true"
                    } &&
                    menu.slots.any {
                        it.actionId==
                            "settings:damage-indicators" &&
                        it.displayName==
                            "Damage indicators: true"
                    } &&
                    menu.slots.any {
                        it.actionId==
                            "settings:point-purchases"
                    }
            ),
            FixtureResult(
                "hotbar-router-swap-and-custom-evidence",
                hotbarChange.layout
                    .slot(
                        HotbarAction.SUMMONER
                    )==8 &&
                    hotbarChange.layout
                        .slot(
                            HotbarAction.SETTINGS
                        )==2 &&
                    hotbarChange.layout
                        .evidence==
                        HotbarLayoutEvidence
                            .PLAYER_CUSTOM
            ),
            FixtureResult(
                "hotbar-editor-marks-current-slot",
                hotbarMenu.slots
                    .firstOrNull {
                        it.slot==26
                    }?.let {
                        it.actionId==
                            "hotbar:move:SUMMONER:8" &&
                            it.displayName
                                ?.contains(
                                    "(current)"
                                )==true
                    } == true
            ),
            FixtureResult(
                "hotbar-persistence-codec-roundtrip",
                roundTrip==
                    hotbarChange.layout &&
                    roundTrip?.evidence==
                        HotbarLayoutEvidence
                            .PLAYER_CUSTOM
            )
        )
    }
}
