package dev.cubecrafttd.testing

import dev.cubecrafttd.match.*
import java.util.UUID

object EngineeringArmageddonVoteFixture {
    fun run(): List<FixtureResult> {
        val red=UUID.fromString(
            "00000000-0000-0000-0000-000000050001"
        )
        val blue=UUID.fromString(
            "00000000-0000-0000-0000-000000050002"
        )
        val outsider=UUID.fromString(
            "00000000-0000-0000-0000-000000050003"
        )
        val runtime=
            EngineeringArmageddonVoteRuntime(
                eligiblePlayers=setOf(red,blue),
                allowedTypes=ArmageddonType.entries.toSet(),
                defaultType=ArmageddonType.WITHER
            )

        val initial=runtime.snapshot()
        val redLightning=runtime.cast(
            red,ArmageddonType.LIGHTNING
        )
        val split=runtime.cast(
            blue,ArmageddonType.HORDE
        )
        val united=runtime.cast(
            blue,ArmageddonType.LIGHTNING
        )

        val outsiderRejected=runCatching {
            runtime.cast(
                outsider,ArmageddonType.WITHER
            )
        }.isFailure

        val restricted=
            EngineeringArmageddonVoteRuntime(
                eligiblePlayers=setOf(red),
                allowedTypes=setOf(
                    ArmageddonType.WITHER,
                    ArmageddonType.HORDE
                ),
                defaultType=ArmageddonType.HORDE
            )
        val unavailableRejected=runCatching {
            restricted.cast(
                red,ArmageddonType.LIGHTNING
            )
        }.isFailure

        return listOf(
            FixtureResult(
                "engineering-armageddon-vote-no-vote-default",
                initial.selection.type==ArmageddonType.WITHER &&
                    initial.resolution==
                        EngineeringArmageddonVoteResolution.DEFAULT_NO_VOTES &&
                    initial.selection.source==
                        ArmageddonSelectionSource.ENGINEERING_VOTE_FALLBACK
            ),
            FixtureResult(
                "engineering-armageddon-vote-unique-highest",
                redLightning.snapshot.selection.type==
                    ArmageddonType.LIGHTNING &&
                    redLightning.snapshot.resolution==
                        EngineeringArmageddonVoteResolution.UNIQUE_HIGHEST_VOTE &&
                    redLightning.snapshot.selection.source==
                        ArmageddonSelectionSource.ENGINEERING_PLAYER_VOTE_RESULT
            ),
            FixtureResult(
                "engineering-armageddon-vote-tie-falls-back",
                split.snapshot.selection.type==
                    ArmageddonType.WITHER &&
                    split.snapshot.resolution==
                        EngineeringArmageddonVoteResolution.DEFAULT_TIE
            ),
            FixtureResult(
                "engineering-armageddon-vote-can-change",
                united.previousVote==ArmageddonType.HORDE &&
                    united.snapshot.count(
                        ArmageddonType.LIGHTNING
                    )==2 &&
                    united.snapshot.selection.type==
                        ArmageddonType.LIGHTNING
            ),
            FixtureResult(
                "engineering-armageddon-vote-rejects-outsider",
                outsiderRejected
            ),
            FixtureResult(
                "engineering-armageddon-vote-rejects-unrunnable-type",
                unavailableRejected
            )
        )
    }
}
