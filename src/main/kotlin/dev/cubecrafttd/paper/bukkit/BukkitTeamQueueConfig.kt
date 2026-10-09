package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.match.TeamRosterPolicy
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration

object BukkitTeamQueueConfig {
    fun teamSize(config: ConfigurationSection): Int {
        val size = (config.get("queue.team-size") ?: 6) as? Int
            ?: error("queue.team-size must be an integer")
        require(size in 1..TeamRosterPolicy.MAX_TEAM_PLAYERS) { "queue.team-size must be 1..6" }
        return size
    }

    /** Real Paper YAML semantics; no online-player matchmaking claim. */
    fun smoke() {
        fun config(value: String?) = YamlConfiguration().also {
            if (value != null) it.loadFromString("queue:\n  team-size: $value\n")
        }
        check(teamSize(config(null)) == 6)
        check(teamSize(config("6")) == 6 && teamSize(config("1")) == 1)
        listOf("0", "7", "6.5", "'6'", "false").forEach { value ->
            check(runCatching { teamSize(config(value)) }.isFailure) { "Invalid queue profile was accepted: $value" }
        }
    }
}
