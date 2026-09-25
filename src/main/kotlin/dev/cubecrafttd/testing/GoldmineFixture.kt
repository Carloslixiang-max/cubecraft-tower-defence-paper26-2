package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.economy.*
import java.util.UUID

object GoldmineFixture {
    fun run(): List<FixtureResult> {
        val player = UUID.fromString("00000000-0000-0000-0000-000000001111")
        val ledger = EconomyLedger()
        val runtime = GoldmineRuntime(player, TeamId.RED, 4, GoldmineIncomeMode.NORMAL, nextIncomeTick = 20)
        val service = GoldmineIncomeService(ledger)
        val before = service.tick(runtime, 19)
        val at20 = service.tick(runtime, 20)
        val catchup = service.tick(runtime, 61)
        val account = EconomyAccount(TeamId.RED, player, EconomyCurrency.MATCH_COINS)

        return listOf(
            FixtureResult("goldmine-before-first-tick-zero", before == 0L),
            FixtureResult("goldmine-level4-normal-25-per-sec", at20 == 25L),
            FixtureResult("goldmine-deterministic-catchup", catchup == 50L && ledger.balance(account) == 75L),
            FixtureResult(
                "goldmine-double-income-table",
                GoldmineDefinitionRepository.get(GoldmineIncomeMode.DOUBLE_INCOME, 6).coinsPerSecond == 200L
            )
        )
    }
}
