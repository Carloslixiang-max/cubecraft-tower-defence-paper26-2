package dev.cubecrafttd.player

import java.util.UUID

data class PlayerSnapshotComparison(
    val mismatches: List<String>
) {
    val passed: Boolean
        get() = mismatches.isEmpty()
}

object PlayerSnapshotComparator {
    fun compare(
        expected: PlayerSnapshot,
        actual: PlayerSnapshot
    ): PlayerSnapshotComparison {
        val out=
            mutableListOf<String>()

        fun scalar(
            key: String,
            left: Any?,
            right: Any?
        ) {
            if(left!=right)
                out += key
        }

        fun bytes(
            key: String,
            left: ByteArray,
            right: ByteArray
        ) {
            if(!left.contentEquals(right))
                out += key
        }

        scalar(
            "playerUuid",
            expected.playerUuid,
            actual.playerUuid
        )
        scalar(
            "capturedAtArenaTick",
            expected.capturedAtArenaTick,
            actual.capturedAtArenaTick
        )
        bytes(
            "location",
            expected.locationPayload,
            actual.locationPayload
        )
        scalar(
            "gameMode",
            expected.gameModeName,
            actual.gameModeName
        )
        bytes(
            "inventory",
            expected.inventoryPayload,
            actual.inventoryPayload
        )
        bytes(
            "armor",
            expected.armorPayload,
            actual.armorPayload
        )
        bytes(
            "offhand",
            expected.offhandPayload,
            actual.offhandPayload
        )
        scalar(
            "level",
            expected.level,
            actual.level
        )
        scalar(
            "expProgress",
            expected.expProgress,
            actual.expProgress
        )
        scalar(
            "totalExperience",
            expected.totalExperience,
            actual.totalExperience
        )
        scalar(
            "health",
            expected.health,
            actual.health
        )
        scalar(
            "absorption",
            expected.absorption,
            actual.absorption
        )
        scalar(
            "foodLevel",
            expected.foodLevel,
            actual.foodLevel
        )
        scalar(
            "saturation",
            expected.saturation,
            actual.saturation
        )
        scalar(
            "exhaustion",
            expected.exhaustion,
            actual.exhaustion
        )
        scalar(
            "fireTicks",
            expected.fireTicks,
            actual.fireTicks
        )
        scalar(
            "remainingAir",
            expected.remainingAir,
            actual.remainingAir
        )
        scalar(
            "allowFlight",
            expected.allowFlight,
            actual.allowFlight
        )
        scalar(
            "flying",
            expected.flying,
            actual.flying
        )
        scalar(
            "fallDistance",
            expected.fallDistance,
            actual.fallDistance
        )
        bytes(
            "potionEffects",
            expected.potionEffectsPayload,
            actual.potionEffectsPayload
        )
        bytes(
            "velocity",
            expected.velocityPayload,
            actual.velocityPayload
        )
        scalar(
            "heldItemSlot",
            expected.heldItemSlot,
            actual.heldItemSlot
        )
        bytes(
            "cursorItem",
            expected.cursorItemPayload,
            actual.cursorItemPayload
        )

        return PlayerSnapshotComparison(
            out
        )
    }
}

class PlayerSnapshotRoundTripService(
    private val adapter:
        PlayerStateAdapter
) {
    fun run(
        playerUuid: UUID,
        markerTick: Long = 0L
    ): PlayerSnapshotComparison {
        val before=
            adapter.capture(
                playerUuid,
                markerTick
            )
        var restored=false
        try {
            adapter.prepareForMatch(
                playerUuid
            )
            adapter.restore(before)
            restored=true
            val after=
                adapter.capture(
                    playerUuid,
                    markerTick
                )
            return PlayerSnapshotComparator
                .compare(
                    before,
                    after
                )
        } catch(t:Throwable) {
            if(!restored) {
                runCatching {
                    adapter.restore(before)
                }.onFailure {
                    t.addSuppressed(it)
                }
            }
            throw t
        }
    }
}
