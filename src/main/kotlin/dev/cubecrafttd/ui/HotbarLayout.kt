package dev.cubecrafttd.ui

enum class HotbarAction {
    SWORD,
    BOW,
    SUMMONER,
    CASTLE_BAZAAR,
    SETTINGS
}

sealed interface HotbarEntry {
    data class Action(
        val action: HotbarAction
    ) : HotbarEntry

    data class AoE(
        val potionId: String
    ) : HotbarEntry {
        init {
            require(potionId.isNotBlank())
        }
    }
}

enum class HotbarLayoutEvidence {
    OFFICIAL_2021_SCREENSHOT_EXAMPLE,
    ENGINEERING_RUNTIME_DEFAULT,
    PLAYER_CUSTOM
}

data class HotbarLayout(
    val slots: Map<HotbarAction, Int>,
    val evidence: HotbarLayoutEvidence =
        HotbarLayoutEvidence.PLAYER_CUSTOM,
    val aoeSlots: Map<String,Int> =
        emptyMap()
) {
    init {
        require(slots.values.all { it in 0..8 })
        require(aoeSlots.keys.all { it.isNotBlank() })
        require(aoeSlots.values.all { it in 0..8 })
        val occupied=
            slots.values +
                aoeSlots.values
        require(
            occupied.distinct().size==
                occupied.size
        ) {
            "Hotbar entries cannot share a slot"
        }
    }

    fun slot(action: HotbarAction): Int =
        slots[action]
            ?: error("Action $action has no hotbar slot")

    fun aoeSlot(potionId: String): Int? =
        aoeSlots[potionId]

    fun entryAt(slot: Int): HotbarEntry? {
        require(slot in 0..8)

        slots.entries
            .firstOrNull { it.value==slot }
            ?.let {
                return HotbarEntry.Action(
                    it.key
                )
            }

        aoeSlots.entries
            .firstOrNull { it.value==slot }
            ?.let {
                return HotbarEntry.AoE(
                    it.key
                )
            }

        return null
    }

    fun move(
        action: HotbarAction,
        newSlot: Int
    ): HotbarLayout {
        require(newSlot in 0..8)
        check(
            newSlot !in aoeSlots.values
        ) {
            "Fixed-action/AoE swap semantics are not recovered; move the AoE first"
        }

        val existingAtTarget=
            slots.entries
                .firstOrNull {
                    it.value==newSlot
                }
        val oldSlot=slot(action)
        val next=slots.toMutableMap()
        next[action]=newSlot
        if(
            existingAtTarget!=null &&
            existingAtTarget.key!=action
        ) {
            next[
                existingAtTarget.key
            ]=oldSlot
        }
        return HotbarLayout(
            next,
            HotbarLayoutEvidence.PLAYER_CUSTOM,
            aoeSlots
        )
    }

    /**
     * Exact original replacement/swap gestures are unresolved. AoE placement
     * therefore only targets an empty slot or its own current slot.
     */
    fun placeAoE(
        potionId: String,
        newSlot: Int
    ): HotbarLayout {
        require(potionId.isNotBlank())
        require(newSlot in 0..8)

        val occupied=entryAt(newSlot)
        check(
            occupied==null ||
                occupied==
                    HotbarEntry.AoE(
                        potionId
                    )
        ) {
            "Target hotbar slot is occupied; AoE replacement semantics are not recovered"
        }

        val next=aoeSlots.toMutableMap()
        next[potionId]=newSlot
        return HotbarLayout(
            slots,
            HotbarLayoutEvidence.PLAYER_CUSTOM,
            next
        )
    }

    fun removeAoE(
        potionId: String
    ): HotbarLayout {
        if(potionId !in aoeSlots)
            return this
        val next=aoeSlots.toMutableMap()
        next.remove(potionId)
        return HotbarLayout(
            slots,
            HotbarLayoutEvidence.PLAYER_CUSTOM,
            next
        )
    }

    companion object {
        val ENGINEERING_RUNTIME_DEFAULT =
            HotbarLayout(
                mapOf(
                    HotbarAction.SWORD to 0,
                    HotbarAction.BOW to 1,
                    HotbarAction.SUMMONER to 2,
                    HotbarAction.CASTLE_BAZAAR to 3,
                    HotbarAction.SETTINGS to 8
                ),
                HotbarLayoutEvidence
                    .ENGINEERING_RUNTIME_DEFAULT
            )

        @Deprecated(
            "Not evidence-backed as a universal 2021 default; use ENGINEERING_RUNTIME_DEFAULT"
        )
        val DEFAULT_2021 =
            ENGINEERING_RUNTIME_DEFAULT

        val OFFICIAL_2021_SCREENSHOT_EXAMPLE =
            HotbarLayout(
                mapOf(
                    HotbarAction.SWORD to 0,
                    HotbarAction.BOW to 1,
                    HotbarAction.SUMMONER to 4,
                    HotbarAction.CASTLE_BAZAAR to 6,
                    HotbarAction.SETTINGS to 8
                ),
                HotbarLayoutEvidence
                    .OFFICIAL_2021_SCREENSHOT_EXAMPLE
            )
    }
}
