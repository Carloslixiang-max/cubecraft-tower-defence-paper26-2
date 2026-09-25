package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.economy.DeterministicCooldownTracker
import dev.cubecrafttd.match.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.player.*
import dev.cubecrafttd.truth.*
import dev.cubecrafttd.ui.*
import java.util.UUID

object ResolvedGameplayAndAoEUseFixture {
    private fun completeFallback() =
        CompleteFallbackFixtureFactory.create()


    fun run(): List<FixtureResult> {
        val resolved =
            NormalGameplayConfigResolver
                .resolve(
                    completeFallback()
                )

        val missingBlocked = try {
            NormalGameplayConfigResolver
                .resolve(
                    RuntimeFallbackConfigV1()
                )
            false
        } catch (_: IllegalStateException) {
            true
        }

        val owner =
            UUID.fromString(
                "00000000-0000-0000-0000-000000015001"
            )
        val index = ArenaEntityIndex()
        val target = MobRuntimeState(
            MobIdentity(
                MobInstanceId(1),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000015002"
                ),
                "zombie",
                owner,
                TeamId.RED,
                1
            ),
            MobRouteState(
                "route",0,0.0,0.0
            ),
            MobCombatState(
                40.0,40.0,
                MobLifecycleState.MOVING
            )
        )
        index.registerMob(target)

        val cooldown =
            AoEPotionCooldownGate(
                DeterministicCooldownTracker()
            )
        val service =
            AoEPotionUseService(cooldown)

        val meteor =
            RecommendedMatureBazaarDefinitions
                .potion("meteor")
        val valid =
            service.commit(
                meteor,
                AoEPotionRuntimeConfig.Meteor(
                    MeteorPotionResolvedConfig(
                        ResolvedTruth(
                            5L,
                            ResolutionSource
                                .ENGINEERING_FALLBACK
                        )
                    )
                ),
                owner,
                100L,
                listOf(
                    AoEPotionTargetCandidate(
                        target.identity.entityUuid,
                        AoEPotionTargetRelation
                            .ENEMY_TROOPS,
                        true
                    )
                ),
                index
            )

        val blockedAt399 = try {
            service.commit(
                meteor,
                AoEPotionRuntimeConfig.Meteor(
                    MeteorPotionResolvedConfig(
                        ResolvedTruth(
                            5L,
                            ResolutionSource
                                .ENGINEERING_FALLBACK
                        )
                    )
                ),
                owner,
                399L,
                emptyList(),
                index
            )
            false
        } catch (_: IllegalStateException) {
            true
        }

        val badOwner =
            UUID.fromString(
                "00000000-0000-0000-0000-000000015003"
            )
        val failedPlanDidNotConsume = try {
            service.commit(
                meteor,
                AoEPotionRuntimeConfig.Zeus(
                    ZeusPotionResolvedConfig(
                        ResolvedTruth(
                            1.0,
                            ResolutionSource
                                .ENGINEERING_FALLBACK
                        ),
                        ResolvedTruth(
                            1,
                            ResolutionSource
                                .ENGINEERING_FALLBACK
                        )
                    )
                ),
                badOwner,
                200L,
                emptyList(),
                index
            )
            false
        } catch (_: IllegalArgumentException) {
            cooldown.isReady(
                badOwner,
                200L
            )
        }

        return listOf(
            FixtureResult(
                "resolved-normal-gameplay-config",
                resolved.startingBalance.coins==
                    800L &&
                    resolved.startingBalance.exp==
                        0L &&
                    resolved.goldmineFirstIncomeDelayTicks
                        .value==20L &&
                    resolved.troopSpawnCadence
                        .intervalTicks==5L &&
                    resolved.castleAttack
                        .ordinaryAttackIntervalTicks
                        .value==40L &&
                    resolved.guardCombat
                        .rangeBlocks.value==12.0
            ),
            FixtureResult(
                "resolved-normal-config-blocks-missing-mechanics",
                missingBlocked
            ),
            FixtureResult(
                "aoe-use-commit-consumes-15s-after-plan",
                valid.cooldownReadyAtTick==
                    400L &&
                    valid.actionPlan.pulses.size==
                        20
            ),
            FixtureResult(
                "aoe-use-blocked-before-15s-boundary",
                blockedAt399
            ),
            FixtureResult(
                "aoe-invalid-plan-does-not-consume-cooldown",
                failedPlanDidNotConsume
            )
        )
    }
}
