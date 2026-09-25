package dev.cubecrafttd.ui

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.economy.*
import java.util.UUID

data class MatchBazaarState(
    var swordTierIndex: Int = 0,
    var bowTierIndex: Int = 0,
    val unlockedAoE:
        MutableSet<String> = linkedSetOf()
)

data class PotionUseToken(
    val potionId: String,
    val buyerUuid: UUID,
    val definition: AoEPotionDefinition
)

class BazaarTransactionService(
    private val ledger: EconomyLedger
) {
    fun upgradeWeapon(
        state: MatchBazaarState,
        team: TeamId,
        playerUuid: UUID,
        weapon: WeaponKind,
        targetTierIndex: Int,
        gameTick: Long,
        transactionId: Long,
        correlationId: String
    ) {
        check(
            !ledger.hasAppliedCorrelation(
                correlationId
            )
        ) { "Bazaar correlation already applied" }

        val tiers = when (weapon) {
            WeaponKind.SWORD ->
                RecommendedMatureBazaarDefinitions
                    .swordTiers
            WeaponKind.BOW ->
                RecommendedMatureBazaarDefinitions
                    .bowTiers
        }
        val current = when (weapon) {
            WeaponKind.SWORD ->
                state.swordTierIndex
            WeaponKind.BOW ->
                state.bowTierIndex
        }
        check(
            targetTierIndex == current + 1
        ) {
            "Weapon upgrades must advance one tier"
        }
        check(targetTierIndex in tiers.indices)
        val definition =
            tiers[targetTierIndex]
        val account = EconomyAccount(
            team,playerUuid,
            EconomyCurrency.MATCH_COINS
        )
        check(
            ledger.balance(account) >=
                definition.unlockCoins
        ) { "Insufficient Coins" }

        check(
            ledger.apply(
                EconomyTransaction(
                    transactionId,
                    gameTick,
                    account,
                    -definition.unlockCoins,
                    EconomyReason.BAZAAR,
                    correlationId,
                    truthFieldKey =
                        "bazaar.${weapon.name.lowercase()}." +
                            "${definition.tierId}.unlockCoins"
                )
            )
        )

        when (weapon) {
            WeaponKind.SWORD ->
                state.swordTierIndex =
                    targetTierIndex
            WeaponKind.BOW ->
                state.bowTierIndex =
                    targetTierIndex
        }
    }

    fun unlockPotion(
        state: MatchBazaarState,
        team: TeamId,
        playerUuid: UUID,
        potionId: String,
        gameTick: Long,
        transactionId: Long,
        correlationId: String
    ) {
        check(potionId !in state.unlockedAoE) {
            "$potionId already unlocked"
        }
        check(
            !ledger.hasAppliedCorrelation(
                correlationId
            )
        )
        val definition =
            RecommendedMatureBazaarDefinitions
                .potion(potionId)
        val account = EconomyAccount(
            team,playerUuid,
            EconomyCurrency.MATCH_EXP
        )
        check(
            ledger.balance(account) >=
                definition.unlockExp
        ) { "Insufficient match EXP" }

        check(
            ledger.apply(
                EconomyTransaction(
                    transactionId,
                    gameTick,
                    account,
                    -definition.unlockExp,
                    EconomyReason.BAZAAR,
                    correlationId,
                    truthFieldKey =
                        "bazaar.aoe.$potionId.unlockExp"
                )
            )
        )
        state.unlockedAoE += potionId
    }

    fun purchasePotionUse(
        state: MatchBazaarState,
        team: TeamId,
        playerUuid: UUID,
        potionId: String,
        gameTick: Long,
        transactionId: Long,
        correlationId: String
    ): PotionUseToken {
        check(potionId in state.unlockedAoE) {
            "$potionId is not unlocked"
        }
        check(
            !ledger.hasAppliedCorrelation(
                correlationId
            )
        )
        val definition =
            RecommendedMatureBazaarDefinitions
                .potion(potionId)
        val account = EconomyAccount(
            team,playerUuid,
            EconomyCurrency.MATCH_COINS
        )
        check(
            ledger.balance(account) >=
                definition.useCostCoins
        ) { "Insufficient Coins" }

        check(
            ledger.apply(
                EconomyTransaction(
                    transactionId,
                    gameTick,
                    account,
                    -definition.useCostCoins,
                    EconomyReason.BAZAAR,
                    correlationId,
                    truthFieldKey =
                        "bazaar.aoe.$potionId.useCostCoins"
                )
            )
        )

        return PotionUseToken(
            potionId,playerUuid,definition
        )
    }
}
