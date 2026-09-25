package dev.cubecrafttd.testing

import dev.cubecrafttd.player.*
import java.util.UUID

private class RoundTripFixtureAdapter(
    private val uuid: UUID,
    private val perturbAfterRestore:
        Boolean = false
) : PlayerStateAdapter {
    private var current=
        fixtureSnapshot(
            uuid,
            heldItemSlot=6
        )

    override fun capture(
        playerUuid: UUID,
        arenaTick: Long
    ): PlayerSnapshot =
        current.copy(
            capturedAtArenaTick=
                arenaTick
        )

    override fun prepareForMatch(
        playerUuid: UUID
    ) {
        current=
            fixtureSnapshot(
                uuid,
                heldItemSlot=0
            ).copy(
                inventoryPayload=
                    byteArrayOf(99)
            )
    }

    override fun restore(
        snapshot: PlayerSnapshot
    ) {
        current=
            if(perturbAfterRestore) {
                snapshot.copy(
                    heldItemSlot=5
                )
            } else snapshot
    }

    override fun isOnline(
        playerUuid: UUID
    ): Boolean = true

    companion object {
        fun fixtureSnapshot(
            uuid: UUID,
            heldItemSlot: Int
        ) =
            PlayerSnapshot(
                playerUuid=uuid,
                capturedAtArenaTick=0L,
                locationPayload=
                    byteArrayOf(1,2),
                gameModeName="SURVIVAL",
                inventoryPayload=
                    byteArrayOf(3,4),
                armorPayload=
                    byteArrayOf(5),
                offhandPayload=
                    byteArrayOf(6),
                level=7,
                expProgress=0.4f,
                totalExperience=123,
                health=18.0,
                absorption=2.0,
                foodLevel=17,
                saturation=4f,
                exhaustion=1f,
                fireTicks=2,
                remainingAir=280,
                allowFlight=true,
                flying=true,
                fallDistance=1.5f,
                potionEffectsPayload=
                    byteArrayOf(7,8),
                velocityPayload=
                    byteArrayOf(9,10),
                heldItemSlot=
                    heldItemSlot,
                cursorItemPayload=
                    byteArrayOf(11)
            )
    }
}

object PlayerSnapshotRoundTripFixture {
    fun run(): List<FixtureResult> {
        val uuid=
            UUID.fromString(
                "00000000-0000-0000-0000-000000044001"
            )

        val exact=
            PlayerSnapshotRoundTripService(
                RoundTripFixtureAdapter(
                    uuid
                )
            ).run(
                uuid,
                44L
            )

        val perturbed=
            PlayerSnapshotRoundTripService(
                RoundTripFixtureAdapter(
                    uuid,
                    perturbAfterRestore=true
                )
            ).run(
                uuid,
                44L
            )

        val a=
            RoundTripFixtureAdapter
                .fixtureSnapshot(
                    uuid,6
                )
        val b=
            a.copy(
                inventoryPayload=
                    byteArrayOf(42)
            )
        val direct=
            PlayerSnapshotComparator
                .compare(a,b)

        return listOf(
            FixtureResult(
                "snapshot-roundtrip-service-restores-all-fields",
                exact.passed
            ),
            FixtureResult(
                "snapshot-roundtrip-detects-held-slot-drift",
                !perturbed.passed &&
                    perturbed.mismatches==
                        listOf(
                            "heldItemSlot"
                        )
            ),
            FixtureResult(
                "snapshot-comparator-identifies-payload-drift",
                direct.mismatches==
                    listOf(
                        "inventory"
                    )
            )
        )
    }
}
