package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.combat.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.status.*
import dev.cubecrafttd.tower.*
import java.util.UUID

object AttackApplicationFixture {
    private val owner =
        UUID.fromString("00000000-0000-0000-0000-000000007001")

    private fun mob(
        id: Int,
        mobId: String,
        level: Int,
        hp: Double
    ): MobRuntimeState = MobRuntimeState(
        MobIdentity(
            MobInstanceId(id.toLong()),
            UUID.fromString(
                "00000000-0000-0000-0000-%012d".format(id)
            ),
            mobId,
            owner,
            TeamId.BLUE,
            level
        ),
        MobRouteState("route",0,0.0,0.0),
        MobCombatState(hp,hp,MobLifecycleState.MOVING)
    )

    fun run(): List<FixtureResult> {
        val index = ArenaEntityIndex()
        val zombie = mob(1,"zombie",1,5.0)
        val pigman = mob(2,"pigman",1,150.0)
        val caveSpider = mob(3,"spider",4,20.0)
        index.registerMob(zombie)
        index.registerMob(pigman)
        index.registerMob(caveSpider)

        val applier = AttackBatchApplier(index)
        val ctx = TowerAttackContext(71,owner,100)

        val archerKill = BasicTowerProcessors.archer(
            RecommendedMatureTowerDefinitions.get("archer")
                .stage(TowerPathOption.BOTTOM,2),
            zombie.identity.entityUuid,
            ctx
        )
        val killReport = applier.apply(archerKill)

        val mage = BasicTowerProcessors.mage(
            RecommendedMatureTowerDefinitions.get("mage")
                .stage(TowerPathOption.NONE,4),
            pigman.identity.entityUuid,
            ctx
        )
        val pigmanBefore = pigman.combat.health
        val mageReport = applier.apply(mage)

        val artillery = BasicTowerProcessors.artillery(
            RecommendedMatureTowerDefinitions.get("artillery")
                .stage(TowerPathOption.TOP,4),
            zombie.identity.entityUuid,
            listOf(
                zombie.identity.entityUuid,
                caveSpider.identity.entityUuid
            ),
            ctx
        )
        val caveBefore = caveSpider.combat.health
        val artilleryReport = applier.apply(artillery)

        val archerVsCave = BasicTowerProcessors.archer(
            RecommendedMatureTowerDefinitions.get("archer")
                .stage(TowerPathOption.BOTTOM,2),
            caveSpider.identity.entityUuid,
            ctx
        )
        val caveAfterSplash = caveSpider.combat.health
        val directHiddenReport = applier.apply(archerVsCave)

        val effects = StatusEffectSet(
            RecommendedMatureStatusPolicies.policies
        )
        val firstBurn = effects.apply(
            StatusEffectInstance(
                StatusEffectType.BURN,"mage-a",
                5.0,0,100
            )
        )
        val secondBurn = effects.apply(
            StatusEffectInstance(
                StatusEffectType.BURN,"mage-b",
                15.0,10,300
            )
        )

        return listOf(
            FixtureResult(
                "attack-application-kills-and-attributes-tower-owner",
                killReport.killedCount == 1 &&
                    killReport.impacts.single()
                        .attribution?.creditedPlayerUuid == owner &&
                    zombie.combat.lifecycle ==
                        MobLifecycleState.DEAD
            ),
            FixtureResult(
                "pigman-fire-immune-no-damage-no-burn",
                mageReport.totalDamage == 0.0 &&
                    pigman.combat.health == pigmanBefore &&
                    pigman.statusEffects
                        .get(StatusEffectType.BURN) == null
            ),
            FixtureResult(
                "cave-spider-indirect-artillery-splash-applies",
                caveSpider.combat.health < caveBefore &&
                    artilleryReport.impacts
                        .first {
                            it.targetUuid ==
                                caveSpider.identity.entityUuid
                        }.damageApplied > 0.0
            ),
            FixtureResult(
                "cave-spider-direct-archer-hidden",
                directHiddenReport.totalDamage == 0.0 &&
                    caveSpider.combat.health == caveAfterSplash
            ),
            FixtureResult(
                "support-effects-do-not-stack-or-extend-across-towers",
                firstBurn == secondBurn &&
                    effects.get(StatusEffectType.BURN)
                        ?.expireTick == 100L &&
                    effects.get(StatusEffectType.BURN)
                        ?.magnitude == 5.0
            )
        )
    }
}
