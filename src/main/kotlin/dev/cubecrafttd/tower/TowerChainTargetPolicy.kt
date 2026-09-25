package dev.cubecrafttd.tower

/**
 * The original Zeus/Turret next-bounce selection rule has not been recovered.
 * These policies are engineering choices and must never be reported as
 * original CubeCraft truth.
 */
enum class TowerChainTargetPolicy {
    ROUTE_PROGRESS_FORWARD_ENGINEERING,
    NEAREST_FROM_PREVIOUS_ENGINEERING
}
