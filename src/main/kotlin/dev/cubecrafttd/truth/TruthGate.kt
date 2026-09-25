package dev.cubecrafttd.truth

enum class EvidenceStatus {
    MATURE_DIRECT,
    MATURE_CONTEXT,
    MATURE_CONTRACT,
    HISTORICAL_ONLY,
    VIDEO_REQUIRED,
    VIDEO_AND_MAP_REQUIRED,
    ENGINEERING_ONLY
}

data class TruthField<T>(
    val key: String,
    val observedOriginalValue: T?,
    val status: EvidenceStatus,
    val evidenceRefs: List<String> = emptyList()
)

class TruthGate(
    private val engineeringFallbacks: EngineeringFallbackConfig = EngineeringFallbackConfig()
) {
    fun <T> requireObserved(field: TruthField<T>): T = field.observedOriginalValue
        ?: error("Original value unresolved for ${field.key}; status=${field.status}")

    @Suppress("UNCHECKED_CAST")
    fun <T> resolveWithExplicitFallback(field: TruthField<T>): ResolvedTruth<T> {
        field.observedOriginalValue?.let {
            return ResolvedTruth(it, ResolutionSource.OBSERVED_ORIGINAL)
        }
        val fallback = engineeringFallbacks.values[field.key]
            ?: error("No observed original or explicit engineering fallback for ${field.key}")
        return ResolvedTruth(fallback as T, ResolutionSource.ENGINEERING_FALLBACK)
    }
}

enum class ResolutionSource { OBSERVED_ORIGINAL, ENGINEERING_FALLBACK }

data class ResolvedTruth<T>(val value: T, val source: ResolutionSource)
