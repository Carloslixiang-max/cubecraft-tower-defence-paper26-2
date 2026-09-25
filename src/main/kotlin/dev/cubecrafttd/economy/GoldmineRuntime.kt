package dev.cubecrafttd.economy

import dev.cubecrafttd.arena.TeamId
import java.util.UUID

enum class GoldmineIncomeMode { NORMAL, DOUBLE_INCOME }

data class GoldmineLevelDefinition(
    val level: Int,
    val coinsPerSecond: Long,
    val expUpgradeCost: Long
)

object GoldmineDefinitionRepository {
    val normal: List<GoldmineLevelDefinition> = listOf(
        GoldmineLevelDefinition(1, 5, 0),
        GoldmineLevelDefinition(2, 10, 125),
        GoldmineLevelDefinition(3, 15, 350),
        GoldmineLevelDefinition(4, 25, 1000),
        GoldmineLevelDefinition(5, 50, 2500),
        GoldmineLevelDefinition(6, 100, 6000)
    )

    val doubleIncome: List<GoldmineLevelDefinition> =
        normal.map { it.copy(coinsPerSecond = it.coinsPerSecond * 2) }

    fun get(mode: GoldmineIncomeMode, level: Int): GoldmineLevelDefinition =
        (if (mode == GoldmineIncomeMode.NORMAL) normal else doubleIncome)
            .firstOrNull { it.level == level }
            ?: error("Unknown Goldmine level $level")
}

data class GoldmineRuntime(
    val playerUuid: UUID,
    val team: TeamId,
    var level: Int,
    val mode: GoldmineIncomeMode,
    var nextIncomeTick: Long
)

class GoldmineIncomeService(
    private val ledger: EconomyLedger
) {
    /**
     * The caller supplies first nextIncomeTick. This avoids inventing the original
     * phase alignment while keeping the verified Coins/sec cadence deterministic.
     */
    fun tick(runtime: GoldmineRuntime, gameTick: Long): Long {
        if (gameTick < runtime.nextIncomeTick) return 0
        val dueSeconds = ((gameTick - runtime.nextIncomeTick) / 20L) + 1L
        val definition = GoldmineDefinitionRepository.get(runtime.mode, runtime.level)
        val amount = definition.coinsPerSecond * dueSeconds
        val firstTick = runtime.nextIncomeTick
        runtime.nextIncomeTick += dueSeconds * 20L

        ledger.apply(
            EconomyTransaction(
                transactionId = gameTick xor runtime.playerUuid.leastSignificantBits,
                arenaTick = gameTick,
                account = EconomyAccount(runtime.team, runtime.playerUuid, EconomyCurrency.MATCH_COINS),
                delta = amount,
                reason = EconomyReason.GOLDMINE_TICK,
                correlationId = "goldmine:${runtime.playerUuid}:$firstTick:${runtime.nextIncomeTick}",
                truthFieldKey = "goldmine.${runtime.mode.name.lowercase()}.level${runtime.level}.coinsPerSecond"
            )
        )
        return amount
    }
}
