package dev.cubecrafttd.match

import java.util.UUID

data class PregameTeamStartResolution(
    val armageddon: HistoricalPregameArmageddonResolved,
    val pricing: HistoricalPregamePricingResolved
)

/** Votes from players still waiting outside this roster cannot affect its match. */
object PregameTeamStartResolver {
    fun settings(
        roster: EngineeringTeamQueueMatch,
        preferences: Map<UUID, dev.cubecrafttd.ui.PregamePreferenceState>
    ): Map<UUID, dev.cubecrafttd.ui.PlayerMatchSettings> = roster.players.mapNotNull { player ->
        preferences[player]?.settings?.copy()?.let { player to it }
    }.toMap()

    fun resolve(
        roster: EngineeringTeamQueueMatch,
        armageddonVotes: Map<UUID, HistoricalPregameArmageddonVoteOption>,
        pricingVotes: Map<UUID, HistoricalPregamePricingVoteOption>,
        runnableTypes: Set<ArmageddonType>,
        randomChoice: ArmageddonType
    ): PregameTeamStartResolution {
        val eligible = roster.players.toSet()
        val armageddon = HistoricalPregameArmageddonVoteRuntime(eligible, runnableTypes)
        val pricing = HistoricalPregamePricingVoteRuntime(eligible)
        roster.players.forEach { player ->
            armageddonVotes[player]?.let { armageddon.cast(player, it) }
            pricingVotes[player]?.let { pricing.cast(player, it) }
        }
        return PregameTeamStartResolution(armageddon.resolve(randomChoice), pricing.resolve())
    }
}
