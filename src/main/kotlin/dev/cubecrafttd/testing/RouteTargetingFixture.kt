package dev.cubecrafttd.testing

data class RouteCandidate(val id: String, val routeProgress: Double)

object RouteTargetingFixture {
    fun first(candidates: Collection<RouteCandidate>): RouteCandidate? =
        candidates.maxWithOrNull(compareBy<RouteCandidate> { it.routeProgress }.thenByDescending { it.id })

    fun last(candidates: Collection<RouteCandidate>): RouteCandidate? =
        candidates.minWithOrNull(compareBy<RouteCandidate> { it.routeProgress }.thenBy { it.id })

    /**
     * routeProgress, not Euclidean endpoint distance, defines FIRST/LAST.
     * Stable ID tie-break is ENGINEERING_ONLY determinism, not original target-policy truth.
     */
    fun run(): List<FixtureResult> {
        val uShape = listOf(
            RouteCandidate("mob-a", 12.0),
            RouteCandidate("mob-b", 48.0),
            RouteCandidate("mob-c", 31.0)
        )
        val tied = listOf(
            RouteCandidate("mob-b", 20.0),
            RouteCandidate("mob-a", 20.0)
        )

        return listOf(
            FixtureResult("route-first-max-progress", first(uShape)?.id == "mob-b"),
            FixtureResult("route-last-min-progress", last(uShape)?.id == "mob-a"),
            FixtureResult(
                "route-tie-deterministic-engineering",
                first(tied)?.id == "mob-a" && last(tied)?.id == "mob-a",
                listOf("Tie-break is deterministic engineering policy only.")
            )
        )
    }
}
