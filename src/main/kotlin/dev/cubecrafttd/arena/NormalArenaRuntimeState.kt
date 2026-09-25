package dev.cubecrafttd.arena

import dev.cubecrafttd.economy.GoldmineRuntime
import dev.cubecrafttd.troop.*
import dev.cubecrafttd.player.AoEPotionRuntimeQueue
import java.util.UUID

data class NormalArenaRuntimeState(
    val queues:
        MutableMap<TeamId,TroopSendQueue> =
        mutableMapOf(
            TeamId.RED to
                TroopSendQueue(
                    TeamId.RED,12
                ),
            TeamId.BLUE to
                TroopSendQueue(
                    TeamId.BLUE,12
                )
        ),
    val spawnClocks:
        MutableMap<TeamId,TroopQueueSpawnClock>,
    val goldmines:
        MutableMap<UUID,GoldmineRuntime> =
        linkedMapOf(),
    val aoePulses:
        AoEPotionRuntimeQueue =
        AoEPotionRuntimeQueue()
) {
    companion object {
        fun withCadence(
            cadence: TroopSpawnCadence
        ): NormalArenaRuntimeState =
            NormalArenaRuntimeState(
                spawnClocks=
                    mutableMapOf(
                        TeamId.RED to
                            TroopQueueSpawnClock(
                                cadence
                            ),
                        TeamId.BLUE to
                            TroopQueueSpawnClock(
                                cadence
                            )
                    )
            )
    }
}
