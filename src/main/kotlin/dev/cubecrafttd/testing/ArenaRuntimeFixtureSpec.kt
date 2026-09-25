package dev.cubecrafttd.testing

/** Pure-domain fixture descriptions; Paper integration tests live separately. */
data class FixtureResult(
    val id: String,
    val passed: Boolean,
    val details: List<String> = emptyList()
)

interface ArenaRuntimeFixture {
    val id: String
    fun run(): FixtureResult
}
