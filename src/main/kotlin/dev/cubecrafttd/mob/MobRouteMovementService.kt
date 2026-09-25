package dev.cubecrafttd.mob

import dev.cubecrafttd.map.RouteRuntime
import dev.cubecrafttd.map.Vec3
import kotlin.math.max
import kotlin.math.min

data class RouteAdvanceResult(
    val position: Vec3,
    val reachedTerminal: Boolean,
    val distanceConsumed: Double
)


data class RouteProgressSample(
    val routeState: MobRouteState,
    val position: Vec3
)

object RouteProgressSampler {
    fun sample(
        route: RouteRuntime,
        progress: Double
    ): RouteProgressSample {
        require(progress >= 0.0)
        val p=progress.coerceAtMost(
            route.totalLength
        )

        if(route.segments.isEmpty()) {
            val terminal=route.nodes.last()
            return RouteProgressSample(
                MobRouteState(
                    route.routeId,
                    0,1.0,
                    route.totalLength
                ),
                terminal
            )
        }

        val index=
            route.segments.indexOfLast {
                p + 1e-12 >=
                    it.cumulativeStart
            }.coerceIn(
                0,route.segments.lastIndex
            )
        val segment=route.segments[index]
        val local=
            (p-segment.cumulativeStart)
                .coerceIn(
                    0.0,segment.length
                )
        val fraction=
            if(segment.length==0.0)
                1.0
            else
                (local/segment.length)
                    .coerceIn(0.0,1.0)

        val position=Vec3(
            segment.start.x +
                (segment.end.x-
                    segment.start.x)*fraction,
            segment.start.y +
                (segment.end.y-
                    segment.start.y)*fraction,
            segment.start.z +
                (segment.end.z-
                    segment.start.z)*fraction
        )

        return RouteProgressSample(
            MobRouteState(
                route.routeId,
                index,
                fraction,
                p
            ),
            position
        )
    }
}

object MobRouteMovementService {
    /**
     * Advances along immutable route geometry by an engineering distance value.
     * This service does NOT define how CubeCraft speed stats map to blocks/tick.
     */
    fun advance(
        mob: MobRuntimeState,
        route: RouteRuntime,
        distance: Double
    ): RouteAdvanceResult {
        require(distance >= 0.0)
        require(mob.route.routeId == route.routeId)
        check(mob.combat.lifecycle == MobLifecycleState.MOVING)

        if (route.segments.isEmpty()) {
            val terminal = route.nodes.last()
            mob.route = mob.route.copy(
                segmentIndex = 0,
                segmentProgress = 1.0,
                routeProgress = route.totalLength
            )
            return RouteAdvanceResult(terminal, true, 0.0)
        }

        var remaining = distance
        var segmentIndex = mob.route.segmentIndex.coerceIn(0, route.segments.lastIndex)
        var distanceIntoSegment = currentDistanceIntoSegment(mob, route, segmentIndex)
        val startRouteProgress = mob.route.routeProgress

        while (remaining > 0.0 && segmentIndex <= route.segments.lastIndex) {
            val segment = route.segments[segmentIndex]
            val left = max(0.0, segment.length - distanceIntoSegment)
            if (remaining + 1e-12 < left) {
                distanceIntoSegment += remaining
                remaining = 0.0
                break
            }

            remaining -= left
            if (segmentIndex == route.segments.lastIndex) {
                distanceIntoSegment = segment.length
                break
            }
            segmentIndex++
            distanceIntoSegment = 0.0
        }

        val segment = route.segments[segmentIndex]
        val fraction = if (segment.length == 0.0) 1.0
            else (distanceIntoSegment / segment.length).coerceIn(0.0, 1.0)
        val routeProgress = min(
            route.totalLength,
            segment.cumulativeStart + distanceIntoSegment
        )
        val position = interpolate(segment.start, segment.end, fraction)
        val reached = segmentIndex == route.segments.lastIndex &&
            distanceIntoSegment >= segment.length - 1e-12

        mob.route = mob.route.copy(
            segmentIndex = segmentIndex,
            segmentProgress = fraction,
            routeProgress = routeProgress
        )

        return RouteAdvanceResult(
            position = position,
            reachedTerminal = reached,
            distanceConsumed = routeProgress - startRouteProgress
        )
    }

    fun position(mob: MobRuntimeState, route: RouteRuntime): Vec3 {
        val index = mob.route.segmentIndex.coerceIn(0, route.segments.lastIndex)
        val segment = route.segments[index]
        return interpolate(
            segment.start,
            segment.end,
            mob.route.segmentProgress.coerceIn(0.0, 1.0)
        )
    }

    private fun currentDistanceIntoSegment(
        mob: MobRuntimeState,
        route: RouteRuntime,
        segmentIndex: Int
    ): Double {
        val segment = route.segments[segmentIndex]
        val fromRouteProgress = mob.route.routeProgress - segment.cumulativeStart
        return if (fromRouteProgress in -1e-9..(segment.length + 1e-9)) {
            fromRouteProgress.coerceIn(0.0, segment.length)
        } else {
            segment.length * mob.route.segmentProgress.coerceIn(0.0, 1.0)
        }
    }

    private fun interpolate(a: Vec3, b: Vec3, t: Double): Vec3 =
        Vec3(
            a.x + (b.x - a.x) * t,
            a.y + (b.y - a.y) * t,
            a.z + (b.z - a.z) * t
        )
}
