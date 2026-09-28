package dev.cubecrafttd.testing

import dev.cubecrafttd.ui.AoEPotionInventory

object AoEPotionInventoryFixture {
    fun run(): List<FixtureResult> {
        val inventory=
            AoEPotionInventory()
        inventory.add(
            "meteor"
        )
        inventory.add(
            "meteor"
        )
        inventory.add(
            "speed"
        )

        val stacked=
            inventory.quantity(
                "meteor"
            )==2 &&
            inventory.quantity(
                "speed"
            )==1 &&
            inventory.snapshot()==
                mapOf(
                    "meteor" to 2,
                    "speed" to 1
                )

        inventory.consume(
            "meteor"
        )
        inventory.consume(
            "speed"
        )

        val consumed=
            inventory.quantity(
                "meteor"
            )==1 &&
            inventory.quantity(
                "speed"
            )==0 &&
            "speed" !in
                inventory.snapshot()

        val underflowRejected=
            runCatching {
                inventory.consume(
                    "speed"
                )
            }.isFailure

        return listOf(
            FixtureResult(
                "aoe-inventory-stacks-owned-purchases",
                stacked
            ),
            FixtureResult(
                "aoe-inventory-consume-decrements-and-removes-zero",
                consumed
            ),
            FixtureResult(
                "aoe-inventory-underflow-fails-closed",
                underflowRejected
            )
        )
    }
}
