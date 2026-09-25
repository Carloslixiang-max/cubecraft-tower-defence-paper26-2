package dev.cubecrafttd.testing

import dev.cubecrafttd.truth.EngineeringPlaytestProfile
import dev.cubecrafttd.truth.RuntimeFallbackConfigV1

/**
 * Canonical fully-populated engineering config used by pure-domain fixtures.
 * It is deliberately the same explicit playtest profile exposed to operators;
 * none of its engineering-only values are original CubeCraft truth.
 */
object CompleteFallbackFixtureFactory {
    fun create():
        RuntimeFallbackConfigV1 =
        EngineeringPlaytestProfile.create()
}
