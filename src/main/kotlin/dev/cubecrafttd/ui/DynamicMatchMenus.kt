package dev.cubecrafttd.ui

import dev.cubecrafttd.match.ArmageddonType
import dev.cubecrafttd.match.EngineeringArmageddonVoteSnapshot
import dev.cubecrafttd.match.PlayerMatchSessionState
import dev.cubecrafttd.mob.MobDefinitionRepository
import java.util.UUID
import dev.cubecrafttd.mob.RecommendedMatureMobDefinitions

/**
 * Functional Stage-4 menu projections.
 *
 * The action semantics and normalized data are real runtime data. Exact Mature
 * slot positions/lore/icons for these menus have not been fully recovered, so
 * every slot below remains ENGINEERING_FALLBACK.
 */
object DynamicMatchMenus {
    fun summoner(
        player:
            PlayerMatchSessionState,
        sendReady: Boolean = false,
        definitions:
            MobDefinitionRepository =
            RecommendedMatureMobDefinitions
    ): MenuDefinition {
        val slots=
            definitions.all()
                .keys
                .sorted()
                .mapNotNull { mobId ->
                    val level=
                        player.progression
                            .level(mobId)
                    if(level<=0) null
                    else mobId to level
                }
                .mapIndexed {
                    index,(mobId,level) ->
                    MenuSlot(
                        slot=index,
                        actionId=
                            "summoner:mob:$mobId:l$level",
                        evidenceStatus=
                            UiEvidenceStatus
                                .ENGINEERING_FALLBACK,
                        displayName=
                            "$mobId L$level"
                    )
                }
                .toMutableList()

        // Historical direct guide evidence: bottom-middle Nether Star opens
        // mob upgrades; bottom-right Mob Spawner sends the queued mobs.
        slots += MenuSlot(
            slot=22,
            actionId="nav:progression",
            evidenceStatus=
                UiEvidenceStatus
                    .HISTORICAL_DIRECT,
            displayName="Upgrade mobs",
            iconHint="nether-star"
        )
        slots += MenuSlot(
            slot=26,
            actionId="summoner:send",
            evidenceStatus=
                UiEvidenceStatus
                    .HISTORICAL_DIRECT,
            displayName="Send selected troops",
            iconHint=
                if(sendReady)
                    "spawner-glow"
                else
                    "spawner"
        )

        return MenuDefinition(
            title="Mob Summoner",
            size=27,
            slots=slots,
            evidenceStatus=
                UiEvidenceStatus
                    .ENGINEERING_FALLBACK
        )
    }

    fun progression(
        player:
            PlayerMatchSessionState,
        availableExp: Long = 0L,
        rollbackEligibleMobIds:
            Set<String> = emptySet(),
        definitions:
            MobDefinitionRepository =
            RecommendedMatureMobDefinitions
    ): MenuDefinition {
        require(availableExp>=0L)
        val mobIds=
            definitions.all()
                .keys
                .sorted()

        val slots=
            buildList {
                mobIds.forEachIndexed {
                    index,mobId ->
                    val current=
                        player.progression
                            .level(mobId)
                    val maxed=
                        current>=5
                    val nextLevel=
                        if(maxed)
                            null
                        else
                            current+1
                    val nextCost=
                        nextLevel?.let {
                            definitions
                                .get(mobId)
                                .level(it)
                                .unlockOrUpgradeExp
                        }
                    val affordable=
                        nextCost!=null &&
                            availableExp>=nextCost

                    add(
                        MenuSlot(
                            slot=index,
                            actionId=
                                if(maxed)
                                    "noop:progression:maxed:$mobId"
                                else
                                    "progression:upgrade:$mobId",
                            evidenceStatus=
                                UiEvidenceStatus
                                    .ENGINEERING_FALLBACK,
                            displayName=
                                when {
                                    maxed ->
                                        "$mobId — MAX LEVEL"
                                    affordable ->
                                        "$mobId L$nextLevel — $nextCost EXP (available)"
                                    else ->
                                        "$mobId L$nextLevel — $nextCost EXP"
                                },
                            iconHint=
                                if(affordable)
                                    "glass-pane-orange"
                                else
                                    "glass-pane"
                        )
                    )
                    val rollbackEligible=
                        mobId in
                            rollbackEligibleMobIds
                    add(
                        MenuSlot(
                            slot=18+index,
                            actionId=
                                if(rollbackEligible)
                                    "progression:rollback:$mobId"
                                else
                                    "noop:progression:rollback-unavailable:$mobId",
                            evidenceStatus=
                                UiEvidenceStatus
                                    .ENGINEERING_FALLBACK,
                            displayName=
                                if(rollbackEligible)
                                    "$mobId rollback"
                                else
                                    "$mobId rollback unavailable",
                            iconHint="nether-star"
                        )
                    )
                }
                add(
                    MenuSlot(
                        slot=35,
                        actionId="nav:summoner",
                        evidenceStatus=
                            UiEvidenceStatus
                                .ENGINEERING_FALLBACK,
                        displayName=
                            "Back to Mob Summoner"
                    )
                )
            }

        return MenuDefinition(
            title="Troop upgrades",
            size=36,
            slots=slots,
            evidenceStatus=
                UiEvidenceStatus
                    .ENGINEERING_FALLBACK
        )
    }

    fun bazaar(
        player:
            PlayerMatchSessionState
    ): MenuDefinition {
        val slots=
            mutableListOf(
                MenuSlot(
                    10,
                    "bazaar:goldmine:upgrade",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Upgrade Goldmine"
                ),
                MenuSlot(
                    12,
                    "bazaar:sword:upgrade",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Upgrade Sword"
                ),
                MenuSlot(
                    14,
                    "bazaar:bow:upgrade",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Upgrade Bow"
                )
            )

        RecommendedMatureBazaarDefinitions
            .aoePotions
            .forEachIndexed {
                index,potion ->
                val unlocked=
                    potion.potionId in
                        player.bazaar
                            .unlockedAoE
                slots += MenuSlot(
                    slot=27+index,
                    actionId=
                        if(unlocked)
                            "bazaar:potion:use:${potion.potionId}"
                        else
                            "bazaar:potion:unlock:${potion.potionId}",
                    evidenceStatus=
                        UiEvidenceStatus
                            .ENGINEERING_FALLBACK,
                    displayName=
                        if(unlocked)
                            "${potion.potionId} — use ${potion.useCostCoins} Coins"
                        else
                            "${potion.potionId} — unlock ${potion.unlockExp} EXP"
                )
            }

        return MenuDefinition(
            title="Bazaar",
            size=45,
            slots=slots,
            evidenceStatus=
                UiEvidenceStatus
                    .ENGINEERING_FALLBACK
        )
    }

    fun settings(
        player:
            PlayerMatchSessionState
    ): MenuDefinition {
        val model=
            SettingsMenuProjector
                .project(
                    player.interaction
                        .settings
                )

        return MenuDefinition(
            title="Settings",
            size=27,
            evidenceStatus=
                UiEvidenceStatus
                    .ENGINEERING_FALLBACK,
            slots=listOf(
                MenuSlot(
                    10,
                    "settings:particle-density",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Particles: ${model.particleDensity.label}"
                ),
                MenuSlot(
                    11,
                    "settings:auto-centre",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Auto-centre towers: ${model.autoCentreEnabled}"
                ),
                MenuSlot(
                    12,
                    "settings:digital-mob-health",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Digital mob health: ${model.digitalMobHealth}"
                ),
                MenuSlot(
                    13,
                    "settings:damage-indicators",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Damage indicators: ${model.damageIndicators}"
                ),
                MenuSlot(
                    15,
                    if(
                        model.inGamePointPurchasesAvailable
                    )
                        "settings:point-purchases"
                    else
                        "noop:settings:point-purchases-unavailable",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    if(
                        model.inGamePointPurchasesAvailable
                    )
                        "In-game Point purchases: ${model.allowInGamePointPurchases}"
                    else
                        "In-game Point purchases: unavailable"
                ),
                MenuSlot(
                    18,
                    "nav:armageddon",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Armageddon vote (Engineering)"
                ),
                MenuSlot(
                    22,
                    "nav:hotbar",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Edit hotbar layout"
                )
            )
        )
    }

    fun armageddonVote(
        playerUuid: UUID,
        snapshot: EngineeringArmageddonVoteSnapshot,
        locked: Boolean
    ): MenuDefinition {
        val positions=mapOf(
            ArmageddonType.WITHER to 11,
            ArmageddonType.LIGHTNING to 13,
            ArmageddonType.HORDE to 15
        )
        val slots=buildList {
            ArmageddonType.entries
                .filter { it in snapshot.allowedTypes }
                .forEach { type ->
                    val ownVote=snapshot.votes[playerUuid]==type
                    add(
                        MenuSlot(
                            positions.getValue(type),
                            "armageddon:vote:" + type.name.lowercase(),
                            UiEvidenceStatus.ENGINEERING_FALLBACK,
                            type.name + " — " + snapshot.count(type) +
                                " vote(s)" +
                                (if(ownVote) " (your vote)" else "") +
                                (if(locked) " [LOCKED]" else "")
                        )
                    )
                }
            add(
                MenuSlot(
                    22,
                    "nav:settings",
                    UiEvidenceStatus.ENGINEERING_FALLBACK,
                    "Back to Settings"
                )
            )
        }
        return MenuDefinition(
            title="Armageddon vote · " + snapshot.selection.type,
            size=27,
            slots=slots,
            evidenceStatus=UiEvidenceStatus.ENGINEERING_FALLBACK
        )
    }


    fun hotbarEditor(
        player:
            PlayerMatchSessionState
    ): MenuDefinition {
        val layout=
            player.interaction
                .hotbarLayout
        val slots=
            buildList {
                HotbarAction.entries
                    .forEachIndexed {
                        row,action ->
                        repeat(9) { slot ->
                            val current=
                                layout.slot(action)==
                                    slot
                            add(
                                MenuSlot(
                                    slot=
                                        row*9 +
                                            slot,
                                    actionId=
                                        "hotbar:move:" +
                                            action.name +
                                            ":" +
                                            slot,
                                    evidenceStatus=
                                        UiEvidenceStatus
                                            .ENGINEERING_FALLBACK,
                                    displayName=
                                        action.name
                                            .lowercase()
                                            .replace(
                                                '_',' '
                                            ) +
                                            " -> slot " +
                                            (slot+1) +
                                            if(current)
                                                " (current)"
                                            else ""
                                )
                            )
                        }
                    }
                add(
                    MenuSlot(
                        53,
                        "nav:settings",
                        UiEvidenceStatus
                            .ENGINEERING_FALLBACK,
                        "Back to Settings"
                    )
                )
            }

        return MenuDefinition(
            title="Hotbar editor",
            size=54,
            slots=slots,
            evidenceStatus=
                UiEvidenceStatus
                    .ENGINEERING_FALLBACK
        )
    }

}
