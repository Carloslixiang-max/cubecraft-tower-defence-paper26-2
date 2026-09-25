package dev.cubecrafttd.castle

import dev.cubecrafttd.arena.ArenaContext
import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.map.Vec3
import java.util.UUID

data class GuardEntitySpawnRequest(
    val guardInstanceId: Long,
    val team: TeamId,
    val position: Vec3
)

fun interface GuardEntitySpawnPort {
    fun spawn(
        request: GuardEntitySpawnRequest
    ): UUID
}

data class GuardBootstrapReport(
    val guardUuids: Set<UUID>,
    val guardsPerTeam: Map<TeamId,Int>
)

/**
 * Creates exactly the two configured Castle Guard runtime anchors per team.
 *
 * Visual Castle Guard tower builds stay in the map. The live entity created by
 * the Paper adapter is only an invisible geometry/ownership anchor for range,
 * LOS, teardown and Armageddon disable behavior.
 */
class GuardBootstrapService {
    fun bootstrap(
        context: ArenaContext,
        spawnPort: GuardEntitySpawnPort
    ): GuardBootstrapReport {
        check(
            context.entityIndex.guardsByUuid
                .isEmpty()
        ) {
            "Castle Guards already bootstrapped"
        }

        var nextId=1L
        val spawned=
            linkedSetOf<UUID>()

        TeamId.entries.forEach { team ->
            val anchors=
                context.mapRuntime
                    .guardAnchors[team]
                    .orEmpty()
            check(
                anchors.size==
                    RECOMMENDED_MATURE_GUARD_COUNT_PER_CASTLE
            ) {
                "$team requires exactly " +
                    "$RECOMMENDED_MATURE_GUARD_COUNT_PER_CASTLE Guard anchors; " +
                    "found ${anchors.size}"
            }

            anchors.forEach { anchor ->
                val id=nextId++
                val uuid=spawnPort.spawn(
                    GuardEntitySpawnRequest(
                        guardInstanceId=id,
                        team=team,
                        position=anchor.position
                    )
                )
                val guard=GuardRuntime(
                    identity=
                        GuardIdentity(
                            guardInstanceId=id,
                            team=team,
                            entityUuid=uuid
                        ),
                    nextAttackTick=
                        context.gameTick
                )
                context.entityIndex
                    .registerGuard(guard)
                spawned += uuid
            }
        }

        context.entityIndex.assertConsistent()
        return GuardBootstrapReport(
            guardUuids=spawned,
            guardsPerTeam=
                TeamId.entries
                    .associateWith {
                        context.entityIndex
                            .guardsByTeam
                            .getValue(it)
                            .size
                    }
        )
    }
}
