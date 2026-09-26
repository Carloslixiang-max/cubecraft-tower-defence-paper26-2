package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.match.MatchOutcome
import dev.cubecrafttd.ui.EngineeringMatchEndProjector
import net.kyori.adventure.text.Component
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
            Map<UUID,TeamId>
    ) {
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
            }
    }
}
