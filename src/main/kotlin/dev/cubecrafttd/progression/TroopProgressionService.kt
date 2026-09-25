package dev.cubecrafttd.progression

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.mob.MobDefinitionRepository
import dev.cubecrafttd.mob.RecommendedMatureMobDefinitions
import java.util.UUID

const val TROOP_UPGRADE_ROLLBACK_WINDOW_TICKS:
    Long = 10L * 20L

enum class ProgressionMode {
    CLASSIC_PROGRESSION,
    ALL_UNLOCKED
}

data class TroopProgressionState(
    val unlockedLevelByMob:
        MutableMap<String,Int> =
        linkedMapOf()
) {
    fun level(mobId: String): Int =
        unlockedLevelByMob[mobId] ?: 0
}

data class TroopProgressionChange(
    val mobId: String,
    val previousLevel: Int,
    val newLevel: Int,
    val expCost: Long,
    val purchasedAtTick: Long,
    val rollbackDeadlineTick: Long,
    val purchaseCorrelationId: String
)

data class TroopProgressionReceipt(
    val mobId: String,
    val newLevel: Int,
    val expSpent: Long,
    val rollbackDeadlineTick: Long
)

class TroopProgressionService(
    private val ledger: EconomyLedger,
    private val definitions:
        MobDefinitionRepository =
        RecommendedMatureMobDefinitions
) {
    private val lastChangeByPlayerAndMob =
        linkedMapOf<Pair<UUID,String>,
            TroopProgressionChange>()

    fun initialize(
        state: TroopProgressionState,
        mode: ProgressionMode =
            ProgressionMode.CLASSIC_PROGRESSION
    ) {
        definitions.all().values.forEach {
            definition ->
            state.unlockedLevelByMob[
                definition.mobId
            ] = when(mode) {
                ProgressionMode
                    .ALL_UNLOCKED -> 5
                ProgressionMode
                    .CLASSIC_PROGRESSION -> {
                    val first=
                        definition.level(1)
                    if(
                        first.unlockOrUpgradeExp==
                            0L
                    ) 1 else 0
                }
            }
        }
    }

    fun unlockOrUpgrade(
        state: TroopProgressionState,
        team: TeamId,
        playerUuid: UUID,
        mobId: String,
        gameTick: Long,
        transactionId: Long,
        correlationId: String
    ): TroopProgressionReceipt {
        check(
            !ledger.hasAppliedCorrelation(
                correlationId
            )
        ) { "Progression correlation already applied" }

        val definition=definitions.get(mobId)
        val current=state.level(mobId)
        val target=current+1
        check(target in 1..5) {
            "$mobId is already maxed or target invalid"
        }

        val targetDefinition=
            definition.level(target)
        val cost=
            targetDefinition.unlockOrUpgradeExp
        val account=EconomyAccount(
            team,playerUuid,
            EconomyCurrency.MATCH_EXP
        )
        check(ledger.balance(account)>=cost) {
            "Insufficient match EXP"
        }

        check(
            ledger.apply(
                EconomyTransaction(
                    transactionId,
                    gameTick,
                    account,
                    -cost,
                    EconomyReason
                        .TROOP_UNLOCK_UPGRADE,
                    correlationId,
                    truthFieldKey=
                        "troops.$mobId.level$target." +
                            "unlock_or_upgrade_exp"
                )
            )
        )

        state.unlockedLevelByMob[mobId]=target
        val change=TroopProgressionChange(
            mobId,current,target,cost,gameTick,
            gameTick+
                TROOP_UPGRADE_ROLLBACK_WINDOW_TICKS,
            correlationId
        )
        lastChangeByPlayerAndMob[
            playerUuid to mobId
        ]=change

        return TroopProgressionReceipt(
            mobId,target,cost,
            change.rollbackDeadlineTick
        )
    }

    fun rollbackLast(
        state: TroopProgressionState,
        team: TeamId,
        playerUuid: UUID,
        mobId: String,
        gameTick: Long,
        transactionId: Long,
        correlationId: String
    ): Long {
        check(
            !ledger.hasAppliedCorrelation(
                correlationId
            )
        )
        val key=playerUuid to mobId
        val change=
            lastChangeByPlayerAndMob[key]
                ?: error(
                    "No rollback-eligible change"
                )
        check(
            gameTick <=
                change.rollbackDeadlineTick
        ) {
            "10-second rollback window expired"
        }
        check(
            state.level(mobId)==
                change.newLevel
        ) {
            "Progression moved beyond rollback target"
        }

        val account=EconomyAccount(
            team,playerUuid,
            EconomyCurrency.MATCH_EXP
        )
        check(
            ledger.apply(
                EconomyTransaction(
                    transactionId,
                    gameTick,
                    account,
                    change.expCost,
                    EconomyReason
                        .TROOP_UNLOCK_ROLLBACK,
                    correlationId,
                    truthFieldKey=
                        "troop_upgrade_rollback_100_percent",
                    metadata=mapOf(
                        "originalPurchase" to
                            change.purchaseCorrelationId
                    )
                )
            )
        )

        state.unlockedLevelByMob[mobId]=
            change.previousLevel
        lastChangeByPlayerAndMob.remove(key)
        return change.expCost
    }

    fun mayRollback(
        playerUuid: UUID,
        mobId: String,
        gameTick: Long
    ): Boolean {
        val change=
            lastChangeByPlayerAndMob[
                playerUuid to mobId
            ] ?: return false
        return gameTick <=
            change.rollbackDeadlineTick
    }
}
