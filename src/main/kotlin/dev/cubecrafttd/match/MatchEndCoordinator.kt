package dev.cubecrafttd.match

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.stats.*
import java.util.UUID

data class MatchEndReport(
    val outcome: MatchOutcome,
    val stats: MatchStatsSnapshot,
    val teardown: ArenaTeardownReport
)

class MatchEndCoordinator(
    private val stats:
        MatchStatsRecorder,
    private val teardownService:
        ArenaTeardownService
) {
    fun finish(
        context: ArenaContext,
        outcome: MatchOutcome
    ): MatchEndReport {
        val winnerTeam =
            (outcome as?
                MatchOutcome.Winner)
                ?.team

        fun recordTeam(
            team: TeamId,
            players: Set<UUID>
        ) {
            players.forEach { player ->
                val won = when {
                    outcome is MatchOutcome.Tie ->
                        null
                    winnerTeam == null ->
                        null
                    else ->
                        team == winnerTeam
                }
                stats.recordOutcome(
                    player,outcome,won
                )
            }
        }

        recordTeam(
            TeamId.RED,
            context.redTeam.players
        )
        recordTeam(
            TeamId.BLUE,
            context.blueTeam.players
        )

        val snapshot =
            stats.snapshot()
        val teardown =
            teardownService.teardown(context)

        return MatchEndReport(
            outcome,snapshot,teardown
        )
    }
}
