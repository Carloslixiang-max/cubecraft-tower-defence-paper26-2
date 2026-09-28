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

        val pointPurchaseRejected=
            runCatching {
                router.handle(
                    MenuActionInvocation(
                        player,
                        "settings:point-purchases",
                        ClickKind.LEFT
                    )
                )
            }.isFailure

        val menu=
            DynamicMatchMenus
                .settings(playerState)

        playerState.interaction
            .aoeInventory
            .add(
                "meteor",
                2
            )

        val editorInitial=
            DynamicMatchMenus
                .hotbarEditor(
                    playerState
                )

        val selected=
            router.handle(
                MenuActionInvocation(
                    player,
                    "hotbar:select:SUMMONER",
                    ClickKind.LEFT
                )
            ) as
                MatchMenuActionResult
                    .HotbarEditorSelectionChanged

        val editorSelected=
            DynamicMatchMenus
                .hotbarEditor(
                    playerState
                )

        val hotbarChange=
            router.handle(
                MenuActionInvocation(
                    player,
                    "hotbar:place:8",
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

        val aoeSelected=
            router.handle(
                MenuActionInvocation(
                    player,
                    "hotbar:select-aoe:meteor",
                    ClickKind.LEFT
                )
            ) as
                MatchMenuActionResult
                    .HotbarEditorSelectionChanged

        val aoePlaced=
            router.handle(
                MenuActionInvocation(
                    player,
                    "hotbar:place:4",
                    ClickKind.LEFT
                )
            ) as
                MatchMenuActionResult
                    .HotbarLayoutChanged

        val roundTrip=
            HotbarLayoutPersistenceCodec
                .decode(
                    HotbarLayoutPersistenceCodec
                        .encode(
                            aoePlaced.layout
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
                "settings-menu-recovers-2021-45-slot-shell",
                menu.size==45 &&
                    menu.evidenceStatus==
                        UiEvidenceStatus
                            .MATURE_CONTEXT &&
                    menu.slots
                        .first {
                            it.slot==40
                        }
                        .let {
                            it.actionId==
                                "noop:settings:2021-navigation-book-unresolved" &&
                            it.evidenceStatus==
                                UiEvidenceStatus
                                    .MATURE_DIRECT &&
                            it.iconHint=="book"
                        }
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
                            "noop:settings:point-purchases-unavailable" &&
                        it.displayName==
                            "In-game Point purchases: unavailable"
                    }
            ),
            FixtureResult(
                "settings-point-purchase-backend-explicitly-unavailable",
                !SettingsRuntimeCapabilities
                    .IN_GAME_POINT_PURCHASES &&
                    pointPurchaseRejected
            ),
            FixtureResult(
                "settings-menu-does-not-expose-fake-point-purchase-toggle",
                menu.slots.none {
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
                "hotbar-editor-recovers-2021-36-slot-shell",
                editorInitial.title==
                    "Change inventory layout" &&
                    editorInitial.size==36 &&
                    editorInitial.evidenceStatus==
                        UiEvidenceStatus
                            .MATURE_CONTEXT &&
                    (27..35).all {
                        slot ->
                        editorInitial.slots.any {
                            it.slot==slot
                        }
                    }
            ),
            FixtureResult(
                "hotbar-editor-select-then-place-flow",
                selected.selectedAction==
                    HotbarAction.SUMMONER &&
                    editorSelected.slots
                        .first {
                            it.slot==35
                        }
                        .actionId==
                        "hotbar:place:8" &&
                    hotbarChange.layout
                        .slot(
                            HotbarAction.SUMMONER
                        )==8 &&
                    playerState.interaction
                        .hotbarEditorSelection==
                        null
            ),
            FixtureResult(
                "hotbar-editor-bottom-row-is-clickable-current-layout",
                hotbarMenu.slots
                    .first {
                        it.slot==35
                    }
                    .let {
                        it.actionId==
                            "hotbar:select:SUMMONER" &&
                        it.displayName==
                            "Mob Summoner · slot 9" &&
                        it.iconHint=="chest"
                    } &&
                    hotbarMenu.slots
                        .count {
                            it.slot in 27..35
                        }==9
            ),
            FixtureResult(
                "hotbar-editor-no-longer-projects-invented-fixed-action-palette",
                hotbarMenu.slots
                    .filter {
                        it.slot<27
                    }
                    .all {
                        it.actionId.startsWith(
                            "hotbar:select-aoe:"
                        )
                    }
            ),
            FixtureResult(
                "hotbar-editor-owned-aoe-can-fill-empty-slot",
                aoeSelected
                    .selectedAoEPotionId==
                    "meteor" &&
                    aoePlaced.layout
                        .aoeSlot(
                            "meteor"
                        )==4 &&
                    aoePlaced.layout
                        .entryAt(
                            4
                        )==
                        HotbarEntry.AoE(
                            "meteor"
                        )
            ),
            FixtureResult(
                "hotbar-persistence-codec-roundtrip",
                roundTrip==
                    aoePlaced.layout &&
                    roundTrip?.evidence==
                        HotbarLayoutEvidence
                            .PLAYER_CUSTOM &&
                    roundTrip
                        .aoeSlot(
                            "meteor"
                        )==4
            )
        )
    }
}
