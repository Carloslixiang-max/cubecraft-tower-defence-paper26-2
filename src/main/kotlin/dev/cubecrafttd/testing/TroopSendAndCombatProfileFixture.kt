package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.troop.*
import java.util.UUID

object TroopSendAndCombatProfileFixture {
    fun run(): List<FixtureResult> {
        val sender = UUID.fromString("00000000-0000-0000-0000-000000004242")
        val ledger = EconomyLedger()
        val account = EconomyAccount(TeamId.RED, sender, EconomyCurrency.MATCH_COINS)
        ledger.apply(
            EconomyTransaction(
                1,0,account,1000,
                EconomyReason.MATCH_INITIALIZATION,
                "seed-send-fixture"
            )
        )
        val cooldowns = DeterministicCooldownTracker()
        val queue = TroopSendQueue(TeamId.BLUE, 12)
        val service = TroopSendCommandService(
            RecommendedMatureMobDefinitions,
            ledger,
            cooldowns
        )
        val receipt = service.execute(
            queue,
            TroopSendCommand(
                senderPlayerUuid = sender,
                senderTeam = TeamId.RED,
                mobId = "zombie",
                level = 1,
                quantity = 12,
                entryId = TroopQueueEntryId(500),
                transactionId = 2,
                purchaseCorrelationId = "send-500",
                cooldownKey = CooldownKey("sender:$sender")
            ),
            gameTick = 100
        )

        val blockedByCooldown = try {
            service.execute(
                TroopSendQueue(TeamId.BLUE, 12),
                TroopSendCommand(
                    sender, TeamId.RED, "zombie", 1, 1,
                    TroopQueueEntryId(501), 3, "send-501",
                    CooldownKey("sender:$sender")
                ),
                gameTick = 200
            )
            false
        } catch (_: IllegalStateException) { true }

        val caveSpider = RecommendedMatureMobCombatProfiles.get("cave_spider")
        val archer = TowerAttackCapability(
            "archer", setOf(TargetLayer.GROUND, TargetLayer.AIR),
            setOf(DamageKind.PHYSICAL)
        )
        val mage = TowerAttackCapability(
            "mage", setOf(TargetLayer.GROUND),
            setOf(DamageKind.FIRE)
        )
        val artillery = TowerAttackCapability(
            "artillery", setOf(TargetLayer.GROUND),
            setOf(DamageKind.PHYSICAL)
        )
        val pigman = RecommendedMatureMobCombatProfiles.get("pigman")
        val zeus = TowerAttackCapability(
            "zeus", setOf(TargetLayer.GROUND, TargetLayer.AIR),
            setOf(DamageKind.LIGHTNING)
        )
        val giant = RecommendedMatureMobCombatProfiles.get("giant")
        val endermite = RecommendedMatureMobCombatProfiles.get("endermite")

        return listOf(
            FixtureResult(
                "troop-send-full-wave-cost-and-queue",
                receipt.totalCost == 180L &&
                    receipt.queueUsedUnits == 12 &&
                    ledger.balance(account) == 820L
            ),
            FixtureResult(
                "troop-send-cooldown-armed-after-success",
                receipt.cooldownReadyAtTick == 400L && blockedByCooldown
            ),
            FixtureResult(
                "cave-spider-direct-visibility",
                !MobCombatEligibility.directTargetable(caveSpider, archer) &&
                    MobCombatEligibility.directTargetable(caveSpider, mage)
            ),
            FixtureResult(
                "cave-spider-indirect-splash-eligibility",
                MobCombatEligibility.canApplyIndirectSplashDamage(
                    caveSpider, artillery
                )
            ),
            FixtureResult(
                "pigman-lightning-immunity",
                !MobCombatEligibility.canApplyDirectDamage(pigman, zeus)
            ),
            FixtureResult(
                "giant-control-immunity",
                !MobCombatEligibility.canApplyEffect(giant, EffectKind.ICE_SLOW) &&
                    !MobCombatEligibility.canApplyEffect(giant, EffectKind.STUN) &&
                    !MobCombatEligibility.canApplyEffect(giant, EffectKind.KNOCKBACK)
            ),
            FixtureResult(
                "endermite-air-and-mage-immunity",
                endermite.targetLayer == TargetLayer.AIR &&
                    !MobCombatEligibility.canApplyDirectDamage(endermite, mage)
            )
        )
    }
}
