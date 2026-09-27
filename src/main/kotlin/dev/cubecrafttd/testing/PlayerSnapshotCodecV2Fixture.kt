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
        cursorItemPayload=byteArrayOf(9,8,7),
        flySpeed=0.35f
    )

    private fun encodeCurrent(
        s:PlayerSnapshot
    ):ByteArray =
        ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use {
                PlayerSnapshotBinaryCodec
                    .write(it,s)
            }
            bytes.toByteArray()
        }

    private fun decode(
        bytes:ByteArray
    ):PlayerSnapshot =
        DataInputStream(
            ByteArrayInputStream(bytes)
        ).use(
            PlayerSnapshotBinaryCodec::read
        )

    private fun payload(
        out:DataOutputStream,
        value:ByteArray
    ) {
        out.writeInt(value.size)
        out.write(value)
    }

    private fun writeLegacyCommon(
        out:DataOutputStream,
        s:PlayerSnapshot
    ) {
        out.writeLong(
            s.playerUuid.mostSignificantBits
        )
        out.writeLong(
            s.playerUuid.leastSignificantBits
        )
        out.writeLong(
            s.capturedAtArenaTick
        )
        payload(out,s.locationPayload)
        out.writeUTF(s.gameModeName)
        payload(out,s.inventoryPayload)
        payload(out,s.armorPayload)
        payload(out,s.offhandPayload)
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
        payload(out,s.potionEffectsPayload)
        payload(out,s.velocityPayload)
    }

    private fun encodeLegacyV2(
        s:PlayerSnapshot
    ):ByteArray =
        ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { out ->
                out.writeInt(0x43544453)
                out.writeInt(2)
                writeLegacyCommon(out,s)
                out.writeInt(s.heldItemSlot)
                payload(
                    out,
                    s.cursorItemPayload
                )
            }
            bytes.toByteArray()
        }

    private fun encodeLegacyV1(
        s:PlayerSnapshot
    ):ByteArray =
        ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { out ->
                out.writeInt(0x43544453)
                out.writeInt(1)
                writeLegacyCommon(out,s)
            }
            bytes.toByteArray()
        }

    fun run():List<FixtureResult> {
        val original=snapshot()
        val v3=decode(
            encodeCurrent(original)
        )
        val v2=decode(
            encodeLegacyV2(original)
        )
        val v1=decode(
            encodeLegacyV1(original)
        )

        return listOf(
            FixtureResult(
                "snapshot-v3-roundtrip-preserves-fly-speed",
                v3.playerUuid==uuid &&
                    v3.heldItemSlot==7 &&
                    v3.cursorItemPayload
                        .contentEquals(
                            byteArrayOf(9,8,7)
                        ) &&
                    v3.flySpeed==0.35f
            ),
            FixtureResult(
                "snapshot-v2-backward-compatible-default-fly-speed",
                v2.playerUuid==uuid &&
                    v2.heldItemSlot==7 &&
                    v2.cursorItemPayload
                        .contentEquals(
                            byteArrayOf(9,8,7)
                        ) &&
                    v2.flySpeed==0.1f
            ),
            FixtureResult(
                "snapshot-v1-backward-compatible",
                v1.playerUuid==uuid &&
                    v1.level==5 &&
                    v1.heldItemSlot==0 &&
                    v1.cursorItemPayload
                        .isEmpty() &&
                    v1.flySpeed==0.1f
            ),
            FixtureResult(
                "snapshot-v3-existing-fields-preserved",
                v3.locationPayload
                    .contentEquals(
                        original.locationPayload
                    ) &&
                    v3.inventoryPayload
                        .contentEquals(
                            original.inventoryPayload
                    ) &&
                    v3.health==18.0 &&
                    v3.flying
            )
        )
    }
}
