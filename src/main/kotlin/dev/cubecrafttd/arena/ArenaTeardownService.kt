package dev.cubecrafttd.arena

import dev.cubecrafttd.tower.lifecycle.TowerBodyMutationService
import dev.cubecrafttd.tower.visual.TowerBodyConflict
import java.util.UUID

fun interface TrackedEntityRemovalPort {
    fun remove(entityUuid: UUID): Boolean
}

fun interface TrackedEntityPresencePort {
    fun exists(entityUuid: UUID): Boolean
}

fun interface MatchPlayerRestorePort {
    /**
     * true = restored now.
     * false = player remains pending for reconnect/recovery journal.
     */
    fun restore(playerUuid: UUID): Boolean
}

data class ArenaTeardownReport(
    val towerBodyConflicts:
        List<TowerBodyConflict>,
    val failedEntityRemovals:
        Set<UUID>,
    val pendingPlayerRestores:
        Set<UUID>,
    val removedEntityCount: Int,
    val restoredPlayerCount: Int,
    val survivingTrackedEntities:
        Set<UUID> = emptySet()
) {
    val externalResiduePossible: Boolean
        get() =
            failedEntityRemovals.isNotEmpty() ||
                survivingTrackedEntities.isNotEmpty()

    val fullyCleanNow: Boolean
        get() =
            towerBodyConflicts.isEmpty() &&
            failedEntityRemovals.isEmpty() &&
            survivingTrackedEntities.isEmpty() &&
            pendingPlayerRestores.isEmpty()
}

class ArenaTeardownService(
    private val bodyMutation:
        TowerBodyMutationService,
    private val entityRemoval:
        TrackedEntityRemovalPort,
    private val playerRestore:
        MatchPlayerRestorePort,
    private val entityPresence:
        TrackedEntityPresencePort =
            TrackedEntityPresencePort { false }
) {
    fun teardown(
        context: ArenaContext
    ): ArenaTeardownReport {
        if (context.state ==
            ArenaState.CLOSED
        ) {
            return ArenaTeardownReport(
                emptyList(),emptySet(),
                emptySet(),0,0
            )
        }

        context.state = ArenaState.ENDING

        // Stop mutation sources before touching world/body state.
        context.taskGroup.close()

        val bodyConflicts =
            mutableListOf<TowerBodyConflict>()

        context.entityIndex
            .towersByInstanceId
            .keys
            .toList()
            .sortedByDescending { it.value }
            .forEach { id ->
                val result =
                    bodyMutation.remove(
                        id.value
                    )
                bodyConflicts +=
                    result.conflictReport
                        .conflicts
            }

        val entityIds =
            linkedSetOf<UUID>().apply {
                addAll(
                    context.entityIndex
                        .mobsByUuid.keys
                )
                addAll(
                    context.entityIndex
                        .guardsByUuid.keys
                )
                addAll(
                    context.entityIndex
                        .transientDisplays
                )
                addAll(
                    context.entityIndex
                        .projectiles
                )
            }

        var removed=0
        val failed=
            linkedSetOf<UUID>()
        entityIds.forEach { uuid ->
            val ok=runCatching {
                entityRemoval.remove(uuid)
            }.getOrDefault(false)
            if (ok) removed++
            else failed += uuid
        }

        val players =
            linkedSetOf<UUID>().apply {
                addAll(
                    context.redTeam.players
                )
                addAll(
                    context.blueTeam.players
                )
            }
        var restored=0
        val pending=
            linkedSetOf<UUID>()
        players.forEach { uuid ->
            val ok=runCatching {
                playerRestore.restore(uuid)
            }.getOrDefault(false)
            if (ok) restored++
            else pending += uuid
        }

        val surviving=
            entityIds
                .filterTo(linkedSetOf()) {
                    uuid ->
                    runCatching {
                        entityPresence.exists(
                            uuid
                        )
                    }.getOrDefault(true)
                }

        context.entityIndex.clearAll()
        check(context.towerBodyLedger.isEmpty()) {
            "TowerBodyLedger must be empty after teardown"
        }
        context.state = ArenaState.CLOSED

        return ArenaTeardownReport(
            towerBodyConflicts=bodyConflicts,
            failedEntityRemovals=failed,
            pendingPlayerRestores=pending,
            removedEntityCount=removed,
            restoredPlayerCount=restored,
            survivingTrackedEntities=
                surviving
        )
    }
}
