package dev.cubecrafttd.ui

enum class UiEvidenceStatus {
    MATURE_DIRECT,
    MATURE_CONTEXT,
    HISTORICAL_DIRECT,
    ENGINEERING_FALLBACK,
    UNKNOWN
}

data class MenuSlot(
    val slot: Int,
    val actionId: String,
    val evidenceStatus: UiEvidenceStatus,
    val displayName: String? = null,
    val iconHint: String? = null
) {
    init { require(slot >= 0) }
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
