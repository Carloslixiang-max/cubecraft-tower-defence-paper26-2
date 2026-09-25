package dev.cubecrafttd.map.schematic

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

data class SchematicDimensions(val width: Int, val height: Int, val length: Int)

data class SchematicOffset(val x: Int, val y: Int, val z: Int)

data class DecodedBlockEntity(
    val id: String,
    val x: Int,
    val y: Int,
    val z: Int
)

data class DecodedMapVolume(
    val rootName: String,
    val dimensions: SchematicDimensions,
    val offset: SchematicOffset,
    val palette: Map<String, Int>,
    val paletteInverse: Map<Int, String>,
    val blockIds: IntArray,
    val sha256: String,
    val blockEntities: List<DecodedBlockEntity> = emptyList(),
    val schematicEntityCount: Int = 0
) {
    private fun index(x: Int, y: Int, z: Int): Int =
        x + z * dimensions.width + y * dimensions.width * dimensions.length

    fun idAt(x: Int, y: Int, z: Int): Int? {
        if (x !in 0 until dimensions.width ||
            y !in 0 until dimensions.height ||
            z !in 0 until dimensions.length
        ) return null
        return blockIds[index(x, y, z)]
    }

    fun blockAt(x: Int, y: Int, z: Int): String? =
        idAt(x, y, z)?.let(paletteInverse::get)

    fun countBlock(blockState: String): Int {
        val id = palette[blockState] ?: return 0
        return blockIds.count { it == id }
    }
}

class SpongeSchematicV2Decoder {
    fun decode(sourceBytes: ByteArray): DecodedMapVolume {
        val sha = sha256(sourceBytes)
        val decompressed = gunzipIfNeeded(sourceBytes)
        val reader = NbtReader(decompressed)
        val (rootName, root) = reader.rootCompound()

        fun number(key: String): Int =
            (root[key] as? Number)?.toInt()
                ?: error("Missing/invalid schematic number: $key")

        val width = number("Width")
        val height = number("Height")
        val length = number("Length")

        val offsetList = root["Offset"] as? List<*> ?: listOf(0, 0, 0)
        require(offsetList.size >= 3) { "Offset must have 3 entries" }
        val offset = SchematicOffset(
            (offsetList[0] as Number).toInt(),
            (offsetList[1] as Number).toInt(),
            (offsetList[2] as Number).toInt()
        )

        val paletteRaw = root["Palette"] as? Map<*, *>
            ?: error("Missing Palette")
        val palette = paletteRaw.entries.associate { (k, v) ->
            (k as? String ?: error("Palette key is not string")) to
                ((v as? Number)?.toInt() ?: error("Palette id is not numeric"))
        }
        val inverse = palette.entries.associate { it.value to it.key }

        val blockData = root["BlockData"] as? ByteArray
            ?: error("Missing BlockData byte array")
        val ids = decodeVarInts(blockData).toIntArray()

        val blockEntities = (root["BlockEntities"] as? List<*>)
            .orEmpty()
            .map { raw ->
                val map = raw as? Map<*, *>
                    ?: error("BlockEntities entry is not a compound")
                val id = map["Id"] as? String
                    ?: error("BlockEntity missing Id")
                val pos = map["Pos"] as? List<*>
                    ?: error("BlockEntity $id missing Pos")
                require(pos.size >= 3)
                DecodedBlockEntity(
                    id,
                    (pos[0] as Number).toInt(),
                    (pos[1] as Number).toInt(),
                    (pos[2] as Number).toInt()
                )
            }
        val schematicEntityCount =
            (root["Entities"] as? List<*>)?.size ?: 0

        val expected = width * height * length
        require(ids.size == expected) {
            "Block count ${ids.size} != expected $expected"
        }

        return DecodedMapVolume(
            rootName = rootName,
            dimensions = SchematicDimensions(width, height, length),
            offset = offset,
            palette = palette,
            paletteInverse = inverse,
            blockIds = ids,
            sha256 = sha,
            blockEntities = blockEntities,
            schematicEntityCount = schematicEntityCount
        )
    }

    private fun gunzipIfNeeded(bytes: ByteArray): ByteArray {
        if (bytes.size >= 2 &&
            bytes[0].toInt() and 0xFF == 0x1F &&
            bytes[1].toInt() and 0xFF == 0x8B
        ) {
            return GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
        }
        return bytes
    }

    private fun decodeVarInts(data: ByteArray): List<Int> {
        val out = ArrayList<Int>()
        var value = 0
        var shift = 0
        for (raw in data) {
            val byte = raw.toInt() and 0xFF
            value = value or ((byte and 0x7F) shl shift)
            if ((byte and 0x80) != 0) {
                shift += 7
                require(shift <= 35) { "Invalid VarInt in BlockData" }
            } else {
                out += value
                value = 0
                shift = 0
            }
        }
        require(shift == 0) { "Truncated VarInt in BlockData" }
        return out
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
}

private class NbtReader(bytes: ByteArray) {
    private val input = DataInputStream(ByteArrayInputStream(bytes))

    fun rootCompound(): Pair<String, Map<String, Any?>> {
        val tag = u8()
        require(tag == 10) { "Expected root TAG_Compound (10), got $tag" }
        val name = string()
        @Suppress("UNCHECKED_CAST")
        val payload = payload(tag) as Map<String, Any?>
        return name to payload
    }

    private fun u8(): Int = input.readUnsignedByte()
    private fun i8(): Byte = input.readByte()
    private fun i16(): Short = input.readShort()
    private fun u16(): Int = input.readUnsignedShort()
    private fun i32(): Int = input.readInt()
    private fun i64(): Long = input.readLong()
    private fun f32(): Float = input.readFloat()
    private fun f64(): Double = input.readDouble()

    private fun string(): String {
        val length = u16()
        val bytes = ByteArray(length)
        input.readFully(bytes)
        return bytes.toString(Charsets.UTF_8)
    }

    private fun payload(tag: Int): Any? = when (tag) {
        1 -> i8()
        2 -> i16()
        3 -> i32()
        4 -> i64()
        5 -> f32()
        6 -> f64()
        7 -> ByteArray(i32()).also(input::readFully)
        8 -> string()
        9 -> {
            val childTag = u8()
            val size = i32()
            List(size) { payload(childTag) }
        }
        10 -> {
            val map = linkedMapOf<String, Any?>()
            while (true) {
                val childTag = try { u8() } catch (e: EOFException) {
                    throw EOFException("NBT compound truncated")
                }
                if (childTag == 0) break
                val name = string()
                map[name] = payload(childTag)
            }
            map
        }
        11 -> List(i32()) { i32() }
        12 -> List(i32()) { i64() }
        else -> error("Unsupported NBT tag $tag")
    }
}
