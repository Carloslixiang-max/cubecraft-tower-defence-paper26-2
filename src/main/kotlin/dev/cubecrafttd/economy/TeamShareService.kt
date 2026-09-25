package dev.cubecrafttd.economy

import dev.cubecrafttd.arena.TeamId
import java.util.UUID

data class TeamShareReceipt(
    val senderUuid: UUID,
    val recipientUuid: UUID,
    val amount: Long
)

class TeamShareService(
    private val ledger: EconomyLedger
) {
    fun shareCoins(
        team: TeamId,
        teamMembers: Set<UUID>,
        senderUuid: UUID,
        recipientUuid: UUID,
        amount: Long,
        gameTick: Long,
        transactionBaseId: Long,
        correlationId: String
    ): TeamShareReceipt {
        require(amount > 0L)
        check(senderUuid != recipientUuid)
        check(senderUuid in teamMembers)
        check(recipientUuid in teamMembers)

        val debitCorrelation=
            "$correlationId:debit"
        val creditCorrelation=
            "$correlationId:credit"
        check(
            !ledger.hasAppliedCorrelation(
                debitCorrelation
            ) &&
            !ledger.hasAppliedCorrelation(
                creditCorrelation
            )
        ) { "Share correlation already applied" }

        val sender=EconomyAccount(
            team,senderUuid,
            EconomyCurrency.MATCH_COINS
        )
        val recipient=EconomyAccount(
            team,recipientUuid,
            EconomyCurrency.MATCH_COINS
        )
        check(ledger.balance(sender)>=amount) {
            "Insufficient Coins"
        }

        check(
            ledger.apply(
                EconomyTransaction(
                    transactionBaseId,
                    gameTick,
                    sender,
                    -amount,
                    EconomyReason.TEAM_SHARE,
                    debitCorrelation,
                    metadata=mapOf(
                        "recipient" to
                            recipientUuid.toString()
                    )
                )
            )
        )

        try {
            check(
                ledger.apply(
                    EconomyTransaction(
                        transactionBaseId+1,
                        gameTick,
                        recipient,
                        amount,
                        EconomyReason.TEAM_SHARE,
                        creditCorrelation,
                        metadata=mapOf(
                            "sender" to
                                senderUuid.toString()
                        )
                    )
                )
            )
        } catch (t: Throwable) {
            ledger.apply(
                EconomyTransaction(
                    transactionBaseId+2,
                    gameTick,
                    sender,
                    amount,
                    EconomyReason
                        .TRANSACTION_ROLLBACK,
                    "$correlationId:rollback",
                    metadata=mapOf(
                        "failedCredit" to
                            creditCorrelation
                    )
                )
            )
            throw t
        }

        return TeamShareReceipt(
            senderUuid,recipientUuid,amount
        )
    }
}
