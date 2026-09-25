package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.map.*
import dev.cubecrafttd.map.importer.MapRuntimeCompiler
import dev.cubecrafttd.mob.*
import java.util.UUID

object ArenaIsolationFixture {
    private fun context(id: String): ArenaContext {
        val map = MapRuntimeCompiler.compile(FarmFullRuntimeFixture.candidate())
        return ArenaContext(ArenaId(id), UUID.nameUUIDFromBytes(id.toByteArray()), map)
    }

    fun run(): List<FixtureResult> {
        val redSender = UUID.fromString("00000000-0000-0000-0000-000000000001")
        val a = context("arena-a")
        val b = context("arena-b")

        val mobA = MobRuntimeState(
            MobIdentity(
                MobInstanceId(101),
                UUID.fromString("00000000-0000-0000-0000-000000000101"),
                "zombie", redSender, TeamId.BLUE, 1
            ),
            MobRouteState("blue_branch_high_x", 0, 0.0, 0.0),
            MobCombatState(100.0, 100.0, MobLifecycleState.MOVING)
        )
        val mobB = MobRuntimeState(
            MobIdentity(
                MobInstanceId(202),
                UUID.fromString("00000000-0000-0000-0000-000000000202"),
                "zombie", redSender, TeamId.BLUE, 1
            ),
            MobRouteState("blue_branch_high_x", 0, 0.0, 0.0),
            MobCombatState(100.0, 100.0, MobLifecycleState.MOVING)
        )

        a.entityIndex.registerMob(mobA)
        b.entityIndex.registerMob(mobB)
        a.entityIndex.assertConsistent()
        b.entityIndex.assertConsistent()

        val isolated = mobA.identity.entityUuid !in b.entityIndex.mobsByUuid &&
            mobB.identity.entityUuid !in a.entityIndex.mobsByUuid

        a.entityIndex.unregisterMob(mobA.identity.entityUuid)
        val bUnaffected = b.entityIndex.mobsByUuid.size == 1

        return listOf(
            FixtureResult("arena-two-context-index-isolation", isolated),
            FixtureResult("arena-unregister-does-not-cross-context", bUnaffected)
        )
    }
}
