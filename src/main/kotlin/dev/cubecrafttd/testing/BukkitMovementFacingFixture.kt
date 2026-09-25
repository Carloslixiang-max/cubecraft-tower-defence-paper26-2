package dev.cubecrafttd.testing

import dev.cubecrafttd.paper.bukkit.BukkitMovementFacingResolver
import kotlin.math.abs

object BukkitMovementFacingFixture {
    private fun close(
        actual: Float,
        expected: Float
    ): Boolean =
        abs(actual-expected)<0.001f

    fun run(): List<FixtureResult> =
        listOf(
            FixtureResult(
                "live-facing-positive-z-is-zero-yaw",
                close(
                    BukkitMovementFacingResolver
                        .yawDegrees(
                            0.0,0.0,
                            0.0,1.0,
                            33f
                        ),
                    0f
                )
            ),
            FixtureResult(
                "live-facing-positive-x-is-negative-90",
                close(
                    BukkitMovementFacingResolver
                        .yawDegrees(
                            0.0,0.0,
                            1.0,0.0,
                            33f
                        ),
                    -90f
                )
            ),
            FixtureResult(
                "live-facing-negative-x-is-positive-90",
                close(
                    BukkitMovementFacingResolver
                        .yawDegrees(
                            0.0,0.0,
                            -1.0,0.0,
                            33f
                        ),
                    90f
                )
            ),
            FixtureResult(
                "live-facing-negative-z-is-180",
                abs(
                    abs(
                        BukkitMovementFacingResolver
                            .yawDegrees(
                                0.0,0.0,
                                0.0,-1.0,
                                33f
                            )
                    )-180f
                )<0.001f
            ),
            FixtureResult(
                "live-facing-stationary-preserves-yaw",
                close(
                    BukkitMovementFacingResolver
                        .yawDegrees(
                            4.0,7.0,
                            4.0,7.0,
                            37.5f
                        ),
                    37.5f
                )
            )
        )
}
