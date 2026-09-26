package dev.cubecrafttd.player

import java.util.UUID

/**
 * Core-domain representation only. Paper adapters serialize/restore actual ItemStack,
 * Location, potion effects, etc. Opaque payloads keep Bukkit types out of core.
 */
data class PlayerSnapshot(
    val playerUuid: UUID,
    val capturedAtArenaTick: Long,
    val locationPayload: ByteArray,
    val gameModeName: String,
    val inventoryPayload: ByteArray,
    val armorPayload: ByteArray,
    val offhandPayload: ByteArray,
    val level: Int,
    val expProgress: Float,
    val totalExperience: Int,
    val health: Double,
    val absorption: Double,
    val foodLevel: Int,
    val saturation: Float,
    val exhaustion: Float,
    val fireTicks: Int,
    val remainingAir: Int,
    val allowFlight: Boolean,
    val flying: Boolean,
    val fallDistance: Float,
    val potionEffectsPayload: ByteArray,
    val velocityPayload: ByteArray,
    val heldItemSlot: Int = 0,
    val cursorItemPayload: ByteArray = ByteArray(0)
) {
    init {
        require(heldItemSlot in 0..8)
    }
}

enum class PlayerSnapshotState { CAPTURED, RESTORING, RESTORED }

data class PlayerSnapshotRecord(
    val snapshot: PlayerSnapshot,
    var state: PlayerSnapshotState = PlayerSnapshotState.CAPTURED
)

interface PlayerStateAdapter {
    fun capture(playerUuid: UUID, arenaTick: Long): PlayerSnapshot
    fun prepareForMatch(playerUuid: UUID)
    fun restore(snapshot: PlayerSnapshot)
    fun isOnline(playerUuid: UUID): Boolean
}

/**
 * Exactly-once semantics inside one arena lifecycle.
 * A death/quit/end callback may all race logically; only the first successful restore wins.
 */
class PlayerSnapshotStore {
    private val records = linkedMapOf<UUID, PlayerSnapshotRecord>()

    fun put(snapshot: PlayerSnapshot) {
        val existing=
            records[snapshot.playerUuid]
        check(
            existing==null ||
                existing.state==
                    PlayerSnapshotState.RESTORED
        ) {
            "Snapshot already active for ${snapshot.playerUuid}: ${existing?.state}"
        }
        records[snapshot.playerUuid] =
            PlayerSnapshotRecord(snapshot)
    }

    fun canCapture(
        playerUuid: UUID
    ): Boolean {
        val existing=
            records[playerUuid]
        return existing==null ||
            existing.state==
                PlayerSnapshotState.RESTORED
    }

    fun record(playerUuid: UUID): PlayerSnapshotRecord? = records[playerUuid]

    fun beginRestore(playerUuid: UUID): PlayerSnapshot? {
        val record = records[playerUuid] ?: return null
        if (record.state != PlayerSnapshotState.CAPTURED) return null
        record.state = PlayerSnapshotState.RESTORING
        return record.snapshot
    }

    fun markRestored(playerUuid: UUID) {
        val record = records[playerUuid] ?: return
        check(record.state == PlayerSnapshotState.RESTORING)
        record.state = PlayerSnapshotState.RESTORED
    }

    fun markRestoreFailed(playerUuid: UUID) {
        val record = records[playerUuid] ?: return
        if (record.state == PlayerSnapshotState.RESTORING) {
            record.state = PlayerSnapshotState.CAPTURED
        }
    }

    fun unresolved(): Set<UUID> = records
        .filterValues { it.state != PlayerSnapshotState.RESTORED }
        .keys

    fun clearRestored() {
        records.entries.removeIf { it.value.state == PlayerSnapshotState.RESTORED }
    }
}
