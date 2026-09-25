package dev.cubecrafttd.troop

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.mob.MobDefinitionRepository
import java.util.UUID

data class TroopSendCommand(
    val senderPlayerUuid: UUID,
    val senderTeam: TeamId,
    val mobId: String,
    val level: Int,
    val quantity: Int,
    val entryId: TroopQueueEntryId,
    val transactionId: Long,
    val purchaseCorrelationId: String,
    val cooldownKey: CooldownKey
)

data class TroopSendReceipt(
    val attackedTeam: TeamId,
    val totalCost: Long,
    val cooldownReadyAtTick: Long,
    val queueUsedUnits: Int
)

class TroopSendCommandService(
    private val definitions: MobDefinitionRepository,
    private val ledger: EconomyLedger,
    private val cooldowns: DeterministicCooldownTracker,
    private val purchaseService: TroopPurchaseService = TroopPurchaseService(ledger)
) {
    /**
     * Must run on the arena-owned mutation thread.
     * Cooldown scope is supplied through cooldownKey and is intentionally not hard-coded
     * because current evidence establishes 15s duration but not the exact shared scope.
     */
    fun execute(
        queue: TroopSendQueue,
        command: TroopSendCommand,
        gameTick: Long,
        cooldownTicks: Long = NormalModeConstants.SEND_COOLDOWN_TICKS
    ): TroopSendReceipt {
        require(command.quantity > 0)
        val attackedTeam = opposite(command.senderTeam)
        check(queue.attackedTeam == attackedTeam) {
            "Sender team ${command.senderTeam} must enqueue against $attackedTeam"
        }
        check(cooldowns.isReady(command.cooldownKey, gameTick)) {
            "Troop send cooldown active for ${command.cooldownKey.value}"
        }

        val level = definitions.get(command.mobId).level(command.level)
        val totalCost = Math.multiplyExact(level.sendCoins, command.quantity.toLong())
        val payer = EconomyAccount(
            command.senderTeam,
            command.senderPlayerUuid,
            EconomyCurrency.MATCH_COINS
        )
        check(ledger.balance(payer) >= totalCost) {
            "Insufficient MATCH_COINS"
        }

        val entry = TroopQueueEntry(
            entryId = command.entryId,
            senderPlayerUuid = command.senderPlayerUuid,
            attackedTeam = attackedTeam,
            mobId = command.mobId,
            level = command.level,
            quantity = command.quantity,
            enqueuedAtTick = gameTick,
            purchaseCorrelationId = command.purchaseCorrelationId
        )
        check(queue.canAccept(entry)) { "Troop queue cannot accept requested quantity" }

        // No mutation above this line. From here the arena-owned single mutation context
        // guarantees the preflight cannot be invalidated by another writer.
        purchaseService.purchaseAndEnqueue(
            queue = queue,
            request = TroopPurchaseRequest(
                entry = entry,
                payer = payer,
                totalCost = totalCost,
                costTruthFieldKey =
                    "troops.${command.mobId}.level${command.level}.send_coins",
                transactionId = command.transactionId
            ),
            arenaTick = gameTick
        )
        check(cooldowns.consume(command.cooldownKey, gameTick, cooldownTicks)) {
            "Cooldown state changed after successful single-thread preflight"
        }

        return TroopSendReceipt(
            attackedTeam = attackedTeam,
            totalCost = totalCost,
            cooldownReadyAtTick = cooldowns.readyAtTick(command.cooldownKey),
            queueUsedUnits = queue.usedUnits()
        )
    }

    private fun opposite(team: TeamId): TeamId =
        if (team == TeamId.RED) TeamId.BLUE else TeamId.RED
}
