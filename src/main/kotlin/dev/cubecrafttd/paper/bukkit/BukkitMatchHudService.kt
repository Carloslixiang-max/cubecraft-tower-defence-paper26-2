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
import dev.cubecrafttd.ui.HistoricalCastleHealthHudProjector
import dev.cubecrafttd.ui.HistoricalMatchSidebarProjector
import org.bukkit.plugin.Plugin
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import java.util.UUID

/**
 * Live Engineering Playtest observability.
 *
 * The text is intentionally tagged ENG and is not presented as recovered
 * original CubeCraft UI wording.
 */
class BukkitMatchHudService(
    private val plugin: Plugin
) {
    private val server=
        plugin.server
    private val actionBar =
        BukkitActionBarPort(server)
    private val sidebar=
        BukkitHistoricalSidebarService(
            plugin
        )
    private val castleHealthBars=
        linkedMapOf<UUID,BossBar>()

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

                val coins=
                    balance(
                        ledger,
                        team,
                        playerUuid,
                        EconomyCurrency
                            .MATCH_COINS
                    )
                val exp=
                    balance(
                        ledger,
                        team,
                        playerUuid,
                        EconomyCurrency
                            .MATCH_EXP
                    )
                val castleHud=
                    HistoricalCastleHealthHudProjector
                        .project(
                            ownCastle.health,
                            ownCastle.maxHealth
                        )
                val historicalSidebarActive=
                    sidebar.push(
                        playerUuid,
                        HistoricalMatchSidebarProjector
                            .project(
                                coins,
                                exp
                            )
                    )

                server.getPlayer(
                    playerUuid
                )?.let { player ->
                    val bar=
                        castleHealthBars
                            .getOrPut(
                                playerUuid
                            ) {
                                server.createBossBar(
                                    castleHud.title,
                                    BarColor.GREEN,
                                    BarStyle.SOLID
                                )
                            }
                    bar.setTitle(
                        castleHud.title
                    )
                    bar.setProgress(
                        castleHud.progress
                    )
                    bar.addPlayer(
                        player
                    )
                }

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
                                coins=coins,
                                exp=exp,
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
                    if(historicalSidebarActive)
                        ""
                    else
                        text
                )
            }
    }

    fun clear(
        playerUuid: UUID
    ) {
        castleHealthBars
            .remove(
                playerUuid
            )
            ?.removeAll()
        sidebar.clear(
            playerUuid
        )
    }

    fun clear(
        playerUuids: Collection<UUID>
    ) {
        playerUuids.forEach(
            ::clear
        )
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
