package dev.cubecrafttd.ui

object HotbarLayoutPersistenceCodec {
    private const val AOE_PREFIX=
        "AOE__"

    fun encode(
        layout: HotbarLayout
    ): Map<String,Int> =
        buildMap {
            layout.slots.forEach {
                (action,slot) ->
                put(action.name,slot)
            }
            layout.aoeSlots.forEach {
                (potionId,slot) ->
                put(
                    AOE_PREFIX+potionId,
                    slot
                )
            }
        }

    fun decode(
        values: Map<String,Int>
    ): HotbarLayout? {
        val slots=
            HotbarAction.entries
                .mapNotNull {
                    action ->
                    values[action.name]
                        ?.let {
                            action to it
                        }
                }
                .toMap()
        if(
            slots.size !=
                HotbarAction.entries.size
        ) return null

        val knownPotionIds=
            RecommendedMatureBazaarDefinitions
                .aoePotions
                .mapTo(linkedSetOf()) {
                    it.potionId
                }
        val aoeSlots=
            values.mapNotNull {
                (key,slot) ->
                if(
                    !key.startsWith(
                        AOE_PREFIX
                    )
                ) {
                    null
                } else {
                    key.removePrefix(
                        AOE_PREFIX
                    )
                        .takeIf {
                            it in knownPotionIds
                        }
                        ?.let {
                            it to slot
                        }
                }
            }.toMap()

        return runCatching {
            HotbarLayout(
                slots,
                HotbarLayoutEvidence
                    .PLAYER_CUSTOM,
                aoeSlots
            )
        }.getOrNull()
    }
}
