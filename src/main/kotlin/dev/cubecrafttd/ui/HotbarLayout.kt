package dev.cubecrafttd.ui

enum class HotbarAction {
    SWORD,
    BOW,
    SUMMONER,
    CASTLE_BAZAAR,
    SETTINGS
}

enum class HotbarLayoutEvidence {
    OFFICIAL_2021_SCREENSHOT_EXAMPLE,
    ENGINEERING_RUNTIME_DEFAULT,
    PLAYER_CUSTOM
}

data class HotbarLayout(
    val slots: Map<HotbarAction, Int>,
    val evidence: HotbarLayoutEvidence =
        HotbarLayoutEvidence.PLAYER_CUSTOM
) {
    init {
        require(slots.values.all { it in 0..8 })
        require(slots.values.distinct().size == slots.size)
    }

    fun slot(action: HotbarAction): Int =
        slots[action] ?: error("Action $action has no hotbar slot")

    fun move(
        action: HotbarAction,
        newSlot: Int
    ): HotbarLayout {
        require(newSlot in 0..8)
        val existingAtTarget = slots.entries
            .firstOrNull { it.value == newSlot }
        val oldSlot = slot(action)
        val next = slots.toMutableMap()
        next[action] = newSlot
        if (existingAtTarget != null &&
            existingAtTarget.key != action
        ) {
            next[existingAtTarget.key] = oldSlot
        }
        return HotbarLayout(
            next,
            HotbarLayoutEvidence.PLAYER_CUSTOM
        )
    }

    companion object {
        /**
         * Runtime compatibility preset only. Official 2021 evidence proves the
         * layout is player-customizable and saved between games; it does not
         * prove this was the universal default.
         */
        val ENGINEERING_RUNTIME_DEFAULT = HotbarLayout(
            mapOf(
                HotbarAction.SWORD to 0,
                HotbarAction.BOW to 1,
                HotbarAction.SUMMONER to 2,
                HotbarAction.CASTLE_BAZAAR to 3,
                HotbarAction.SETTINGS to 8
            ),
            HotbarLayoutEvidence.ENGINEERING_RUNTIME_DEFAULT
        )

        @Deprecated(
            "Not evidence-backed as a universal 2021 default; use ENGINEERING_RUNTIME_DEFAULT"
        )
        val DEFAULT_2021 = ENGINEERING_RUNTIME_DEFAULT

        /**
         * Official Jan-2021 inventory-layout screenshot example. It visibly
         * places the Summoner chest at slot 5, Bazaar block at slot 7 and
         * Settings crafting table at slot 9 (human numbering). The screenshot
         * also contains a utility AoE slot, which is modeled separately later
         * because the 2021 editor allows multiple AoEs.
         */
        val OFFICIAL_2021_SCREENSHOT_EXAMPLE = HotbarLayout(
            mapOf(
                HotbarAction.SWORD to 0,
                HotbarAction.BOW to 1,
                HotbarAction.SUMMONER to 4,
                HotbarAction.CASTLE_BAZAAR to 6,
                HotbarAction.SETTINGS to 8
            ),
            HotbarLayoutEvidence.OFFICIAL_2021_SCREENSHOT_EXAMPLE
        )
    }
}
