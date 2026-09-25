package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.castle.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.troop.*
import java.util.UUID

object CastleEconomyQueueFixture {
    fun run(): List<FixtureResult> {
        val sender = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val mobUuid = UUID.fromString("00000000-0000-0000-0000-000000000101")
        val castles = mutableMapOf(
            TeamId.RED to CastleRuntime(TeamId.RED),
            TeamId.BLUE to CastleRuntime(TeamId.BLUE)
        )
        val combat = CastleCombatService(castles)
        val mob = MobRuntimeState(
            MobIdentity(MobInstanceId(1), mobUuid, "zombie", sender, TeamId.BLUE, 1),
            MobRouteState("blue-route", 0, 1.0, 10.0),
            MobCombatState(100.0, 100.0, MobLifecycleState.MOVING)
        )
        combat.enterCastleAttackState(mob)
        combat.applyCastleHit(mob, 20.0)

        val ledger = EconomyLedger()
        val account = EconomyAccount(TeamId.RED, sender, EconomyCurrency.MATCH_COINS)
        ledger.apply(EconomyTransaction(1, 0, account, 100, EconomyReason.MATCH_INITIALIZATION, "seed"))

        val queue = TroopSendQueue(TeamId.BLUE, 12)
        val entry = TroopQueueEntry(
            TroopQueueEntryId(7), sender, TeamId.BLUE, "zombie", 1,
            quantity = 3, enqueuedAtTick = 5, purchaseCorrelationId = "troop-buy-7"
        )
        TroopPurchaseService(ledger).purchaseAndEnqueue(
            queue,
            TroopPurchaseRequest(entry, account, 30, "troops.zombie.level1.send_coins", 2),
            arenaTick = 5
        )

        return listOf(
            FixtureResult("castle-known-start-hp", castles.getValue(TeamId.BLUE).maxHealth == 1000.0),
            FixtureResult("castle-hit-state", mob.combat.lifecycle == MobLifecycleState.ATTACKING_CASTLE && castles.getValue(TeamId.BLUE).health == 980.0),
            FixtureResult("troop-purchase-atomic-preflight", ledger.balance(account) == 70L && queue.usedUnits() == 3)
        )
    }
}
