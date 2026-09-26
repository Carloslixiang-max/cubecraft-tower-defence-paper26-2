package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.match.*
import dev.cubecrafttd.progression.*
import dev.cubecrafttd.truth.*
import java.util.UUID

object MatchParticipantDepartureFixture {
    fun run(): List<FixtureResult> {
        val red=UUID.fromString(
            "00000000-0000-0000-0000-000000051001"
        )
        val blue=UUID.fromString(
            "00000000-0000-0000-0000-000000051002"
        )
        val outsider=UUID.fromString(
            "00000000-0000-0000-0000-000000051003"
        )
        val context=ArenaContext(
            ArenaId("departure"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000051100"
            ),
            TestingMapFactory.minimal()
        )
        context.redTeam.players += red
        context.blueTeam.players += blue

        fun observed(value: Long)=
            ResolvedTruth(
                value,
                ResolutionSource.OBSERVED_ORIGINAL
            )

        val session=
            MatchSessionState(
                preset=
                    MatchRulePresetFactory
                        .recommendedMature(
                            MatchMode.NORMAL,
                            observed(500L),
                            observed(0L)
                        ),
                players=
                    linkedMapOf(
                        red to
                            PlayerMatchSessionState(
                                red,
                                TroopProgressionState()
                            ),
                        blue to
                            PlayerMatchSessionState(
                                blue,
                                TroopProgressionState()
                            )
                    )
            )
        val departed=linkedSetOf<UUID>()
        val service=
            MatchParticipantDepartureService(
                context,
                session,
                departed
            )

        val first=service.depart(red)
        val duplicate=service.depart(red)
        val unknown=service.depart(outsider)

        return listOf(
            FixtureResult(
                "departure-removes-player-from-active-session",
                first?.team==TeamId.RED &&
                    first.newlyDeparted &&
                    first.removedFromActiveSession &&
                    first.activePlayersRemaining==1 &&
                    red !in session.players
            ),
            FixtureResult(
                "departure-preserves-historical-team-membership",
                red in context.redTeam.players &&
                    red in departed
            ),
            FixtureResult(
                "departure-keeps-other-player-active",
                blue in session.players &&
                    blue !in departed
            ),
            FixtureResult(
                "departure-is-idempotent",
                duplicate?.newlyDeparted==false &&
                    duplicate?.removedFromActiveSession==false &&
                    duplicate?.activePlayersRemaining==1
            ),
            FixtureResult(
                "departure-ignores-nonparticipant",
                unknown==null
            )
        )
    }
}
