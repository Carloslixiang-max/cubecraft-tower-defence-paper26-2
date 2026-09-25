package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.mob.*
import java.util.UUID

object MobDefinitionAndEconomyFixture {
    fun run(): List<FixtureResult> {
        val defs = RecommendedMatureMobDefinitions
        val all = defs.all()
        val sender = UUID.fromString("00000000-0000-0000-0000-000000001234")

        val cooldown = DeterministicCooldownTracker()
        val key = CooldownKey("sender:$sender")
        val first = cooldown.consume(key, 100, NormalModeConstants.SEND_COOLDOWN_TICKS)
        val tooSoon = cooldown.consume(key, 399, NormalModeConstants.SEND_COOLDOWN_TICKS)
        val ready = cooldown.consume(key, 400, NormalModeConstants.SEND_COOLDOWN_TICKS)

        val ledger = EconomyLedger()
        val zombie = MobRuntimeState(
            MobIdentity(
                MobInstanceId(1234),
                UUID.fromString("00000000-0000-0000-0000-000000009999"),
                "zombie",
                sender,
                TeamId.BLUE,
                level = 5
            ),
            MobRouteState("fixture",0,0.0,0.0),
            MobCombatState(0.0,154.0,MobLifecycleState.DEAD)
        )
        val expService = SentMobExpRewardService(defs, ledger, PricingMode.NORMAL)
        val expNormal = expService.onSentMobDeath(zombie, 500)
        val expAgain = expService.onSentMobDeath(zombie, 501)
        val account = EconomyAccount(TeamId.RED, sender, EconomyCurrency.MATCH_EXP)

        val doubleLedger = EconomyLedger()
        val doubleMob = zombie.copy(
            identity = zombie.identity.copy(instanceId = MobInstanceId(1235))
        )
        val expDouble = SentMobExpRewardService(
            defs, doubleLedger, PricingMode.DOUBLE_INCOME
        ).onSentMobDeath(doubleMob, 500)

        return listOf(
            FixtureResult("mob-definitions-ten-families", all.size == 10),
            FixtureResult("mob-definitions-five-levels-each", all.values.all { it.levels.size == 5 }),
            FixtureResult(
                "mob-definition-2021-skeleton-override",
                defs.get("skeleton").level(4).sendCoins == 225L &&
                    defs.get("skeleton").level(4).castleDamage == 1.5
            ),
            FixtureResult(
                "mob-definition-2021-creeper-replacement",
                defs.get("creeper").level(5).health == 1250.0 &&
                    defs.get("creeper").special.regeneration == true
            ),
            FixtureResult(
                "mob-definition-2021-giant-threshold",
                defs.get("giant").special.runThresholdHealthFraction == 0.20
            ),
            FixtureResult(
                "mob-definition-slime-shrink",
                defs.get("slime").special.shrinkOnKillMaxHealthLossFraction == 0.25
            ),
            FixtureResult(
                "castle-damage-supports-fractions",
                defs.get("spider").level(3).castleDamage == 1.5 &&
                    defs.get("slime").level(4).castleDamage == 2.5
            ),
            FixtureResult(
                "normal-send-cooldown-15s-deterministic",
                first && !tooSoon && ready &&
                    NormalModeConstants.SEND_COOLDOWN_TICKS == 300L
            ),
            FixtureResult(
                "sent-mob-exp-on-death-idempotent",
                expNormal == 15L && expAgain == 15L && ledger.balance(account) == 15L
            ),
            FixtureResult(
                "double-income-doubles-sent-mob-exp",
                expDouble == 30L
            )
        )
    }
}
