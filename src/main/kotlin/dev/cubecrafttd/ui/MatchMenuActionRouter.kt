package dev.cubecrafttd.ui

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.match.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.progression.*
import dev.cubecrafttd.troop.*
import dev.cubecrafttd.tower.visual.TowerPath
import java.util.UUID

sealed interface MatchMenuActionResult {
    data class TowerSelected(
        val selection:
            TowerBuilderSelection
    ) : MatchMenuActionResult

    data class SummonerDraftChanged(
        val receipt:
            SummonerDraftEditReceipt
    ) : MatchMenuActionResult

    data class SummonerBatchSent(
        val receipt:
            TroopBatchSendReceipt
    ) : MatchMenuActionResult

    data class ProgressionChanged(
        val mobId: String,
        val level: Int,
        val expDelta: Long,
        val rollbackUntilTick:
            Long?
    ) : MatchMenuActionResult

    data class GoldmineUpgraded(
        val level: Int,
        val coinsPerSecond: Long
    ) : MatchMenuActionResult

    data class WeaponTierUpgraded(
        val weapon: WeaponKind,
        val tierIndex: Int
    ) : MatchMenuActionResult

    data class PotionUnlocked(
        val potionId: String
    ) : MatchMenuActionResult

    data class PotionUsePurchased(
        val token: PotionUseToken
    ) : MatchMenuActionResult

    data class SettingsChanged(
        val settings:
            SettingsMenuModel
    ) : MatchMenuActionResult

    data class HotbarLayoutChanged(
        val layout:
            HotbarLayout
    ) : MatchMenuActionResult
}

class MatchMenuActionRouter(
    private val context: ArenaContext,
    private val runtime:
        NormalArenaRuntimeState,
    private val session:
        MatchSessionState,
    private val ledger:
        EconomyLedger,
    private val progression:
        TroopProgressionService,
    private val cooldowns:
        DeterministicCooldownTracker =
        DeterministicCooldownTracker(),
    private val definitions:
        MobDefinitionRepository =
        RecommendedMatureMobDefinitions,
    initialSequence: Long = 1L
) {
    private var nextSequence=
        initialSequence

    private val draftService=
        SummonerDraftService(
            definitions,ledger
        )
    private val batchSend=
        TroopBatchSendService(
            definitions,ledger,cooldowns
        )
    private val goldmine=
        GoldmineUpgradeService(ledger)
    private val bazaar=
        BazaarTransactionService(ledger)

    fun handle(
        invocation:
            MenuActionInvocation
    ): MatchMenuActionResult {
        check(
            context.state==
                ArenaState.RUNNING
        ) {
            "Menu actions require RUNNING arena"
        }

        val player=
            invocation.playerUuid
        val team=teamOf(player)
        val playerState=
            session.players[player]
                ?: error(
                    "Player not in match session"
                )

        val parts=
            invocation.actionId
                .split(':')
        check(parts.isNotEmpty())

        return when(parts[0]) {
            "tower" ->
                handleTower(
                    playerState,
                    parts,
                    invocation.click
                )

            "summoner" ->
                handleSummoner(
                    playerState,
                    team,
                    player,
                    parts,
                    invocation.click
                )

            "progression" ->
                handleProgression(
                    playerState,
                    team,
                    player,
                    parts
                )

            "bazaar" ->
                handleBazaar(
                    playerState,
                    team,
                    player,
                    parts
                )

            "settings" ->
                handleSettings(
                    playerState,
                    parts
                )

            "hotbar" ->
                handleHotbar(
                    playerState,
                    parts
                )

            else ->
                error(
                    "Unknown menu action ${invocation.actionId}"
                )
        }
    }

    private fun handleTower(
        player:
            PlayerMatchSessionState,
        parts: List<String>,
        click: ClickKind
    ): MatchMenuActionResult {
        check(parts.size==2) {
            "tower:<towerId>"
        }
        val towerId=parts[1]
        val path=when(click) {
            ClickKind.LEFT,
            ClickKind.SHIFT_LEFT ->
                TowerPath.TOP

            ClickKind.RIGHT,
            ClickKind.SHIFT_RIGHT ->
                TowerPath.BOTTOM
        }

        return MatchMenuActionResult
            .TowerSelected(
                player.interaction.builder
                    .select(
                        towerId,path
                    )
            )
    }

    private fun handleSummoner(
        playerState:
            PlayerMatchSessionState,
        team: TeamId,
        playerUuid: UUID,
        parts: List<String>,
        click: ClickKind
    ): MatchMenuActionResult {
        check(parts.size>=2)
        return when(parts[1]) {
            "mob" -> {
                check(parts.size==4) {
                    "summoner:mob:<mobId>:l<level>"
                }
                val levelToken=parts[3]
                check(
                    levelToken.startsWith("l")
                )
                val key=
                    SummonerDraftKey(
                        parts[2],
                        levelToken
                            .drop(1)
                            .toInt()
                    )
                val queue=
                    runtime.queues
                        .getValue(
                            opposite(team)
                        )
                MatchMenuActionResult
                    .SummonerDraftChanged(
                        draftService.edit(
                            playerState
                                .interaction
                                .summonerDraft,
                            playerState
                                .progression,
                            queue,
                            team,
                            playerUuid,
                            key,
                            click
                        )
                    )
            }

            "send" -> {
                check(parts.size==2)
                val draft=
                    playerState
                        .interaction
                        .summonerDraft
                val lines=
                    draft.snapshot()
                check(lines.isNotEmpty()) {
                    "Summoner draft is empty"
                }

                val selections=
                    lines.map {
                        line ->
                        val seq=nextSequence++
                        TroopBatchSelection(
                            mobId=
                                line.key.mobId,
                            level=
                                line.key.level,
                            quantity=
                                line.quantity,
                            entryId=
                                TroopQueueEntryId(
                                    seq
                                ),
                            transactionId=
                                seq,
                            correlationId=
                                "summoner:$playerUuid:$seq"
                        )
                    }

                val receipt=
                    batchSend.execute(
                        queue=
                            runtime.queues
                                .getValue(
                                    opposite(team)
                                ),
                        command=
                            TroopBatchSendCommand(
                                senderPlayerUuid=
                                    playerUuid,
                                senderTeam=team,
                                selections=
                                    selections,
                                cooldownKey=
                                    CooldownKey(
                                        "sender:$playerUuid"
                                    )
                            ),
                        gameTick=
                            context.gameTick
                    )

                draft.clear()
                MatchMenuActionResult
                    .SummonerBatchSent(
                        receipt
                    )
            }

            else ->
                error(
                    "Unknown Summoner action"
                )
        }
    }

    private fun handleProgression(
        playerState:
            PlayerMatchSessionState,
        team: TeamId,
        playerUuid: UUID,
        parts: List<String>
    ): MatchMenuActionResult {
        check(parts.size==3) {
            "progression:<upgrade|rollback>:<mobId>"
        }
        val mobId=parts[2]
        val seq=nextSequence++

        return when(parts[1]) {
            "upgrade" -> {
                val receipt=
                    progression.unlockOrUpgrade(
                        playerState
                            .progression,
                        team,
                        playerUuid,
                        mobId,
                        context.gameTick,
                        seq,
                        "progression:$playerUuid:$seq"
                    )
                MatchMenuActionResult
                    .ProgressionChanged(
                        mobId=
                            receipt.mobId,
                        level=
                            receipt.newLevel,
                        expDelta=
                            -receipt.expSpent,
                        rollbackUntilTick=
                            receipt
                                .rollbackDeadlineTick
                    )
            }

            "rollback" -> {
                val refunded=
                    progression.rollbackLast(
                        playerState
                            .progression,
                        team,
                        playerUuid,
                        mobId,
                        context.gameTick,
                        seq,
                        "progression-rollback:$playerUuid:$seq"
                    )
                MatchMenuActionResult
                    .ProgressionChanged(
                        mobId=mobId,
                        level=
                            playerState
                                .progression
                                .level(mobId),
                        expDelta=refunded,
                        rollbackUntilTick=null
                    )
            }

            else ->
                error(
                    "Unknown progression action"
                )
        }
    }

    private fun handleBazaar(
        playerState:
            PlayerMatchSessionState,
        team: TeamId,
        playerUuid: UUID,
        parts: List<String>
    ): MatchMenuActionResult {
        check(parts.size>=2)
        val seq=nextSequence++

        return when(parts[1]) {
            "goldmine" -> {
                check(
                    parts.size==3 &&
                        parts[2]=="upgrade"
                ) {
                    "bazaar:goldmine:upgrade"
                }
                val runtimeMine=
                    runtime.goldmines[
                        playerUuid
                    ] ?: error(
                        "Goldmine runtime missing"
                    )
                val definition=
                    goldmine.upgrade(
                        runtimeMine,
                        context.gameTick,
                        seq,
                        "goldmine-upgrade:$playerUuid:$seq"
                    )
                MatchMenuActionResult
                    .GoldmineUpgraded(
                        definition.level,
                        definition.coinsPerSecond
                    )
            }

            "sword",
            "bow" -> {
                check(
                    parts.size==3 &&
                        parts[2]=="upgrade"
                )
                val kind=
                    if(parts[1]=="sword")
                        WeaponKind.SWORD
                    else WeaponKind.BOW
                val current=
                    if(kind==
                        WeaponKind.SWORD)
                        playerState.bazaar
                            .swordTierIndex
                    else
                        playerState.bazaar
                            .bowTierIndex
                val target=current+1
                bazaar.upgradeWeapon(
                    playerState.bazaar,
                    team,
                    playerUuid,
                    kind,
                    target,
                    context.gameTick,
                    seq,
                    "bazaar-${parts[1]}:$playerUuid:$seq"
                )
                MatchMenuActionResult
                    .WeaponTierUpgraded(
                        kind,target
                    )
            }

            "potion" -> {
                check(parts.size==4) {
                    "bazaar:potion:<unlock|use>:<id>"
                }
                val potionId=parts[3]
                when(parts[2]) {
                    "unlock" -> {
                        bazaar.unlockPotion(
                            playerState.bazaar,
                            team,
                            playerUuid,
                            potionId,
                            context.gameTick,
                            seq,
                            "bazaar-potion-unlock:$playerUuid:$seq"
                        )
                        MatchMenuActionResult
                            .PotionUnlocked(
                                potionId
                            )
                    }

                    "use" -> {
                        val token=
                            bazaar
                                .purchasePotionUse(
                                    playerState
                                        .bazaar,
                                    team,
                                    playerUuid,
                                    potionId,
                                    context.gameTick,
                                    seq,
                                    "bazaar-potion-use:$playerUuid:$seq"
                                )
                        MatchMenuActionResult
                            .PotionUsePurchased(
                                token
                            )
                    }

                    else ->
                        error(
                            "Unknown potion action"
                        )
                }
            }

            else ->
                error(
                    "Unknown Bazaar action"
                )
        }
    }


    private fun handleSettings(
        playerState:
            PlayerMatchSessionState,
        parts: List<String>
    ): MatchMenuActionResult {
        check(parts.size==2) {
            "settings:<auto-centre|point-purchases>"
        }
        val settings=
            playerState.interaction
                .settings

        when(parts[1]) {
            "auto-centre" ->
                settings.setAutoCentre(
                    !settings
                        .autoCentreTowers
                )
            "point-purchases" ->
                settings
                    .allowInGamePointPurchases=
                    !settings
                        .allowInGamePointPurchases
            else ->
                error(
                    "Unknown settings action ${parts[1]}"
                )
        }

        return MatchMenuActionResult
            .SettingsChanged(
                SettingsMenuProjector
                    .project(settings)
            )
    }

    private fun handleHotbar(
        playerState:
            PlayerMatchSessionState,
        parts: List<String>
    ): MatchMenuActionResult {
        check(
            parts.size==4 &&
                parts[1]=="move"
        ) {
            "hotbar:move:<action>:<slot>"
        }
        val action=
            HotbarAction.valueOf(
                parts[2]
                    .uppercase()
            )
        val slot=
            parts[3].toInt()
        val next=
            playerState.interaction
                .hotbarLayout
                .move(
                    action,
                    slot
                )
        playerState.interaction
            .hotbarLayout=next
        return MatchMenuActionResult
            .HotbarLayoutChanged(
                next
            )
    }

    private fun teamOf(
        player: UUID
    ): TeamId =
        when {
            player in
                context.redTeam.players ->
                TeamId.RED
            player in
                context.blueTeam.players ->
                TeamId.BLUE
            else ->
                error(
                    "Player is not in arena"
                )
        }

    private fun opposite(
        team: TeamId
    ): TeamId =
        if(team==TeamId.RED)
            TeamId.BLUE
        else TeamId.RED
}
