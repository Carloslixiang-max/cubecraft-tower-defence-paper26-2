package dev.cubecrafttd.testing

import dev.cubecrafttd.recovery.AutomaticRecoveryGuard
import java.util.UUID

object AutomaticRecoveryGuardFixture {
    fun run(): List<FixtureResult> {
        val player = UUID(118, 1)
        var active = false
        val guard = AutomaticRecoveryGuard { active }
        val initial = guard.permitsRestore(player)
        active = true
        val duringMatch = guard.permitsRestore(player)
        // The same guard is queried again when a previously queued callback runs.
        val queuedBeforeMatch = guard.permitsRestore(player)
        active = false
        val afterDeparture = guard.permitsRestore(player)
        guard.deferUntilNextProcess(player)
        val armedProbe = guard.permitsRestore(player)
        val afterRestart = AutomaticRecoveryGuard { false }.permitsRestore(player)
        val ownerLookupFailure = AutomaticRecoveryGuard { error("Unavailable arena registry") }.permitsRestore(player)
        return listOf(
            FixtureResult("recovery.unowned-pending-online-restores", initial),
            FixtureResult("recovery.healthy-match-keeps-durable-snapshot", !duringMatch),
            FixtureResult("recovery.queued-callback-rechecks-new-match-ownership", initial && !queuedBeforeMatch),
            FixtureResult("recovery.departed-player-becomes-recoverable", afterDeparture),
            FixtureResult("recovery.restart-probe-held-until-new-process", !armedProbe && afterRestart),
            FixtureResult("recovery.unknown-ownership-fails-closed", !ownerLookupFailure)
        )
    }
}
