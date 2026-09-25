package dev.cubecrafttd.ui

data class SummonerDraftKey(
    val mobId: String,
    val level: Int
) {
    init {
        require(mobId.isNotBlank())
        require(level in 1..5)
    }
}

data class SummonerDraftLine(
    val key: SummonerDraftKey,
    val quantity: Int
) {
    init {
        require(quantity > 0)
    }
}

/**
 * LinkedHashMap preserves first-selection order, matching the recovered
 * "first selected = first sent" behavior.
 */
class SummonerDraftState {
    private val quantities=
        linkedMapOf<SummonerDraftKey,Int>()

    fun quantity(
        key: SummonerDraftKey
    ): Int = quantities[key] ?: 0

    fun setQuantity(
        key: SummonerDraftKey,
        quantity: Int
    ) {
        require(quantity >= 0)
        if(quantity==0) {
            quantities.remove(key)
        } else {
            quantities[key]=quantity
        }
    }

    fun totalUnits(): Int =
        quantities.values.sum()

    fun snapshot():
        List<SummonerDraftLine> =
        quantities.map { (key,quantity) ->
            SummonerDraftLine(
                key,quantity
            )
        }

    fun clear() {
        quantities.clear()
    }

    fun isEmpty(): Boolean =
        quantities.isEmpty()
}
