package dev.cubecrafttd.testing

import dev.cubecrafttd.match.*
import java.util.UUID

object HistoricalPregameArmageddonVoteFixture {
    fun run(): List<FixtureResult> {
        val red=UUID.fromString(
            "00000000-0000-0000-0000-000000055001"
        )
        val blue=UUID.fromString(
            "00000000-0000-0000-0000-000000055002"
        )
        val outsider=UUID.fromString(
            "00000000-0000-0000-0000-000000055003"
        )

        fun runtime()=
            HistoricalPregameArmageddonVoteRuntime(
                eligiblePlayers=
                    setOf(red,blue),
                runnableTypes=
                    ArmageddonType.entries
                        .toSet()
            )

        val noVotes=
            runtime().resolve(
                ArmageddonType.HORDE
            )

        val concreteRuntime=
            runtime()
        concreteRuntime.cast(
            red,
            HistoricalPregameArmageddonVoteOption
                .LIGHTNING
        )
        concreteRuntime.cast(
            blue,
            HistoricalPregameArmageddonVoteOption
                .LIGHTNING
        )
        val concrete=
            concreteRuntime.resolve(
                ArmageddonType.WITHER
            )

        val randomRuntime=
            runtime()
        randomRuntime.cast(
            red,
            HistoricalPregameArmageddonVoteOption
                .RANDOM
        )
        randomRuntime.cast(
            blue,
            HistoricalPregameArmageddonVoteOption
                .RANDOM
        )
        val votedRandom=
            randomRuntime.resolve(
                ArmageddonType.WITHER
            )

        val tieRuntime=
            runtime()
        tieRuntime.cast(
            red,
            HistoricalPregameArmageddonVoteOption
                .WITHER
        )
        tieRuntime.cast(
            blue,
            HistoricalPregameArmageddonVoteOption
                .HORDE
        )
        val tie=
            tieRuntime.resolve(
                ArmageddonType.LIGHTNING
            )

        val outsiderRejected=
            runCatching {
                runtime().cast(
                    outsider,
                    HistoricalPregameArmageddonVoteOption
                        .WITHER
                )
            }.isFailure

        val restricted=
            HistoricalPregameArmageddonVoteRuntime(
                eligiblePlayers=setOf(red),
                runnableTypes=
                    setOf(
                        ArmageddonType.WITHER
                    )
            )
        val unrunnableRejected=
            runCatching {
                restricted.cast(
                    red,
                    HistoricalPregameArmageddonVoteOption
                        .LIGHTNING
                )
            }.isFailure

        return listOf(
            FixtureResult(
                "historical-pregame-armageddon-no-votes-random",
                noVotes.selection.type==
                    ArmageddonType.HORDE &&
                    noVotes.selection.source==
                        ArmageddonSelectionSource
                            .RANDOM_RESULT_ALREADY_RESOLVED &&
                    noVotes.resolution==
                        HistoricalPregameArmageddonResolution
                            .NO_VOTES_RANDOM
            ),
            FixtureResult(
                "historical-pregame-armageddon-concrete-vote",
                concrete.selection.type==
                    ArmageddonType.LIGHTNING &&
                    concrete.selection.source==
                        ArmageddonSelectionSource
                            .EXPLICIT_VOTE_RESULT
            ),
            FixtureResult(
                "historical-pregame-armageddon-voted-random",
                votedRandom.selection.type==
                    ArmageddonType.WITHER &&
                    votedRandom.resolution==
                        HistoricalPregameArmageddonResolution
                            .VOTED_RANDOM
            ),
            FixtureResult(
                "historical-pregame-armageddon-tie-explicit-engineering-fallback",
                tie.selection.type==
                    ArmageddonType.LIGHTNING &&
                    tie.selection.source==
                        ArmageddonSelectionSource
                            .ENGINEERING_VOTE_FALLBACK &&
                    tie.resolution==
                        HistoricalPregameArmageddonResolution
                            .ENGINEERING_TIE_RANDOM_FALLBACK
            ),
            FixtureResult(
                "historical-pregame-armageddon-rejects-outsider",
                outsiderRejected
            ),
            FixtureResult(
                "historical-pregame-armageddon-rejects-unrunnable-concrete",
                unrunnableRejected
            )
        )
    }
}
