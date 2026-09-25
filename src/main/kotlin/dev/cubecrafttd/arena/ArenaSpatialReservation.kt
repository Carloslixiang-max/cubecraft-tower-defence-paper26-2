package dev.cubecrafttd.arena

import dev.cubecrafttd.map.*
import java.util.UUID

const val ENGINEERING_ARENA_ISOLATION_PADDING_BLOCKS:
    Double = 16.0

data class ArenaSpatialEnvelope(
    val minX: Double,
    val minY: Double,
    val minZ: Double,
    val maxX: Double,
    val maxY: Double,
    val maxZ: Double
) {
    init {
        require(minX<=maxX)
        require(minY<=maxY)
        require(minZ<=maxZ)
    }

    fun overlaps(
        other: ArenaSpatialEnvelope
    ): Boolean =
        !(maxX < other.minX ||
            other.maxX < minX ||
            maxY < other.minY ||
            other.maxY < minY ||
            maxZ < other.minZ ||
            other.maxZ < minZ)
}

object ArenaSpatialEnvelopeResolver {
    fun fromMap(
        map: MapRuntimeDefinition,
        paddingBlocks: Double =
            ENGINEERING_ARENA_ISOLATION_PADDING_BLOCKS
    ): ArenaSpatialEnvelope {
        require(paddingBlocks>=0.0)

        val points=
            mutableListOf<Vec3>()

        fun add(
            x: Double,
            y: Double,
            z: Double
        ) {
            points += Vec3(x,y,z)
        }

        fun add(
            p: BlockPos
        ) = add(
            p.x.toDouble(),
            p.y.toDouble(),
            p.z.toDouble()
        )

        map.routesById.values
            .forEach { route ->
                points += route.nodes
            }

        map.placementRegions.values
            .forEach { region ->
                region.legalBaseBlocks
                    .forEach(::add)
            }

        map.explicitTowerSpots
            .forEach { spot ->
                AdvancedPlacementResolver
                    .footprintCells(
                        spot.center,
                        spot.footprint
                    )
                    .forEach(::add)
            }

        map.teamSpawns.values
            .forEach { spawn ->
                spawn.mobSpawn
                    ?.let(points::add)
                spawn.playerSpawn
                    ?.let(points::add)
            }

        map.terminalCrossSections.values
            .forEach { terminal ->
                add(
                    terminal
                        .centerlineEndpoint
                )
                terminal.cells
                    .forEach(::add)
            }

        map.castleContactCandidates.values
            .forEach { candidate ->
                candidate.boundaryCells
                    .forEach(::add)
            }

        map.castleContactRegionsExact.values
            .filterNotNull()
            .forEach { region ->
                region.cells
                    .forEach(::add)
            }

        map.guardAnchors.values
            .flatten()
            .forEach { anchor ->
                points += anchor.position
            }

        check(points.isNotEmpty()) {
            "Arena map has no spatial geometry"
        }

        return ArenaSpatialEnvelope(
            minX=
                points.minOf { it.x } -
                    paddingBlocks,
            minY=
                points.minOf { it.y } -
                    paddingBlocks,
            minZ=
                points.minOf { it.z } -
                    paddingBlocks,
            maxX=
                points.maxOf { it.x } +
                    paddingBlocks,
            maxY=
                points.maxOf { it.y } +
                    paddingBlocks,
            maxZ=
                points.maxOf { it.z } +
                    paddingBlocks
        )
    }
}

data class ArenaSpatialReservation(
    val arenaId: ArenaId,
    val worldId: UUID,
    val players: Set<UUID>,
    val envelope: ArenaSpatialEnvelope
) {
    init {
        require(players.isNotEmpty())
    }

    companion object {
        fun fromMap(
            arenaId: ArenaId,
            worldId: UUID,
            players: Set<UUID>,
            map: MapRuntimeDefinition
        ): ArenaSpatialReservation =
            ArenaSpatialReservation(
                arenaId=arenaId,
                worldId=worldId,
                players=players.toSet(),
                envelope=
                    ArenaSpatialEnvelopeResolver
                        .fromMap(map)
            )
    }
}

sealed interface ArenaIsolationConflict {
    data class ArenaIdAlreadyReserved(
        val arenaId: ArenaId
    ) : ArenaIsolationConflict

    data class PlayerAlreadyReserved(
        val playerUuid: UUID,
        val existingArenaId: ArenaId
    ) : ArenaIsolationConflict

    data class SpatialOverlap(
        val existingArenaId: ArenaId,
        val worldId: UUID
    ) : ArenaIsolationConflict
}

sealed interface ArenaReservationResult {
    data object Accepted :
        ArenaReservationResult

    data class Rejected(
        val conflicts:
            List<ArenaIsolationConflict>
    ) : ArenaReservationResult {
        init {
            require(conflicts.isNotEmpty())
        }

        fun summary(): String =
            conflicts.joinToString(
                separator="; "
            ) { conflict ->
                when(conflict) {
                    is ArenaIsolationConflict
                        .ArenaIdAlreadyReserved ->
                        "arenaId=" +
                            conflict.arenaId.value +
                            " already reserved"
                    is ArenaIsolationConflict
                        .PlayerAlreadyReserved ->
                        "player=" +
                            conflict.playerUuid +
                            " already in " +
                            conflict
                                .existingArenaId
                                .value
                    is ArenaIsolationConflict
                        .SpatialOverlap ->
                        "spatial overlap with " +
                            conflict
                                .existingArenaId
                                .value +
                            " in world=" +
                            conflict.worldId
                }
            }
    }
}

/**
 * Operational isolation for concurrent live arenas.
 *
 * The 16-block envelope padding is an Engineering safety margin, not original
 * CubeCraft gameplay truth. It prevents known live adapters, tower bodies,
 * guards and player state from being shared by overlapping arena instances.
 */
class ArenaIsolationRegistry {
    private val reservations=
        linkedMapOf<
            ArenaId,
            ArenaSpatialReservation
        >()

    fun reserve(
        request: ArenaSpatialReservation
    ): ArenaReservationResult {
        val conflicts=
            mutableListOf<
                ArenaIsolationConflict
            >()

        if(
            request.arenaId in
                reservations
        ) {
            conflicts +=
                ArenaIsolationConflict
                    .ArenaIdAlreadyReserved(
                        request.arenaId
                    )
        }

        reservations.values
            .sortedBy {
                it.arenaId.value
            }
            .forEach { existing ->
                request.players
                    .intersect(
                        existing.players
                    )
                    .sortedBy(UUID::toString)
                    .forEach { player ->
                        conflicts +=
                            ArenaIsolationConflict
                                .PlayerAlreadyReserved(
                                    player,
                                    existing.arenaId
                                )
                    }

                if(
                    request.worldId==
                        existing.worldId &&
                    request.envelope
                        .overlaps(
                            existing.envelope
                        )
                ) {
                    conflicts +=
                        ArenaIsolationConflict
                            .SpatialOverlap(
                                existing.arenaId,
                                request.worldId
                            )
                }
            }

        if(conflicts.isNotEmpty()) {
            return ArenaReservationResult
                .Rejected(conflicts)
        }

        reservations[
            request.arenaId
        ]=request
        return ArenaReservationResult.Accepted
    }

    fun release(
        arenaId: ArenaId
    ): Boolean =
        reservations.remove(arenaId)!=null

    fun activeCount(): Int =
        reservations.size

    fun snapshot():
        List<ArenaSpatialReservation> =
        reservations.values
            .sortedBy {
                it.arenaId.value
            }
}

