package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.castle.*
import dev.cubecrafttd.match.*
import java.util.UUID

object NormalMatchClockRuntimeFixture {
    fun run():List<FixtureResult> {
        val context=ArenaContext(
            ArenaId("clock"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000023100"
            ),
            TestingMapFactory.minimal()
        )
        context.state=ArenaState.RUNNING

        val guards=(1L..4L).map { id ->
            GuardRuntime(
                GuardIdentity(
                    id,
                    if(id<=2)
                        TeamId.RED
                    else
                        TeamId.BLUE,
                    UUID.nameUUIDFromBytes(
                        "clock-guard-$id"
                            .toByteArray()
                    )
                )
            )
        }
        guards.forEach(
            context.entityIndex::registerGuard
        )

        var starts=0
        var startedType:
            ArmageddonType?=null
        val runtime=
            NormalMatchClockRuntime(
                startGameTick=100L,
                selection=
                    ResolvedArmageddonSelection(
                        ArmageddonType.WITHER,
                        ArmageddonSelectionSource
                            .EXPLICIT_VOTE_RESULT
                    ),
                tiePolicy=
                    TimeoutTiePolicy.DRAW,
                armageddonPort=
                    ArmageddonStartPort {
                        type,_,_ ->
                        starts++
                        startedType=type
                    }
            )

        context.gameTick=
            100L + 29_999L
        val before=
            runtime.tick(context,guards)

        context.gameTick=
            100L + 30_000L
        val activation=
            runtime.tick(context,guards)

        context.gameTick=
            100L + 30_001L
        val once=
            runtime.tick(context,guards)

        context.castles
            .getValue(TeamId.RED)
            .health=200.0
        context.castles
            .getValue(TeamId.BLUE)
            .health=150.0
        context.gameTick=
            100L + 48_000L
        val end=
            runtime.tick(context,guards)

        val replacementRuntime=
            NormalMatchClockRuntime(
                startGameTick=0L,
                selection=
                    ResolvedArmageddonSelection(
                        ArmageddonType.WITHER,
                        ArmageddonSelectionSource
                            .ENGINEERING_TEST
                    ),
                tiePolicy=
                    TimeoutTiePolicy.DRAW,
                armageddonPort=
                    ArmageddonStartPort {
                        _,_,_ -> Unit
                    }
            )
        val replacement=
            replacementRuntime
                .replaceSelectionBeforeArmageddon(
                    ResolvedArmageddonSelection(
                        ArmageddonType.HORDE,
                        ArmageddonSelectionSource
                            .ENGINEERING_PLAYER_VOTE_RESULT
                    )
                )
        val lockAfterStart=
            runCatching {
                runtime
                    .replaceSelectionBeforeArmageddon(
                        ResolvedArmageddonSelection(
                            ArmageddonType.HORDE,
                            ArmageddonSelectionSource
                                .ENGINEERING_PLAYER_VOTE_RESULT
                        )
                    )
            }.isFailure

        return listOf(
            FixtureResult(
                "match-clock-before-25m",
                before.phase==
                    MatchPhase.PRE_ARMAGEDDON &&
                    starts==1 // evaluated after all calls
            ),
            FixtureResult(
                "match-clock-armageddon-exactly-once",
                activation.phase==
                    MatchPhase.ARMAGEDDON &&
                    activation.armageddonActivated!=
                        null &&
                    once.armageddonActivated==
                        null &&
                    starts==1 &&
                    startedType==
                        ArmageddonType.WITHER
            ),
            FixtureResult(
                "match-clock-armageddon-common-effects",
                guards.all {
                    it.lifecycle==
                        GuardLifecycleState
                            .DISABLED_ARMAGEDDON
                } &&
                    context.castles
                        .values.all {
                            it.health<=
                                it.maxHealth*0.25
                        }
            ),
            FixtureResult(
                "match-clock-hard-end-40m",
                end.phase==
                    MatchPhase.FINISHED &&
                    (end.outcome as?
                        MatchOutcome.Winner)
                        ?.team==
                        TeamId.RED
            ),
            FixtureResult(
                "match-clock-does-not-resolve-vote-winner",
                runtime.selection().source==
                    ArmageddonSelectionSource
                        .EXPLICIT_VOTE_RESULT
            ),
            FixtureResult(
                "match-clock-selection-replaceable-before-armageddon",
                replacement.type==
                    ArmageddonType.HORDE &&
                    replacementRuntime.selection().source==
                        ArmageddonSelectionSource
                            .ENGINEERING_PLAYER_VOTE_RESULT
            ),
            FixtureResult(
                "match-clock-selection-locks-after-armageddon",
                lockAfterStart
            )
        )
    }
}
