package dev.cubecrafttd.testing

import dev.cubecrafttd.map.*
import dev.cubecrafttd.mob.RouteProgressSampler

object RouteProgressSamplerFixture {
    fun run():List<FixtureResult> {
        val route=RouteRuntime.compile(
            "r",
            listOf(
                Vec3(0.0,0.0,0.0),
                Vec3(3.0,0.0,0.0),
                Vec3(3.0,0.0,4.0)
            )
        )
        val a=
            RouteProgressSampler.sample(
                route,2.0
            )
        val b=
            RouteProgressSampler.sample(
                route,5.0
            )
        val end=
            RouteProgressSampler.sample(
                route,99.0
            )

        return listOf(
            FixtureResult(
                "route-progress-sample-first-segment",
                a.routeState.segmentIndex==0 &&
                    a.position==
                        Vec3(2.0,0.0,0.0)
            ),
            FixtureResult(
                "route-progress-sample-second-segment",
                b.routeState.segmentIndex==1 &&
                    b.position==
                        Vec3(3.0,0.0,2.0)
            ),
            FixtureResult(
                "route-progress-sample-clamps-terminal",
                end.routeState.routeProgress==
                    7.0 &&
                    end.position==
                        Vec3(3.0,0.0,4.0)
            )
        )
    }
}
