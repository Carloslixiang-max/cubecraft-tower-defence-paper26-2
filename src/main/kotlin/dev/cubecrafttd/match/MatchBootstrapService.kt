package dev.cubecrafttd.match

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.player.PlayerRecoveryCoordinator
import dev.cubecrafttd.progression.*
import java.util.UUID

data class MatchBootstrapRequest(
    val redPlayers: Set<UUID>,
    val bluePlayers: Set<UUID>,
    val preset: MatchRulePreset,
    val firstGoldmineIncomeTick: Long,
    val economyTransactionBaseId: Long
) {
    init {
        require(
            redPlayers.intersect(
                bluePlayers
            ).isEmpty()
        ) {
            "Player cannot be on both teams"
        }
        require(
            firstGoldmineIncomeTick >= 0L
        )
    }
}

data class MatchBootstrapReport(
    val capturedPlayers: Set<UUID>,
    val seededPlayers: Set<UUID>,
    val session: MatchSessionState
)

class MatchBootstrapService(
    private val recovery:
        PlayerRecoveryCoordinator,
    private val ledger:
        EconomyLedger,
    private val progression:
        TroopProgressionService
) {
    fun prepare(
        context: ArenaContext,
        runtime:
            NormalArenaRuntimeState,
        request: MatchBootstrapRequest
    ): MatchBootstrapReport {
        check(
            context.state ==
                ArenaState.CREATED
        ) {
            "Arena must be CREATED"
        }

        val allPlayers=
            linkedSetOf<UUID>().apply {
                addAll(request.redPlayers)
                addAll(request.bluePlayers)
            }
        check(allPlayers.isNotEmpty()) {
            "Match requires at least one player"
        }

        val captured=
            linkedSetOf<UUID>()
        try {
            allPlayers.forEach { player ->
                recovery.captureBeforeMatch(
                    player,
                    context.gameTick
                )
                captured += player
            }
        } catch (t: Throwable) {
            // Best-effort immediate rollback for players already prepared.
            captured.toList()
                .asReversed()
                .forEach {
                    recovery.restoreIfPossible(it)
                }
            throw t
        }

        context.redTeam.players.clear()
        context.redTeam.players +=
            request.redPlayers
        context.blueTeam.players.clear()
        context.blueTeam.players +=
            request.bluePlayers

        val session=
            MatchSessionState(
                request.preset
            )
        var tx=request
            .economyTransactionBaseId

        fun seed(
            team: TeamId,
            player: UUID
        ) {
            RecommendedMaturePricing
                .seedPlayer(
                    ledger,
                    team,
                    player,
                    request.preset
                        .startingBalance,
                    tx,
                    "match-start:$player"
                )
            tx += 2

            val progressionState=
                TroopProgressionState()
            progression.initialize(
                progressionState,
                request.preset
                    .progressionMode
            )
            session.players[player]=
                PlayerMatchSessionState(
                    player,
                    progressionState
                )

            runtime.goldmines[player]=
                GoldmineRuntime(
                    playerUuid=player,
                    team=team,
                    level=1,
                    mode=request.preset
                        .goldmineIncomeMode,
                    nextIncomeTick=
                        request
                            .firstGoldmineIncomeTick
                )
        }

        request.redPlayers
            .sortedBy(UUID::toString)
            .forEach {
                seed(TeamId.RED,it)
            }
        request.bluePlayers
            .sortedBy(UUID::toString)
            .forEach {
                seed(TeamId.BLUE,it)
            }

        context.state =
            ArenaState.PREPARED

        return MatchBootstrapReport(
            capturedPlayers=captured,
            seededPlayers=allPlayers,
            session=session
        )
    }

    fun start(
        context: ArenaContext
    ) {
        check(
            context.state ==
                ArenaState.PREPARED
        ) {
            "Arena must be PREPARED before start"
        }
        context.state =
            ArenaState.RUNNING
    }
}
