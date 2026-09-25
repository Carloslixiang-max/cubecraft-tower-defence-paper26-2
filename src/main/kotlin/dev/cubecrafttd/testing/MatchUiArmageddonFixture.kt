package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.castle.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.match.*
import dev.cubecrafttd.truth.*
import dev.cubecrafttd.ui.*
import java.util.UUID

object MatchUiArmageddonFixture {
    fun run(): List<FixtureResult> {
        val timing = NormalMatchTiming()
        val redCastle = CastleRuntime(
            TeamId.RED,
            health=900.0
        )
        val blueCastle = CastleRuntime(
            TeamId.BLUE,
            health=100.0
        )
        val guards = listOf(
            GuardRuntime(
                GuardIdentity(
                    1,TeamId.RED,
                    UUID.fromString(
                        "00000000-0000-0000-0000-000000000001"
                    )
                )
            ),
            GuardRuntime(
                GuardIdentity(
                    2,TeamId.RED,
                    UUID.fromString(
                        "00000000-0000-0000-0000-000000000002"
                    )
                )
            ),
            GuardRuntime(
                GuardIdentity(
                    3,TeamId.BLUE,
                    UUID.fromString(
                        "00000000-0000-0000-0000-000000000003"
                    )
                )
            ),
            GuardRuntime(
                GuardIdentity(
                    4,TeamId.BLUE,
                    UUID.fromString(
                        "00000000-0000-0000-0000-000000000004"
                    )
                )
            )
        )
        val arm = ArmageddonService.activate(
            ArmageddonType.HORDE,
            mapOf(
                TeamId.RED to redCastle,
                TeamId.BLUE to blueCastle
            ),
            guards
        )

        val quick = RecommendedMaturePricing
            .startingBalance(
                PricingMode.QUICK_START,
                ResolvedTruth(
                    500L,
                    ResolutionSource.ENGINEERING_FALLBACK
                ),
                ResolvedTruth(
                    0L,
                    ResolutionSource.OBSERVED_ORIGINAL
                )
            )
        val normal = RecommendedMaturePricing
            .startingBalance(
                PricingMode.NORMAL,
                ResolvedTruth(
                    500L,
                    ResolutionSource.ENGINEERING_FALLBACK
                ),
                ResolvedTruth(
                    0L,
                    ResolutionSource.OBSERVED_ORIGINAL
                )
            )

        val settings19 = PlayerMatchSettings(19)
        val blocked = try {
            settings19.setAutoCentre(false)
            false
        } catch (_: IllegalStateException) {
            true
        }
        val settings20 = PlayerMatchSettings(20)
        settings20.setAutoCentre(false)

        return listOf(
            FixtureResult(
                "normal-match-25-plus-15-minutes",
                timing.armageddonStartTick ==
                    30000L &&
                    timing.armageddonDurationTicks ==
                        18000L &&
                    timing.hardEndTick == 48000L
            ),
            FixtureResult(
                "armageddon-caps-only-castles-above-25-percent",
                redCastle.health == 250.0 &&
                    blueCastle.health == 100.0
            ),
            FixtureResult(
                "armageddon-disables-four-guards",
                arm.disabledGuards == 4 &&
                    guards.all {
                        it.lifecycle ==
                            GuardLifecycleState
                                .DISABLED_ARMAGEDDON
                    }
            ),
            FixtureResult(
                "quick-start-mature-balance",
                quick.coins == 1500L &&
                    quick.exp == 100L &&
                    normal.coins == 500L &&
                    normal.exp == 0L
            ),
            FixtureResult(
                "auto-centre-disable-unlocks-at-20-wins",
                blocked &&
                    !settings20.autoCentreTowers
            ),
            FixtureResult(
                "timeout-higher-castle-health-wins",
                MatchOutcomeResolver.atHardTimeout(
                    CastleRuntime(
                        TeamId.RED,health=300.0
                    ),
                    CastleRuntime(
                        TeamId.BLUE,health=200.0
                    ),
                    TimeoutTiePolicy.DRAW
                ) is MatchOutcome.Winner
            )
        )
    }
}
