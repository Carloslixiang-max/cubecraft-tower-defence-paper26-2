package dev.cubecrafttd.truth

/**
 * DTO mirroring the project truth-gate JSON. Unknown original values remain null.
 * Engineering fallbacks are loaded from a separate source and never written back here.
 */
data class TruthGateDocument(
    val schemaVersion: String,
    val targetPreset: String,
    val fields: Map<String, TruthField<Any?>>
)

interface TruthGateDataSource {
    fun load(): TruthGateDocument
}

class TruthGateRepository(
    private val source: TruthGateDataSource
) {
    private val document: TruthGateDocument by lazy(source::load)

    fun field(key: String): TruthField<Any?> =
        document.fields[key] ?: error("Unknown truth field: $key")

    fun all(): Map<String, TruthField<Any?>> = document.fields.toMap()
}
