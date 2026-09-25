package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.ArenaContext
import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.economy.EconomyAccount
import dev.cubecrafttd.economy.EconomyCurrency
import dev.cubecrafttd.economy.EconomyLedger
import dev.cubecrafttd.match.MatchSessionState
import dev.cubecrafttd.match.NormalMatchClockRuntime
import dev.cubecrafttd.ui.EngineeringMatchHudProjector
import dev.cubecrafttd.ui.EngineeringMatchHudSnapshot
import org.bukkit.Server
import java.util.UUID

/**
 * Live Engineering Playtest observability.
 *
 * The text is intentionally tagged ENG and is not presented as recovered
 * original CubeCraft UI wording.
 */
class BukkitMatchHudService(
    server: Server
) {
    private val actionBar =
        BukkitActionBarPort(server)

    fun push(
        context: ArenaContext,
        ledger: EconomyLedger,
        session: MatchSessionState,
        clock: NormalMatchClockRuntime
    ) {
        session.players.keys
            .sortedBy(UUID::toString)
            .forEach { playerUuid ->
                val team =
                    when {
                        playerUuid in
                            context.redTeam.players ->
                            TeamId.RED
                        playerUuid in
                            context.blueTeam.players ->
                            TeamId.BLUE
                        else -> return@forEach
                    }

                val enemyTeam =
                    if(team==TeamId.RED)
                        TeamId.BLUE
                    else
                        TeamId.RED

                val ownCastle =
                    context.castles
                        .getValue(team)
                val enemyCastle =
                    context.castles
                        .getValue(enemyTeam)

                val text =
                    EngineeringMatchHudProjector
                        .render(
                            EngineeringMatchHudSnapshot(
                                team=team,
                                ownCastleHealth=
                                    ownCastle.health,
                                ownCastleMaxHealth=
                                    ownCastle.maxHealth,
                                enemyCastleHealth=
                                    enemyCastle.health,
                                enemyCastleMaxHealth=
                                    enemyCastle.maxHealth,
                                coins=
                                    balance(
                                        ledger,
                                        team,
                                        playerUuid,
                                        EconomyCurrency
                                            .MATCH_COINS
                                    ),
                                exp=
                                    balance(
                                        ledger,
                                        team,
                                        playerUuid,
                                        EconomyCurrency
                                            .MATCH_EXP
                                    ),
                                elapsedTicks=
                                    clock.elapsedTicks(
                                        context
                                    ),
                                armageddonType=
                                    clock.selection()
                                        .type,
                                armageddonStarted=
                                    clock
                                        .isArmageddonStarted()
                            )
                        )

                actionBar.send(
                    playerUuid,
                    text
                )
            }
    }

    private fun balance(
        ledger: EconomyLedger,
        team: TeamId,
        playerUuid: UUID,
        currency: EconomyCurrency
    ): Long =
        ledger.balance(
            EconomyAccount(
                team,
                playerUuid,
                currency
            )
        )
}
