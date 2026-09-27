package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.match.MatchOutcome
import dev.cubecrafttd.stats.MatchStatsSnapshot
import dev.cubecrafttd.ui.EngineeringMatchEndProjector
import dev.cubecrafttd.ui.HistoricalMatchEndLeaderboardProjector
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Server
import java.util.UUID

/**
 * Live Engineering Playtest match-end feedback.
 *
 * Inventory/menu cleanup happens before player snapshot restoration. The result
 * title/chat is emitted only after restoration so the player sees it in their
 * recovered pre-match state. Departed players are intentionally excluded by the
 * controller before this service is called.
 */
class BukkitMatchEndFeedbackService(
    private val server: Server
) {
    fun beforeRestore(
        players: Set<UUID>
    ) {
        players
            .sortedBy(UUID::toString)
            .forEach { uuid ->
                server.getPlayer(uuid)
                    ?.let { player ->
                        player.closeInventory()
                        player.sendActionBar(
                            Component.empty()
                        )
                    }
            }
    }

    fun present(
        outcome: MatchOutcome,
        teamByPlayer:
            Map<UUID,TeamId>,
        stats: MatchStatsSnapshot
    ) {
        val historicalTopPlayers=
            HistoricalMatchEndLeaderboardProjector
                .project(
                    stats
                )
        val showHistoricalTopPlayers=
            outcome !is MatchOutcome.Tie ||
                outcome.policy !=
                    TimeoutTiePolicy
                        .ENGINEERING_CUSTOM

        teamByPlayer
            .entries
            .sortedBy {
                it.key.toString()
            }
            .forEach {
                (uuid,team) ->
                val player=
                    server.getPlayer(uuid)
                        ?: return@forEach
                val view=
                    EngineeringMatchEndProjector
                        .project(
                            outcome,
                            team
                        )

                player.showTitle(
                    Title.title(
                        Component.text(
                            view.title
                        ),
                        Component.text(
                            view.subtitle
                        )
                    )
                )
                player.sendMessage(
                    Component.text(
                        view.chatLine
                    )
                )

                if(showHistoricalTopPlayers) {
                    player.sendMessage(
                        Component.empty()
                    )
                    player.sendMessage(
                        Component.text(
                            historicalTopPlayers
                                .heading,
                            NamedTextColor.GREEN
                        )
                    )
                    historicalTopPlayers
                        .entries
                        .forEach {
                            entry ->
                            val name=
                                server
                                    .getPlayer(
                                        entry.playerUuid
                                    )
                                    ?.name
                                    ?: server
                                        .getOfflinePlayer(
                                            entry.playerUuid
                                        )
                                        .name
                                    ?: entry
                                        .playerUuid
                                        .toString()
                                        .take(8)

                            val hover=
                                Component.text(
                                    "Towers placed: ",
                                    NamedTextColor
                                        .LIGHT_PURPLE
                                ).append(
                                    Component.text(
                                        entry.towersPlaced
                                            .toString(),
                                        NamedTextColor.GOLD
                                    )
                                ).append(
                                    Component.newline()
                                ).append(
                                    Component.text(
                                        "Troops sent: ",
                                        NamedTextColor
                                            .LIGHT_PURPLE
                                    )
                                ).append(
                                    Component.text(
                                        entry.troopsSent
                                            .toString(),
                                        NamedTextColor.GOLD
                                    )
                                ).append(
                                    Component.newline()
                                ).append(
                                    Component.text(
                                        "Enemies killed: ",
                                        NamedTextColor
                                            .LIGHT_PURPLE
                                    )
                                ).append(
                                    Component.text(
                                        entry.enemiesKilled
                                            .toString(),
                                        NamedTextColor.GOLD
                                    )
                                ).append(
                                    Component.newline()
                                ).append(
                                    Component.newline()
                                ).append(
                                    Component.text(
                                        "Overall score: ",
                                        NamedTextColor
                                            .LIGHT_PURPLE
                                    )
                                ).append(
                                    Component.text(
                                        entry.overallScore
                                            .toString(),
                                        NamedTextColor.GOLD
                                    )
                                )

                            player.sendMessage(
                                Component.text(
                                    "${entry.rank}: ",
                                    NamedTextColor.YELLOW
                                ).append(
                                    Component.text(
                                        name,
                                        NamedTextColor.YELLOW
                                    ).hoverEvent(
                                        HoverEvent.showText(
                                            hover
                                        )
                                    )
                                )
                            )
                        }
                }
            }
    }
}
