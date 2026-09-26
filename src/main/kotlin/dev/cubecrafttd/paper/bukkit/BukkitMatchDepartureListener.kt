package dev.cubecrafttd.paper.bukkit

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerKickEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import java.util.UUID

/**
 * Historical Tower Defence matches could continue after a player left. This
 * listener therefore does not award a win or force-end a live arena merely
 * because one participant disconnects.
 *
 * The departing player is removed from active live interaction immediately;
 * their durable pre-match snapshot stays pending for the recovery listener.
 * If no active participants remain at all, a one-tick-later Engineering cleanup
 * closes the orphaned arena without turning the disconnect into a gameplay win.
 */
class BukkitMatchDepartureListener(
    private val plugin: Plugin,
    private val controller:
        BukkitNormalArenaController
) : Listener {
    init {
        plugin.server.pluginManager
            .registerEvents(this,plugin)
    }

    @EventHandler(priority=EventPriority.MONITOR)
    fun onQuit(
        event: PlayerQuitEvent
    ) {
        depart(
            event.player.uniqueId,
            "quit"
        )
    }

    @EventHandler(
        priority=EventPriority.MONITOR,
        ignoreCancelled=true
    )
    fun onKick(
        event: PlayerKickEvent
    ) {
        depart(
            event.player.uniqueId,
            "kick"
        )
    }

    private fun depart(
        playerUuid: UUID,
        source: String
    ) {
        val report=
            controller
                .markPlayerDeparted(
                    playerUuid
                ) ?: return
        if(!report.newlyDeparted) {
            return
        }

        plugin.logger.info(
            "TD player departed: arena=" +
                report.arenaId +
                " player=" +
                playerUuid +
                " team=" +
                report.team +
                " source=" +
                source +
                " activeRemaining=" +
                report.activePlayersRemaining +
                " teammateManageableTowers=" +
                report.releasedTowerCount
        )

        if(report.allPlayersDeparted) {
            plugin.server.scheduler
                .runTask(
                    plugin,
                    Runnable {
                        controller
                            .stopIfNoActivePlayers(
                                report.arenaId
                            )
                    }
                )
        }
    }
}
