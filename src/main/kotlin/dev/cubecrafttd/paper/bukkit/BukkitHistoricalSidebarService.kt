package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.ui.HistoricalMatchSidebarView
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Scoreboard
import java.util.UUID

/**
 * Historical sidebar adapter with conservative scoreboard ownership.
 *
 * It only takes over players currently using Bukkit's main scoreboard. If
 * another plugin already owns a custom scoreboard, TD keeps its action-bar
 * fallback and does not overwrite that UI. Any offline clear is completed on
 * the next join; the only scoreboard we ever restore is mainScoreboard because
 * that is the only pre-match state this service is allowed to replace.
 */
class BukkitHistoricalSidebarService(
    private val plugin: Plugin
) : Listener {
    private val server=
        plugin.server
    private val activeBoards=
        linkedMapOf<UUID,Scoreboard>()
    private val customScoreboardOptOut=
        linkedSetOf<UUID>()
    private val pendingMainRestore=
        linkedSetOf<UUID>()

    init {
        server.pluginManager
            .registerEvents(
                this,
                plugin
            )
    }

    fun push(
        playerUuid: UUID,
        view: HistoricalMatchSidebarView
    ): Boolean {
        val player=
            server.getPlayer(
                playerUuid
            ) ?: return false
        val manager=
            server.scoreboardManager

        if(
            pendingMainRestore.remove(
                playerUuid
            )
        ) {
            player.scoreboard=
                manager.mainScoreboard
        }

        if(
            playerUuid in
                customScoreboardOptOut
        ) {
            return false
        }

        var board=
            activeBoards[
                playerUuid
            ]
        if(board==null) {
            if(
                player.scoreboard !==
                    manager.mainScoreboard
            ) {
                customScoreboardOptOut +=
                    playerUuid
                return false
            }

            board=
                manager.newScoreboard
            val objective=
                board.registerNewObjective(
                    "ctd",
                    Criteria.DUMMY,
                    Component.text(
                        view.title,
                        NamedTextColor.YELLOW
                    )
                )
            objective.displaySlot=
                DisplaySlot.SIDEBAR
            activeBoards[
                playerUuid
            ]=board
            player.scoreboard=board
        }

        val objective=
            board.getObjective(
                "ctd"
            ) ?: return false

        board.entries
            .toList()
            .forEach(
                board::resetScores
            )

        listOf(
            "§r " to 4,
            "§f${view.coins} §aCoins" to 3,
            "§f${view.exp} §bEXP" to 2,
            "§r  " to 1,
            "§e${view.footer}" to 0
        ).forEach {
            (line,score) ->
            objective
                .getScore(
                    line
                )
                .score=score
        }
        return true
    }

    fun clear(
        playerUuid: UUID
    ) {
        customScoreboardOptOut
            .remove(
                playerUuid
            )
        val board=
            activeBoards.remove(
                playerUuid
            ) ?: return

        val player=
            server.getPlayer(
                playerUuid
            )
        if(player==null) {
            pendingMainRestore +=
                playerUuid
        } else if(
            player.scoreboard ===
                board
        ) {
            player.scoreboard=
                server
                    .scoreboardManager
                    .mainScoreboard
        }

        board.getObjective(
            "ctd"
        )?.unregister()
    }

    fun clear(
        playerUuids: Collection<UUID>
    ) {
        playerUuids.forEach(
            ::clear
        )
    }

    @EventHandler
    fun onJoin(
        event: PlayerJoinEvent
    ) {
        val uuid=
            event.player.uniqueId
        if(
            pendingMainRestore.remove(
                uuid
            )
        ) {
            event.player.scoreboard=
                server
                    .scoreboardManager
                    .mainScoreboard
        }
    }
}
