package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.paper.LiveActionBarPort
import net.kyori.adventure.text.Component
import org.bukkit.Server
import java.util.UUID

class BukkitActionBarPort(
    private val server: Server
) : LiveActionBarPort {
    override fun send(
        playerUuid: UUID,
        text: String
    ) {
        server.getPlayer(playerUuid)
            ?.sendActionBar(Component.text(text))
    }
}
