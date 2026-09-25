package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.castle.*
import java.util.UUID

object GuardIndexFixture {
    fun run(): List<FixtureResult> {
        val index=ArenaEntityIndex()
        val guard=GuardRuntime(
            GuardIdentity(
                1,TeamId.BLUE,
                UUID.fromString(
                    "00000000-0000-0000-0000-000000011001"
                )
            )
        )
        index.registerGuard(guard)
        index.assertConsistent()
        val removed=
            index.unregisterGuard(
                guard.identity.entityUuid
            )
        index.assertConsistent()

        return listOf(
            FixtureResult(
                "guard-index-registers-by-uuid-and-team",
                removed===guard &&
                    index.guardsByUuid.isEmpty() &&
                    index.guardsByTeam
                        .getValue(TeamId.BLUE)
                        .isEmpty()
            )
        )
    }
}
