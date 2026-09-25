package dev.cubecrafttd.testing

import dev.cubecrafttd.combat.*
import dev.cubecrafttd.status.StatusEffectType
import dev.cubecrafttd.tower.*
import java.util.UUID

object BasicTowerProcessorFixture {
    fun run(): List<FixtureResult> {
        val owner = UUID.fromString("00000000-0000-0000-0000-000000007777")
        val t1 = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val t2 = UUID.fromString("00000000-0000-0000-0000-000000000002")
        val ctx = TowerAttackContext(99, owner, 100)

        val artillery = BasicTowerProcessors.artillery(
            RecommendedMatureTowerDefinitions.get("artillery")
                .stage(TowerPathOption.TOP,4),
            t1, listOf(t1,t2), ctx
        )
        val mage = BasicTowerProcessors.mage(
            RecommendedMatureTowerDefinitions.get("mage")
                .stage(TowerPathOption.NONE,4),
            t1, ctx
        )
        val iceHit = BasicTowerProcessors.ice(
            RecommendedMatureTowerDefinitions.get("ice")
                .stage(TowerPathOption.NONE,4),
            t1, ctx, chanceRoll = 0.2
        )
        val iceMiss = BasicTowerProcessors.ice(
            RecommendedMatureTowerDefinitions.get("ice")
                .stage(TowerPathOption.NONE,4),
            t1, ctx, chanceRoll = 0.8
        )
        val poison = BasicTowerProcessors.poison(
            RecommendedMatureTowerDefinitions.get("poison")
                .stage(TowerPathOption.TOP,4),
            t1, ctx
        )
        val quake = BasicTowerProcessors.quake(
            RecommendedMatureTowerDefinitions.get("quake")
                .stage(TowerPathOption.NONE,4),
            t1, listOf(t1,t2), ctx
        )

        val points = listOf(
            MobHitboxPoint(t1,0.0,0.0,0.0),
            MobHitboxPoint(t2,1.0,0.0,1.0),
            MobHitboxPoint(UUID.fromString("00000000-0000-0000-0000-000000000003"),5.0,0.0,0.0)
        )
        val aoe = SphericalAoeResolver.withinRadius(0.0,0.0,0.0,2.0,points)

        return listOf(
            FixtureResult(
                "artillery-direct-frag-and-stun",
                artillery.impacts.size == 2 &&
                    artillery.impacts.first { it.targetUuid == t1 }.damages.single().amount == 20.0 &&
                    artillery.impacts.first { it.targetUuid == t2 }.damages.single().amount == 12.5 &&
                    artillery.impacts.all { it.effects.any { e -> e.type == StatusEffectType.STUN } }
            ),
            FixtureResult(
                "mage-ignition-and-burn",
                mage.impacts.single().damages.single().amount == 10.0 &&
                    mage.impacts.single().effects.single().magnitude == 15.0 &&
                    mage.impacts.single().effects.single().expireTick == 340L
            ),
            FixtureResult(
                "ice-l4-deterministic-chance",
                iceHit.impacts.single().damages.single().amount == 4.0 &&
                    iceMiss.impacts.single().damages.isEmpty()
            ),
            FixtureResult(
                "poison-duration-metadata",
                poison.impacts.single().effects.single().type == StatusEffectType.POISON &&
                    poison.impacts.single().effects.single().expireTick == 2500L
            ),
            FixtureResult(
                "quake-aoe-stun",
                quake.impacts.size == 2 &&
                    quake.impacts.all { it.damages.single().amount == 11.0 } &&
                    quake.impacts.all { it.effects.single().expireTick == 220L }
            ),
            FixtureResult(
                "spherical-aoe-resolver",
                aoe.map { it.entityUuid }.toSet() == setOf(t1,t2)
            )
        )
    }
}
