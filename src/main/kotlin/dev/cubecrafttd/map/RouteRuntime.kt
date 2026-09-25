package dev.cubecrafttd.map

import kotlin.math.sqrt

data class Vec3(val x: Double, val y: Double, val z: Double) {
    fun distanceTo(other: Vec3): Double {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}

data class RouteSegment(
    val index: Int,
    val start: Vec3,
    val end: Vec3,
    val cumulativeStart: Double,
    val length: Double
)

data class RouteRuntime(
    val routeId: String,
    val nodes: List<Vec3>,
    val segments: List<RouteSegment>,
    val totalLength: Double
) {
    companion object {
        fun compile(routeId: String, nodes: List<Vec3>): RouteRuntime {
            require(nodes.size >= 2)
            var cumulative = 0.0
            val segments = nodes.zipWithNext().mapIndexed { index, (a, b) ->
                val length = a.distanceTo(b)
                RouteSegment(index, a, b, cumulative, length).also { cumulative += length }
            }
            return RouteRuntime(routeId, nodes.toList(), segments, cumulative)
        }
    }
}
