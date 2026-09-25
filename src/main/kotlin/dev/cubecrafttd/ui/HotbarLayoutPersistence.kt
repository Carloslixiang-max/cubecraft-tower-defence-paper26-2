package dev.cubecrafttd.ui

object HotbarLayoutPersistenceCodec {
    fun encode(
        layout: HotbarLayout
    ): Map<String,Int> =
        layout.slots
            .entries
            .associate {
                it.key.name to it.value
            }

    fun decode(
        values: Map<String,Int>
    ): HotbarLayout? {
        val slots=
            HotbarAction.entries
                .mapNotNull { action ->
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

        return runCatching {
            HotbarLayout(
                slots,
                HotbarLayoutEvidence
                    .PLAYER_CUSTOM
            )
        }.getOrNull()
    }
}
