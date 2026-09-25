package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.MapAuthenticity
import dev.cubecrafttd.map.schematic.*
import dev.cubecrafttd.mob.*
import java.util.UUID
import kotlin.math.abs

object FarmMobMovementFixture {
    fun run(bytes: ByteArray): List<FixtureResult> {
        val volume = SpongeSchematicV2Decoder().decode(bytes)
        val observation = SchematicMarkerClassifier(
            SchematicMarkerConfig.farm()
        ).classify(volume)
        val runtime = SchematicObservationRuntimeCompiler().compile(
            volume,
            observation,
            SchematicImportMetadata(
                "farm_improved_2022_candidate",
                "movement-fixture-v1",
                MapAuthenticity.COMMUNITY_IMPROVEMENT_BY_ORIGINAL_TRACK_AUTHOR,
                productionOriginal = false
            )
        )

        val route = runtime.routesById
            .filterKeys { it.startsWith("blue_") }
            .values.first()

        val mob = MobRuntimeState(
            identity = MobIdentity(
                MobInstanceId(500),
                UUID.fromString("00000000-0000-0000-0000-000000005000"),
                "fixture_mob",
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                TeamId.BLUE,
                level = 1
            ),
            route = MobRouteState(
                routeId = route.routeId,
                segmentIndex = 0,
                segmentProgress = 0.0,
                routeProgress = 0.0
            ),
            combat = MobCombatState(
                health = 100.0,
                maxHealth = 100.0,
                lifecycle = MobLifecycleState.MOVING
            )
        )

        val start = MobRouteMovementService.position(mob, route)
        val first = MobRouteMovementService.advance(mob, route, 17.0)
        val afterFirstProgress = mob.route.routeProgress

        var loops = 0
        var last = first
        while (!last.reachedTerminal && loops < 1000) {
            last = MobRouteMovementService.advance(mob, route, 3.25)
            loops++
        }

        val terminal = route.nodes.last()

        return listOf(
            FixtureResult(
                "farm-movement-start-position",
                start == route.nodes.first()
            ),
            FixtureResult(
                "farm-movement-monotonic-progress",
                afterFirstProgress > 0.0 &&
                    mob.route.routeProgress >= afterFirstProgress
            ),
            FixtureResult(
                "farm-movement-reaches-terminal",
                last.reachedTerminal &&
                    abs(last.position.x - terminal.x) < 1e-9 &&
                    abs(last.position.y - terminal.y) < 1e-9 &&
                    abs(last.position.z - terminal.z) < 1e-9
            ),
            FixtureResult(
                "farm-movement-progress-equals-route-length",
                abs(mob.route.routeProgress - route.totalLength) < 1e-9
            ),
            FixtureResult(
                "farm-movement-does-not-auto-invent-castle-rate",
                mob.combat.lifecycle == MobLifecycleState.MOVING
            )
        )
    }
}
