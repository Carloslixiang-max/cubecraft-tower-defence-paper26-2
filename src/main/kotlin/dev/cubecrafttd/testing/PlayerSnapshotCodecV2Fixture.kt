package dev.cubecrafttd.testing

import dev.cubecrafttd.player.PlayerSnapshot
import dev.cubecrafttd.recovery.PlayerSnapshotBinaryCodec
import java.io.*
import java.util.UUID

object PlayerSnapshotCodecV2Fixture {
    private val uuid=UUID.fromString(
        "00000000-0000-0000-0000-000000016001"
    )

    private fun snapshot()=PlayerSnapshot(
        playerUuid=uuid,
        capturedAtArenaTick=77,
        locationPayload=byteArrayOf(1),
        gameModeName="SURVIVAL",
        inventoryPayload=byteArrayOf(2),
        armorPayload=byteArrayOf(3),
        offhandPayload=byteArrayOf(4),
        level=5,
        expProgress=0.25f,
        totalExperience=123,
        health=18.0,
        absorption=2.0,
        foodLevel=17,
        saturation=3f,
        exhaustion=1f,
        fireTicks=4,
        remainingAir=299,
        allowFlight=true,
        flying=true,
        fallDistance=2f,
        potionEffectsPayload=byteArrayOf(5),
        velocityPayload=byteArrayOf(6),
        heldItemSlot=7,
        cursorItemPayload=byteArrayOf(9,8,7)
    )

    private fun encodeV2(s:PlayerSnapshot):ByteArray =
        ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use {
                PlayerSnapshotBinaryCodec
                    .write(it,s)
            }
            bytes.toByteArray()
        }

    private fun decode(bytes:ByteArray):PlayerSnapshot =
        DataInputStream(
            ByteArrayInputStream(bytes)
        ).use(PlayerSnapshotBinaryCodec::read)

    /**
     * Exact legacy v1 wire format to prove upgrade compatibility.
     */
    private fun encodeLegacyV1(
        s:PlayerSnapshot
    ):ByteArray =
        ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { out ->
                out.writeInt(0x43544453)
                out.writeInt(1)
                out.writeLong(
                    s.playerUuid.mostSignificantBits
                )
                out.writeLong(
                    s.playerUuid.leastSignificantBits
                )
                out.writeLong(
                    s.capturedAtArenaTick
                )
                fun payload(v:ByteArray) {
                    out.writeInt(v.size)
                    out.write(v)
                }
                payload(s.locationPayload)
                out.writeUTF(s.gameModeName)
                payload(s.inventoryPayload)
                payload(s.armorPayload)
                payload(s.offhandPayload)
                out.writeInt(s.level)
                out.writeFloat(s.expProgress)
                out.writeInt(s.totalExperience)
                out.writeDouble(s.health)
                out.writeDouble(s.absorption)
                out.writeInt(s.foodLevel)
                out.writeFloat(s.saturation)
                out.writeFloat(s.exhaustion)
                out.writeInt(s.fireTicks)
                out.writeInt(s.remainingAir)
                out.writeBoolean(s.allowFlight)
                out.writeBoolean(s.flying)
                out.writeFloat(s.fallDistance)
                payload(s.potionEffectsPayload)
                payload(s.velocityPayload)
            }
            bytes.toByteArray()
        }

    fun run():List<FixtureResult> {
        val original=snapshot()
        val v2=decode(encodeV2(original))
        val v1=decode(
            encodeLegacyV1(original)
        )

        return listOf(
            FixtureResult(
                "snapshot-v2-roundtrip-new-fields",
                v2.playerUuid==uuid &&
                    v2.heldItemSlot==7 &&
                    v2.cursorItemPayload
                        .contentEquals(
                            byteArrayOf(9,8,7)
                        )
            ),
            FixtureResult(
                "snapshot-v1-backward-compatible",
                v1.playerUuid==uuid &&
                    v1.level==5 &&
                    v1.heldItemSlot==0 &&
                    v1.cursorItemPayload.isEmpty()
            ),
            FixtureResult(
                "snapshot-v2-existing-fields-preserved",
                v2.locationPayload
                    .contentEquals(
                        original.locationPayload
                    ) &&
                    v2.inventoryPayload
                        .contentEquals(
                            original.inventoryPayload
                        ) &&
                    v2.health==18.0 &&
                    v2.flying
            )
        )
    }
}
