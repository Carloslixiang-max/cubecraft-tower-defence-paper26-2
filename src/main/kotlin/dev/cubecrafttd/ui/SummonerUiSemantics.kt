package dev.cubecrafttd.ui

data class QueueEditContext(
    val currentSelectedQuantity: Int,
    val queueRemainingCapacity: Int,
    val maxAffordableQuantity: Int
) {
    init {
        require(currentSelectedQuantity >= 0)
        require(queueRemainingCapacity >= 0)
        require(maxAffordableQuantity >= 0)
    }
}

sealed interface QueueEditIntent {
    data class SetQuantity(val quantity: Int) : QueueEditIntent
}

object SummonerClickSemantics {
    /**
     * Original behavior:
     * LEFT +1
     * RIGHT -1
     * SHIFT_LEFT buy/select as many as possible
     * SHIFT_RIGHT remove all selected of that type
     */
    fun resolve(
        click: ClickKind,
        context: QueueEditContext
    ): QueueEditIntent.SetQuantity {
        val current = context.currentSelectedQuantity
        val maxAdditional = minOf(
            context.queueRemainingCapacity,
            context.maxAffordableQuantity
        )
        val quantity = when (click) {
            ClickKind.LEFT ->
                current + if (maxAdditional > 0) 1 else 0
            ClickKind.RIGHT ->
                (current - 1).coerceAtLeast(0)
            ClickKind.SHIFT_LEFT ->
                current + maxAdditional
            ClickKind.SHIFT_RIGHT ->
                0
        }
        return QueueEditIntent.SetQuantity(quantity)
    }
}
