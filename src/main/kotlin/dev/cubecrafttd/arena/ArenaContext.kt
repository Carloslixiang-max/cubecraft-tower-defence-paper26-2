package dev.cubecrafttd.arena

import dev.cubecrafttd.map.MapRuntimeDefinition
import dev.cubecrafttd.castle.CastleRuntime
import dev.cubecrafttd.tower.visual.TowerBodyLedger
import java.util.UUID

class ArenaContext(
    val arenaId: ArenaId,
    val worldId: UUID,
    val mapRuntime: MapRuntimeDefinition,
    val redTeam: TeamRuntime = TeamRuntime(TeamId.RED),
    val blueTeam: TeamRuntime = TeamRuntime(TeamId.BLUE),
    val entityIndex: ArenaEntityIndex = ArenaEntityIndex(),
    val taskGroup: ArenaTaskGroup = ArenaTaskGroup(),
    val towerBodyLedger: TowerBodyLedger = TowerBodyLedger(),
    val rng: ArenaDeterministicRng = ArenaDeterministicRng(
        arenaId.value.hashCode().toLong() xor worldId.leastSignificantBits
    ),
    val castles: MutableMap<TeamId, CastleRuntime> = mutableMapOf(
        TeamId.RED to CastleRuntime(TeamId.RED),
        TeamId.BLUE to CastleRuntime(TeamId.BLUE)
    )
) {
    var state: ArenaState = ArenaState.CREATED
        internal set
    var gameTick: Long = 0L
        internal set

    fun advanceSyntheticTick(ticks: Long = 1L) {
        require(ticks >= 0)
        gameTick += ticks
    }

    fun assertClosedClean() {
        check(state == ArenaState.CLOSED)
        check(taskGroup.activeCount() == 0)
        check(entityIndex.isEmpty())
        check(towerBodyLedger.isEmpty())
    }
}
