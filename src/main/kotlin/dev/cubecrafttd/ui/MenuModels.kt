package dev.cubecrafttd.ui

enum class UiEvidenceStatus {
    MATURE_DIRECT,
    MATURE_CONTEXT,
    HISTORICAL_DIRECT,
    ENGINEERING_FALLBACK,
    UNKNOWN
}

enum class MenuTextColor { GREEN, RED, GRAY, GOLD, YELLOW }

/** UTF-16 offsets, end exclusive; only directly observed spans are styled. */
data class MenuBoldSpan(
    val start: Int,
    val endExclusive: Int,
    val color: MenuTextColor? = null
) {
    init { require(start >= 0 && endExclusive > start) }
}

/** Explicit styling for recovered text; unstyled Engineering labels stay plain. */
data class MenuTextStyle(
    val nameColor: MenuTextColor,
    val loreColors: Map<Int, MenuTextColor> = emptyMap(),
    val nameBoldSpans: List<MenuBoldSpan> = emptyList(),
    val loreBoldSpans: Map<Int, List<MenuBoldSpan>> = emptyMap()
) {
    fun validate(name: String?, lore: List<String>) {
        fun checkSpans(text: String, spans: List<MenuBoldSpan>) {
            require(spans.all { it.endExclusive <= text.length })
            require(spans.zipWithNext().all { (a, b) -> a.endExclusive <= b.start })
        }
        if (nameBoldSpans.isNotEmpty()) checkSpans(requireNotNull(name), nameBoldSpans)
        loreBoldSpans.forEach { (index, spans) ->
            require(index in lore.indices)
            checkSpans(lore[index], spans)
        }
    }
}

data class MenuSlot(
    val slot: Int,
    val actionId: String,
    val evidenceStatus: UiEvidenceStatus,
    val displayName: String? = null,
    val iconHint: String? = null,
    val lore: List<String> = emptyList(),
    val textStyle: MenuTextStyle? = null
) {
    init {
        require(slot >= 0)
        textStyle?.validate(displayName, lore)
    }
}

data class MenuDefinition(
    val title: String,
    val size: Int,
    val slots: List<MenuSlot>,
    val evidenceStatus: UiEvidenceStatus
) {
    init {
        require(size > 0 && size % 9 == 0)
        require(slots.all { it.slot < size })
        require(slots.map { it.slot }.distinct().size == slots.size)
    }

    fun actionAt(slot: Int): String? =
        slots.firstOrNull { it.slot == slot }?.actionId
}

enum class ClickKind {
    LEFT,
    RIGHT,
    SHIFT_LEFT,
    SHIFT_RIGHT
}


data class MenuActionInvocation(
    val playerUuid: java.util.UUID,
    val actionId: String,
    val click: ClickKind
) {
    init {
        require(actionId.isNotBlank())
    }
}
