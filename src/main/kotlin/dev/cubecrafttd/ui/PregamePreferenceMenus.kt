package dev.cubecrafttd.ui

/**
 * Waiting-lobby preference surfaces.
 *
 * Official 2021 evidence confirms that Settings and Change inventory layout
 * were available before a match. The exact entry-point slots in CubeCraft's
 * waiting lobby are not recovered here, so navigation from the Engineering
 * pregame hub remains explicitly fallback. These menus never mutate the
 * player's real waiting-lobby inventory.
 */
object PregamePreferenceMenus {
    fun settings(
        settings: PlayerMatchSettings
    ): MenuDefinition {
        val model=
            SettingsMenuProjector
                .project(settings)

        return MenuDefinition(
            title="Settings",
            size=45,
            evidenceStatus=
                UiEvidenceStatus
                    .MATURE_CONTEXT,
            slots=listOf(
                MenuSlot(
                    10,
                    "pregame-settings:particle-density",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Particles: ${model.particleDensity.label}"
                ),
                HistoricalSettingsControls.autoCentre(
                    model,
                    "pregame-settings:auto-centre"
                ),
                MenuSlot(
                    12,
                    "pregame-settings:digital-mob-health",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Digital mob health: ${model.digitalMobHealth}"
                ),
                MenuSlot(
                    13,
                    "pregame-settings:damage-indicators",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Damage indicators: ${model.damageIndicators}"
                ),
                MenuSlot(
                    15,
                    "noop:pregame-settings:point-purchases-unavailable",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "In-game Point purchases: unavailable"
                ),
                MenuSlot(
                    31,
                    "pregame-nav:vote",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Back to pregame voting",
                    "book"
                ),
                MenuSlot(
                    32,
                    "pregame-nav:hotbar",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Change inventory layout",
                    "item-frame"
                ),
                // Direct 2021 screenshot evidence recovers the book at slot 40,
                // but not its click semantics.
                MenuSlot(
                    40,
                    "noop:settings:2021-navigation-book-unresolved",
                    UiEvidenceStatus
                        .MATURE_DIRECT,
                    "2021 navigation control (action unresolved)",
                    "book"
                )
            )
        )
    }

    fun hotbar(
        layout: HotbarLayout,
        selected:
            HotbarEditorSelection?
    ): MenuDefinition {
        val sourceSlots=
            listOf(
                0,1,2,
                9,10,11,
                18,19,20
            )

        val slots=
            buildList {
                RecommendedMatureBazaarDefinitions
                    .aoePotions
                    .take(sourceSlots.size)
                    .forEachIndexed {
                        index,potion ->
                        val selection=
                            HotbarEditorSelection
                                .AoE(
                                    potion.potionId
                                )
                        add(
                            MenuSlot(
                                sourceSlots[index],
                                "pregame-hotbar:select-aoe:" +
                                    potion.potionId,
                                UiEvidenceStatus
                                    .ENGINEERING_FALLBACK,
                                potion.potionId +
                                    " AoE" +
                                    if(
                                        selected==
                                            selection
                                    )
                                        " (selected)"
                                    else
                                        "",
                                "splash-potion"
                            )
                        )
                    }

                repeat(9) {
                    hotbarSlot ->
                    val occupant=
                        layout.entryAt(
                            hotbarSlot
                        )
                    add(
                        MenuSlot(
                            27+hotbarSlot,
                            bottomAction(
                                hotbarSlot,
                                occupant,
                                selected
                            ),
                            UiEvidenceStatus
                                .MATURE_CONTEXT,
                            occupant?.let {
                                entryLabel(it) +
                                    " · slot " +
                                    (hotbarSlot+1) +
                                    if(
                                        selectionMatches(
                                            selected,
                                            it
                                        )
                                    )
                                        " (selected)"
                                    else
                                        ""
                            } ?: (
                                "Empty hotbar slot " +
                                    (hotbarSlot+1)
                            ),
                            occupant?.let(
                                ::entryIcon
                            ) ?: "empty-slot"
                        )
                    )
                }

                add(
                    MenuSlot(
                        26,
                        "pregame-nav:vote",
                        UiEvidenceStatus
                            .ENGINEERING_FALLBACK,
                        "Back to pregame voting",
                        "book"
                    )
                )
            }

        return MenuDefinition(
            title="Change inventory layout",
            size=36,
            slots=slots,
            evidenceStatus=
                UiEvidenceStatus
                    .MATURE_CONTEXT
        )
    }

    private fun bottomAction(
        hotbarSlot: Int,
        occupant: HotbarEntry?,
        selected:
            HotbarEditorSelection?
    ): String {
        if(selected==null) {
            return when(occupant) {
                is HotbarEntry.Action ->
                    "pregame-hotbar:select:" +
                        occupant.action.name
                is HotbarEntry.AoE ->
                    "pregame-hotbar:select-aoe:" +
                        occupant.potionId
                null ->
                    "noop:pregame-hotbar:empty:" +
                        hotbarSlot
            }
        }

        val canPlace=
            when(selected) {
                is HotbarEditorSelection.Action ->
                    occupant !is
                        HotbarEntry.AoE
                is HotbarEditorSelection.AoE ->
                    occupant==null ||
                        occupant==
                            HotbarEntry.AoE(
                                selected.potionId
                            )
            }

        return if(canPlace)
            "pregame-hotbar:place:" +
                hotbarSlot
        else
            "noop:pregame-hotbar:swap-semantics-unresolved:" +
                hotbarSlot
    }

    private fun selectionMatches(
        selection:
            HotbarEditorSelection?,
        entry: HotbarEntry
    ): Boolean =
        when {
            selection is
                HotbarEditorSelection.Action &&
                entry is
                    HotbarEntry.Action ->
                selection.action==
                    entry.action
            selection is
                HotbarEditorSelection.AoE &&
                entry is
                    HotbarEntry.AoE ->
                selection.potionId==
                    entry.potionId
            else -> false
        }

    private fun entryLabel(
        entry: HotbarEntry
    ): String =
        when(entry) {
            is HotbarEntry.Action ->
                when(entry.action) {
                    HotbarAction.SWORD ->
                        "Sword"
                    HotbarAction.BOW ->
                        "Bow"
                    HotbarAction.SUMMONER ->
                        "Mob Summoner"
                    HotbarAction.CASTLE_BAZAAR ->
                        "Castle Bazaar"
                    HotbarAction.SETTINGS ->
                        "Settings"
                }
            is HotbarEntry.AoE ->
                entry.potionId +
                    " AoE"
        }

    private fun entryIcon(
        entry: HotbarEntry
    ): String =
        when(entry) {
            is HotbarEntry.Action ->
                when(entry.action) {
                    HotbarAction.SWORD ->
                        "wooden-sword"
                    HotbarAction.BOW ->
                        "bow"
                    HotbarAction.SUMMONER ->
                        "chest"
                    HotbarAction.CASTLE_BAZAAR ->
                        "bricks"
                    HotbarAction.SETTINGS ->
                        "crafting-table"
                }
            is HotbarEntry.AoE ->
                "splash-potion"
        }
}
