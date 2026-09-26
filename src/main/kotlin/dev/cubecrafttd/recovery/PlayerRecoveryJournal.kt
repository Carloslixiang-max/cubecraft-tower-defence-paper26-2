package dev.cubecrafttd.recovery

import dev.cubecrafttd.player.PlayerSnapshot
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

interface PlayerRecoveryJournal {
    fun save(snapshot: PlayerSnapshot)
    fun delete(playerUuid: UUID)
    fun loadAll(): List<PlayerSnapshot>
}

data class PlayerRecoveryJournalLoadFailure(
    val fileName: String,
    val reason: String
)

class FilePlayerRecoveryJournal(
    private val directory: Path
) : PlayerRecoveryJournal {
    @Volatile
    private var latestLoadFailures:
        List<PlayerRecoveryJournalLoadFailure> =
        emptyList()

    init {
        Files.createDirectories(directory)
    }

    fun loadFailures():
        List<PlayerRecoveryJournalLoadFailure> =
        latestLoadFailures.toList()

    override fun save(snapshot: PlayerSnapshot) {
        val target = file(snapshot.playerUuid)
        val temp = directory.resolve("${snapshot.playerUuid}.snapshot.tmp")
        Files.newOutputStream(temp).use { stream ->
            DataOutputStream(stream).use { PlayerSnapshotBinaryCodec.write(it, snapshot) }
        }
        try {
            Files.move(
                temp, target,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (_: Exception) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    override fun delete(playerUuid: UUID) {
        Files.deleteIfExists(file(playerUuid))
    }

    override fun loadAll(): List<PlayerSnapshot> {
        if(!Files.exists(directory)) {
            latestLoadFailures=
                emptyList()
            return emptyList()
        }

        val failures=
            mutableListOf<
                PlayerRecoveryJournalLoadFailure
            >()
        val snapshots=
            mutableListOf<PlayerSnapshot>()

        try {
            Files.list(directory).use { paths ->
                paths.filter {
                    it.fileName
                        .toString()
                        .endsWith(".snapshot")
                }
                    .sorted()
                    .forEach { path ->
                        runCatching {
                            Files.newInputStream(
                                path
                            ).use { stream ->
                                DataInputStream(
                                    stream
                                ).use(
                                    PlayerSnapshotBinaryCodec
                                        ::read
                                )
                            }
                        }.onSuccess {
                            snapshots += it
                        }.onFailure { error ->
                            failures +=
                                PlayerRecoveryJournalLoadFailure(
                                    fileName=
                                        path.fileName
                                            .toString(),
                                    reason=
                                        error.javaClass
                                            .simpleName +
                                        ": " +
                                        (
                                            error.message
                                                ?: "unknown decode failure"
                                        )
                                )
                        }
                    }
            }
        } catch(error:Throwable) {
            failures +=
                PlayerRecoveryJournalLoadFailure(
                    fileName="<recovery-directory>",
                    reason=
                        error.javaClass
                            .simpleName +
                        ": " +
                        (
                            error.message
                                ?: "failed to enumerate recovery directory"
                        )
                )
        }

        latestLoadFailures=
            failures.toList()
        return snapshots
    }

    private fun file(playerUuid: UUID): Path =
        directory.resolve("$playerUuid.snapshot")
}

object PlayerSnapshotBinaryCodec {
    private const val MAGIC = 0x43544453 // CTDS
    private const val VERSION_V1 = 1
    private const val VERSION_V2 = 2
    private const val CURRENT_VERSION = VERSION_V2

    fun write(out: DataOutputStream, s: PlayerSnapshot) {
        out.writeInt(MAGIC)
        out.writeInt(CURRENT_VERSION)
        writeCommon(out,s)
        out.writeInt(s.heldItemSlot)
        bytes(out,s.cursorItemPayload)
    }

    fun read(input: DataInputStream): PlayerSnapshot {
        require(input.readInt() == MAGIC) {
            "Not a CubeCraft TD player snapshot"
        }
        return when(val version=input.readInt()) {
            VERSION_V1 -> readV1(input)
            VERSION_V2 -> readV2(input)
            else -> error(
                "Unsupported player snapshot version $version"
            )
        }
    }

    private fun writeCommon(
        out: DataOutputStream,
        s: PlayerSnapshot
    ) {
        out.writeLong(s.playerUuid.mostSignificantBits)
        out.writeLong(s.playerUuid.leastSignificantBits)
        out.writeLong(s.capturedAtArenaTick)
        bytes(out, s.locationPayload)
        out.writeUTF(s.gameModeName)
        bytes(out, s.inventoryPayload)
        bytes(out, s.armorPayload)
        bytes(out, s.offhandPayload)
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
        bytes(out, s.potionEffectsPayload)
        bytes(out, s.velocityPayload)
    }

    private fun readV1(
        input: DataInputStream
    ): PlayerSnapshot =
        readCommon(
            input,
            heldItemSlot=0,
            cursorItemPayload=ByteArray(0)
        )

    private fun readV2(
        input: DataInputStream
    ): PlayerSnapshot {
        val common=readCommonValues(input)
        val slot=input.readInt()
        require(slot in 0..8) {
            "Invalid held item slot $slot"
        }
        val cursor=bytes(input)
        return common.toSnapshot(
            heldItemSlot=slot,
            cursorItemPayload=cursor
        )
    }

    private fun readCommon(
        input: DataInputStream,
        heldItemSlot: Int,
        cursorItemPayload: ByteArray
    ): PlayerSnapshot =
        readCommonValues(input).toSnapshot(
            heldItemSlot,
            cursorItemPayload
        )

    private data class CommonValues(
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
        val velocityPayload: ByteArray
    ) {
        fun toSnapshot(
            heldItemSlot: Int,
            cursorItemPayload: ByteArray
        )=PlayerSnapshot(
            playerUuid=playerUuid,
            capturedAtArenaTick=capturedAtArenaTick,
            locationPayload=locationPayload,
            gameModeName=gameModeName,
            inventoryPayload=inventoryPayload,
            armorPayload=armorPayload,
            offhandPayload=offhandPayload,
            level=level,
            expProgress=expProgress,
            totalExperience=totalExperience,
            health=health,
            absorption=absorption,
            foodLevel=foodLevel,
            saturation=saturation,
            exhaustion=exhaustion,
            fireTicks=fireTicks,
            remainingAir=remainingAir,
            allowFlight=allowFlight,
            flying=flying,
            fallDistance=fallDistance,
            potionEffectsPayload=potionEffectsPayload,
            velocityPayload=velocityPayload,
            heldItemSlot=heldItemSlot,
            cursorItemPayload=cursorItemPayload
        )
    }

    private fun readCommonValues(
        input: DataInputStream
    ): CommonValues {
        val uuid = UUID(
            input.readLong(),
            input.readLong()
        )
        return CommonValues(
            playerUuid=uuid,
            capturedAtArenaTick=input.readLong(),
            locationPayload=bytes(input),
            gameModeName=input.readUTF(),
            inventoryPayload=bytes(input),
            armorPayload=bytes(input),
            offhandPayload=bytes(input),
            level=input.readInt(),
            expProgress=input.readFloat(),
            totalExperience=input.readInt(),
            health=input.readDouble(),
            absorption=input.readDouble(),
            foodLevel=input.readInt(),
            saturation=input.readFloat(),
            exhaustion=input.readFloat(),
            fireTicks=input.readInt(),
            remainingAir=input.readInt(),
            allowFlight=input.readBoolean(),
            flying=input.readBoolean(),
            fallDistance=input.readFloat(),
            potionEffectsPayload=bytes(input),
            velocityPayload=bytes(input)
        )
    }

    private fun bytes(
        out: DataOutputStream,
        value: ByteArray
    ) {
        out.writeInt(value.size)
        out.write(value)
    }

    private fun bytes(
        input: DataInputStream
    ): ByteArray {
        val size=input.readInt()
        require(size in 0..16_777_216) {
            "Invalid snapshot payload size $size"
        }
        return ByteArray(size).also(
            input::readFully
        )
    }
}

