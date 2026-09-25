package dev.cubecrafttd.economy

class GoldmineUpgradeService(
    private val ledger: EconomyLedger
) {
    fun upgrade(
        runtime: GoldmineRuntime,
        gameTick: Long,
        transactionId: Long,
        correlationId: String
    ): GoldmineLevelDefinition {
        check(
            !ledger.hasAppliedCorrelation(
                correlationId
            )
        )
        val nextLevel=runtime.level+1
        val definition=
            GoldmineDefinitionRepository
                .get(
                    runtime.mode,
                    nextLevel
                )
        val account=EconomyAccount(
            runtime.team,
            runtime.playerUuid,
            EconomyCurrency.MATCH_EXP
        )
        check(
            ledger.balance(account) >=
                definition.expUpgradeCost
        ) { "Insufficient match EXP" }

        check(
            ledger.apply(
                EconomyTransaction(
                    transactionId,
                    gameTick,
                    account,
                    -definition.expUpgradeCost,
                    EconomyReason.BAZAAR,
                    correlationId,
                    truthFieldKey=
                        "goldmine.level$nextLevel." +
                            "expUpgradeCost"
                )
            )
        )
        runtime.level=nextLevel
        return definition
    }
}
