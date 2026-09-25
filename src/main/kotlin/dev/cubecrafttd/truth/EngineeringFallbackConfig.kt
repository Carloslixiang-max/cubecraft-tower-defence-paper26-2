package dev.cubecrafttd.truth

/** Fallback values are never serialized back as observed original truth. */
data class EngineeringFallbackConfig(
    val values: Map<String, Any> = emptyMap()
)
