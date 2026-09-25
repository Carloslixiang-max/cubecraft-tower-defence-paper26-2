package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.player.*
import dev.cubecrafttd.truth.*
import dev.cubecrafttd.ui.*
import java.util.UUID

object PlayerCombatPotionFixture {
    private val player =
        UUID.fromString(
            "00000000-0000-0000-0000-000000005000"
        )

    private fun <T> fallback(
        value: T
    ) = ResolvedTruth(
        value,
        ResolutionSource
            .ENGINEERING_FALLBACK
    )

    private fun mob(
        id: Long,
        mobId: String,
        level: Int,
        attackedTeam: TeamId,
        hp: Double
    ) = MobRuntimeState(
        MobIdentity(
            MobInstanceId(id),
            UUID.nameUUIDFromBytes(
                "mob-$id".toByteArray()
            ),
            mobId,
            player,
            attackedTeam,
            level
        ),
        MobRouteState(
            "route",0,0.0,
            id.toDouble()
        ),
        MobCombatState(
            hp,hp,
            MobLifecycleState.MOVING
        )
    )

    fun run(): List<FixtureResult> {
        val index=ArenaEntityIndex()
        val swordTarget=
            mob(1,"zombie",1,TeamId.RED,8.0)
        val bowTarget=
            mob(2,"zombie",1,TeamId.RED,10.0)
        val wither=
            mob(3,"wither",1,TeamId.RED,5000.0)
        val enemy1=
            mob(4,"zombie",1,TeamId.RED,40.0)
        val enemy2=
            mob(5,"zombie",1,TeamId.RED,40.0)
        val enemy3=
            mob(6,"zombie",1,TeamId.RED,40.0)
        listOf(
            swordTarget,bowTarget,wither,
            enemy1,enemy2,enemy3
        ).forEach(index::registerMob)

        val combat=
            PlayerWeaponCombatService(index)
        val sword=combat.hit(
            PlayerWeaponHitCommand(
                player,
                swordTarget.identity.entityUuid,
                PlayerWeaponAttackKind.SWORD,
                actualDamage=8.0
            )
        )
        val bow=combat.hit(
            PlayerWeaponHitCommand(
                player,
                bowTarget.identity.entityUuid,
                PlayerWeaponAttackKind.BOW,
                actualDamage=4.5
            )
        )

        val charge=
            WeaponAbilityChargeTracker()
        val key=WeaponAbilityKey(
            player,WeaponKind.SWORD,"freeze"
        )
        val c1=charge.recordValidHit(
            key,fallback(2)
        )
        val c2=charge.recordValidHit(
            key,fallback(2)
        )

        val killLedger=
            EconomyLedger()
        val rewardService=
            MobKillRewardService(
                killLedger,
                MobKillRewardResolver { _,_ ->
                    fallback(15L)
                }
            )
        val swordAttribution = sword.attribution!!
        val reward=
            rewardService.award(
                swordTarget,
                swordAttribution,
                100,77
            )
        val rewardAgain=
            rewardService.award(
                swordTarget,
                swordAttribution,
                101,78
            )

        val zeusDef=
            RecommendedMatureBazaarDefinitions
                .potion("zeus")
        val candidates=listOf(
            AoEPotionTargetCandidate(
                enemy1.identity.entityUuid,
                AoEPotionTargetRelation.ENEMY_TROOPS,
                true
            ),
            AoEPotionTargetCandidate(
                enemy2.identity.entityUuid,
                AoEPotionTargetRelation.ENEMY_TROOPS,
                true
            ),
            AoEPotionTargetCandidate(
                enemy3.identity.entityUuid,
                AoEPotionTargetRelation.ENEMY_TROOPS,
                true
            ),
            AoEPotionTargetCandidate(
                wither.identity.entityUuid,
                AoEPotionTargetRelation.ENEMY_TROOPS,
                true
            )
        )
        val zeusPlan=
            AoEPotionRuntimePlanner.plan(
                zeusDef,
                AoEPotionRuntimeConfig.Zeus(
                    ZeusPotionResolvedConfig(
                        fallback(70.0),
                        fallback(8)
                    )
                ),
                player,200,
                candidates,index
            )

        val meteor=
            AoEPotionRuntimePlanner.plan(
                RecommendedMatureBazaarDefinitions
                    .potion("meteor"),
                AoEPotionRuntimeConfig.Meteor(
                    MeteorPotionResolvedConfig(
                        fallback(5L)
                    )
                ),
                player,300,
                candidates,index
            )

        return listOf(
            FixtureResult(
                "player-sword-uses-adapter-final-damage-and-kill-attribution",
                sword.damageApplied==8.0 &&
                    sword.killed &&
                    sword.attribution
                        ?.creditedPlayerUuid==
                        player &&
                    sword.attribution
                        ?.awardsPlayerKillCoins==
                        true
            ),
            FixtureResult(
                "player-bow-partial-damage",
                !bow.killed &&
                    bowTarget.combat.health==5.5
            ),
            FixtureResult(
                "weapon-ability-charge-truth-gated",
                !c1.triggered &&
                    c2.triggered &&
                    charge.current(key)==0
            ),
            FixtureResult(
                "kill-reward-truth-gated-idempotent",
                reward==15L &&
                    rewardAgain==0L &&
                    killLedger.balance(
                        EconomyAccount(
                            TeamId.RED,
                            player,
                            EconomyCurrency.MATCH_COINS
                        )
                    )==15L
            ),
            FixtureResult(
                "aoe-zeus-excludes-wither-and-respects-max-four",
                zeusPlan.pulses.size==8 &&
                    zeusPlan.pulses.all{
                        (
                            wither.identity.entityUuid
                                !in it.targetMobUuids
                        ) &&
                        it.targetMobUuids.size==3
                    }
            ),
            FixtureResult(
                "aoe-meteor-known-count-and-damage",
                meteor.pulses.size==20 &&
                    meteor.pulses.all{
                        it.damagePerTarget==8.88
                    } &&
                    meteor.pulses.last()
                        .tickOffset==95L
            )
        )
    }
}
