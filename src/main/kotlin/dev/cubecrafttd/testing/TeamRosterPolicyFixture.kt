package dev.cubecrafttd.testing

import dev.cubecrafttd.match.TeamRosterPolicy
import java.util.UUID

object TeamRosterPolicyFixture {
    fun run(): List<FixtureResult> {
        val red = (1L..6L).mapTo(linkedSetOf()) { UUID(0, it) }
        val blue = (7L..12L).mapTo(linkedSetOf()) { UUID(0, it) }
        return listOf(
            FixtureResult("team-roster-accepts-six-per-team-and-six-command-names",
                runCatching { TeamRosterPolicy.validate(red, blue) }.isSuccess &&
                    TeamRosterPolicy.names("a,b,c,d,e,f").size == 6),
            FixtureResult("team-roster-rejects-empty-token-and-case-insensitive-duplicates",
                listOf("", "a,,b", "a,", "Player,player").all {
                    runCatching { TeamRosterPolicy.names(it) }.isFailure
                }),
            FixtureResult("team-roster-rejects-seventh-player-before-bootstrap",
                runCatching { TeamRosterPolicy.validate(red + UUID(0, 13), blue) }.isFailure &&
                    runCatching { TeamRosterPolicy.names("a,b,c,d,e,f,g") }.isFailure),
            FixtureResult("team-roster-rejects-shared-player-and-empty-team",
                runCatching { TeamRosterPolicy.validate(red, blue + red.first()) }.isFailure &&
                    runCatching { TeamRosterPolicy.validate(emptySet(), blue) }.isFailure)
        )
    }
}
