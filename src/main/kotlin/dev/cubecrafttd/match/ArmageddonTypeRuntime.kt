package dev.cubecrafttd.match

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.castle.CastleState
import dev.cubecrafttd.mob.MobInstanceId
import dev.cubecrafttd.truth.ResolvedTruth
import java.util.UUID

data class HordeSpawnOrder(
    val mobId: String,
    val level: Int,
    val quantityPerTeam: Int
) {
    init {
        require(level > 0)
        require(quantityPerTeam > 0)
    }
}

data class HordeArmageddonConfig(
    val waveIntervalTicks: ResolvedTruth<Long>,
    val waves: List<HordeSpawnOrder>
) {
    init {
        require(waveIntervalTicks.value > 0L)
        require(waves.isNotEmpty())
    }
}

data class HordeSpawnRequest(
    val attackedTeam: TeamId,
    val mobId: String,
    val level: Int,
    val quantity: Int,
    val waveIndex: Int
)

class HordeArmageddonEngine(
    private val config: HordeArmageddonConfig,
    startTick: Long
) {
    private var nextWaveTick: Long = startTick
    private var waveIndex: Int = 0

    fun tick(gameTick: Long): List<HordeSpawnRequest> {
        if (waveIndex >= config.waves.size ||
            gameTick < nextWaveTick
        ) return emptyList()

        val out = mutableListOf<HordeSpawnRequest>()
        while (
            waveIndex < config.waves.size &&
            gameTick >= nextWaveTick
        ) {
            val wave = config.waves[waveIndex]
            TeamId.entries.forEach { team ->
                out += HordeSpawnRequest(
                    attackedTeam = team,
                    mobId = wave.mobId,
                    level = wave.level,
                    quantity = wave.quantityPerTeam,
                    waveIndex = waveIndex
                )
            }
            waveIndex++
            nextWaveTick +=
                config.waveIntervalTicks.value
        }
        return out
    }

    fun finished(): Boolean =
        waveIndex >= config.waves.size
}

data class LightningArmageddonConfig(
    val strikeIntervalTicks: ResolvedTruth<Long>,
    val towersPerTeamPerStrike:
        ResolvedTruth<Int>
) {
    init {
        require(strikeIntervalTicks.value > 0L)
        require(towersPerTeamPerStrike.value > 0)
    }
}

data class LightningStrikeSelection(
    val gameTick: Long,
    val towerIds: List<Long>
)

class LightningArmageddonEngine(
    private val config: LightningArmageddonConfig,
    startTick: Long
) {
    private var nextStrikeTick: Long = startTick

    fun tick(
        context: ArenaContext
    ): LightningStrikeSelection? {
        if (context.gameTick < nextStrikeTick) {
            return null
        }

        val selected = mutableListOf<Long>()
        TeamId.entries.forEach { team ->
            val available = context.entityIndex
                .towersByTeam.getValue(team)
                .map { it.value }
                .sorted()
                .toMutableList()

            val count = minOf(
                config.towersPerTeamPerStrike.value,
                available.size
            )
            repeat(count) {
                val index =
                    context.rng.nextInt(
                        available.size
                    )
                selected +=
                    available.removeAt(index)
            }
        }

        var next =
            nextStrikeTick +
                config.strikeIntervalTicks.value
        while (next <= context.gameTick) {
            next +=
                config.strikeIntervalTicks.value
        }
        nextStrikeTick = next

        return LightningStrikeSelection(
            context.gameTick,
            selected.sorted()
        )
    }
}

const val RECOMMENDED_WITHER_MAX_HEALTH:
    Double = 5000.0

enum class WitherTowerTargetPolicy {
    /**
     * Explicit engineering option. "When near a tower" is documented, but the
     * exact original selection tie-break is not recovered.
     */
    NEAREST_IN_RANGE_ENGINEERING,

    /**
     * Explicit engineering option. Random is not claimed to be original.
     */
    RANDOM_IN_RANGE_ENGINEERING
}

data class WitherArmageddonConfig(
    val speedBlocksPerTick:
        ResolvedTruth<Double>,
    val regenerationHealthPerSecond:
        ResolvedTruth<Double>,

    val firstTowerSkullDelayTicks:
        ResolvedTruth<Long>,
    val towerSkullIntervalTicks:
        ResolvedTruth<Long>,
    val towerSkullRangeBlocks:
        ResolvedTruth<Double>,
    val towerTargetPolicy:
        ResolvedTruth<WitherTowerTargetPolicy>,

    val firstTrailWitherSkeletonDelayTicks:
        ResolvedTruth<Long>,
    val trailWitherSkeletonIntervalTicks:
        ResolvedTruth<Long>,
    val trailWitherSkeletonsPerEvent:
        ResolvedTruth<Int>,
    /**
     * Engineering bridge: level 4/5 Skeleton forms are Wither Skeletons in the
     * recovered troop data. This does NOT claim Armageddon trail stats equal a
     * player-sent Skeleton level.
     */
    val trailWitherSkeletonLevel:
        ResolvedTruth<Int>
) {
    init {
        require(speedBlocksPerTick.value >= 0.0)
        require(
            regenerationHealthPerSecond.value >= 0.0
        )
        require(
            firstTowerSkullDelayTicks.value >= 0L
        )
        require(towerSkullIntervalTicks.value > 0L)
        require(towerSkullRangeBlocks.value >= 0.0)
        require(
            firstTrailWitherSkeletonDelayTicks
                .value >= 0L
        )
        require(
            trailWitherSkeletonIntervalTicks.value > 0L
        )
        require(
            trailWitherSkeletonsPerEvent.value > 0
        )
        require(
            trailWitherSkeletonLevel.value in 4..5
        )
    }
}

data class WitherArmageddonState(
    val attackedTeam: TeamId,
    var health: Double =
        RECOMMENDED_WITHER_MAX_HEALTH,
    var routeProgress: Double = 0.0,
    var nextTowerSkullTick: Long,
    var nextTrailSpawnTick: Long,
    var reachedCastle: Boolean = false
)

data class WitherTickEvents(
    val fireTowerSkull: Boolean,
    val spawnWitherSkeletonTrail: Boolean,
    val reachedCastleThisTick: Boolean
)

class WitherArmageddonEngine(
    private val config: WitherArmageddonConfig
) {
    fun tick(
        state: WitherArmageddonState,
        gameTick: Long,
        routeLength: Double,
        targetCastle:
            dev.cubecrafttd.castle.CastleRuntime
    ): WitherTickEvents {
        if (state.reachedCastle) {
            return WitherTickEvents(
                false,false,false
            )
        }

        val regen =
            config.regenerationHealthPerSecond
                .value / 20.0
        state.health =
            (state.health + regen)
                .coerceAtMost(
                    RECOMMENDED_WITHER_MAX_HEALTH
                )

        state.routeProgress =
            (
                state.routeProgress +
                    config.speedBlocksPerTick.value
            ).coerceAtMost(routeLength)

        val skull =
            gameTick >= state.nextTowerSkullTick
        if (skull) {
            do {
                state.nextTowerSkullTick +=
                    config.towerSkullIntervalTicks
                        .value
            } while (
                state.nextTowerSkullTick <=
                    gameTick
            )
        }

        val trail =
            gameTick >= state.nextTrailSpawnTick
        if (trail) {
            do {
                state.nextTrailSpawnTick +=
                    config
                        .trailWitherSkeletonIntervalTicks
                        .value
            } while (
                state.nextTrailSpawnTick <=
                    gameTick
            )
        }

        var reachedNow = false
        if (
            state.routeProgress >= routeLength &&
            !state.reachedCastle
        ) {
            state.reachedCastle = true
            reachedNow = true
            targetCastle.health = 0.0
            targetCastle.state =
                CastleState.DESTROYED
        }

        return WitherTickEvents(
            fireTowerSkull = skull,
            spawnWitherSkeletonTrail = trail,
            reachedCastleThisTick = reachedNow
        )
    }

    fun damage(
        state: WitherArmageddonState,
        amount: Double
    ): Double {
        require(amount >= 0.0)
        state.health =
            (state.health - amount)
                .coerceAtLeast(0.0)
        return state.health
    }
}
