package dev.cubecrafttd.map.schematic

interface MapAssetDecoder {
    fun decode(bytes: ByteArray): DecodedMapVolume
}

class SpongeV2MapAssetDecoder(
    private val decoder: SpongeSchematicV2Decoder = SpongeSchematicV2Decoder()
) : MapAssetDecoder {
    override fun decode(bytes: ByteArray): DecodedMapVolume = decoder.decode(bytes)
}
