package dev.cubecrafttd.arena

/**
 * Stable SplitMix64-based deterministic RNG.
 * This is an ENGINEERING determinism primitive, not a claim about CubeCraft's
 * original random-number generator implementation.
 */
class ArenaDeterministicRng(
    seed: Long
) {
    private var state: Long = seed

    fun nextLong(): Long {
        state += -7046029254386353131L
        var z = state
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        return z xor (z ushr 31)
    }

    fun nextInt(bound: Int): Int {
        require(bound > 0)
        val positive = nextLong().ushr(1)
        return (positive % bound.toLong()).toInt()
    }

    fun nextDouble(): Double {
        val bits = nextLong().ushr(11)
        return bits.toDouble() / 9007199254740992.0
    }

    fun snapshotState(): Long = state
}
