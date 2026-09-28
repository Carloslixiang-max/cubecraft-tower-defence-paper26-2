package dev.cubecrafttd.ui

/**
 * Match-local owned AoE potion inventory.
 *
 * Official 2021 evidence proves that players could keep more than one AoE in
 * their hotbar. This model therefore represents purchased potions as owned
 * consumable quantities instead of treating the Bazaar click itself as the
 * throw/use action.
 */
class AoEPotionInventory {
    private val counts=
        linkedMapOf<String,Int>()

    fun quantity(
        potionId: String
    ): Int =
        counts[potionId] ?: 0

    fun add(
        potionId: String,
        quantity: Int = 1
    ) {
        require(
            potionId.isNotBlank()
        )
        require(quantity>0)
        counts[potionId]=
            quantity(potionId)+
                quantity
    }

    fun consume(
        potionId: String,
        quantity: Int = 1
    ) {
        require(quantity>0)
        val current=
            quantity(potionId)
        check(current>=quantity) {
            "Not enough owned AoE potion $potionId"
        }
        val next=
            current-quantity
        if(next==0) {
            counts.remove(
                potionId
            )
        } else {
            counts[potionId]=
                next
        }
    }

    fun snapshot():
        Map<String,Int> =
        LinkedHashMap(counts)
}
