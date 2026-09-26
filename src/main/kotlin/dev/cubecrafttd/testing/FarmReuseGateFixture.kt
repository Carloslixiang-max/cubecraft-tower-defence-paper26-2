package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.tower.visual.BlockKey
import dev.cubecrafttd.tower.visual.TowerBodyConflict
import java.util.UUID

object FarmReuseGateFixture {
    fun run(): List<FixtureResult> {
        val sticky=
            UUID.fromString(
                "00000000-0000-0000-0000-000000060001"
            )
        val transient=
            UUID.fromString(
                "00000000-0000-0000-0000-000000060002"
            )
        val live=
            linkedSetOf(
                sticky,
                transient
            )
        var persisted=
            FarmReuseGatePersistentState()

        val gate=
            FarmReuseGate(
                entityPresence=
                    TrackedEntityPresencePort {
                        it in live
                    },
                persist={
                    persisted=it
                }
            )

        val report=
            ArenaTeardownReport(
                towerBodyConflicts=
                    listOf(
                        TowerBodyConflict(
                            BlockKey(
                                1,2,3
                            ),
                            60L,
                            "minecraft:dirt",
                            "minecraft:stone"
                        )
                    ),
                failedEntityRemovals=
                    setOf(sticky),
                pendingPlayerRestores=
                    emptySet(),
                removedEntityCount=1,
                restoredPlayerCount=2,
                survivingTrackedEntities=
                    setOf(transient)
            )
        val blocked=
            gate.record(report)

        live.remove(transient)
        val partiallyHealed=
            gate.snapshot()

        live.remove(sticky)
        val entitiesHealed=
            gate.snapshot()

        val reloaded=
            FarmReuseGate(
                initial=persisted,
                entityPresence=
                    TrackedEntityPresencePort {
                        false
                    }
            ).snapshot()

        gate.beginVerifiedWorldReset()
        gate.abortVerifiedWorldResetBeforeMutation()
        val resetAborted=
            gate.snapshot()

        gate.beginVerifiedWorldReset()
        val resetStarted=
            gate.snapshot()
        val resetReloaded=
            FarmReuseGate(
                initial=persisted,
                entityPresence=
                    TrackedEntityPresencePort {
                        false
                    }
            ).snapshot()

        gate.clearAfterVerifiedWorldReset()
        val cleared=
            gate.snapshot()

        var crashPersisted=
            FarmReuseGatePersistentState()
        val crashGate=
            FarmReuseGate(
                entityPresence=
                    TrackedEntityPresencePort {
                        false
                    },
                persist={
                    crashPersisted=it
                }
            )
        crashGate
            .markUncleanRestartSuspectedResidue()
        val crashBlocked=
            crashGate.snapshot()
        val crashReloaded=
            FarmReuseGate(
                initial=crashPersisted,
                entityPresence=
                    TrackedEntityPresencePort {
                        false
                    }
            ).snapshot()
        crashGate.beginVerifiedWorldReset()
        crashGate
            .clearAfterVerifiedWorldReset()
        val crashCleared=
            crashGate.snapshot()

        return listOf(
            FixtureResult(
                "farm-reuse-gate-blocks-on-teardown-residue",
                blocked.blocked &&
                    blocked.hardTowerConflictKeys
                        .size==1 &&
                    blocked.liveTrackedEntityResidue==
                        setOf(
                            sticky,
                            transient
                        )
            ),
            FixtureResult(
                "farm-reuse-gate-auto-heals-entity-residue",
                partiallyHealed
                    .liveTrackedEntityResidue==
                    setOf(sticky) &&
                    entitiesHealed
                        .liveTrackedEntityResidue
                        .isEmpty() &&
                    entitiesHealed.blocked
            ),
            FixtureResult(
                "farm-reuse-gate-persists-hard-tower-conflict-across-reload",
                reloaded.blocked &&
                    reloaded.hardTowerConflictKeys
                        .isNotEmpty() &&
                    reloaded.liveTrackedEntityResidue
                        .isEmpty()
            ),
            FixtureResult(
                "farm-reuse-gate-abort-before-mutation-keeps-hard-residue",
                resetAborted.blocked &&
                    !resetAborted
                        .verifiedResetInProgress &&
                    resetAborted
                        .hardTowerConflictKeys
                        .isNotEmpty()
            ),
            FixtureResult(
                "farm-reuse-gate-reset-maintenance-lock-persists",
                resetStarted.blocked &&
                    resetStarted
                        .verifiedResetInProgress &&
                    resetReloaded
                        .verifiedResetInProgress
            ),
            FixtureResult(
                "farm-reuse-gate-clears-only-after-verified-reset",
                !cleared.blocked &&
                    !cleared
                        .verifiedResetInProgress &&
                    persisted.hardTowerConflictKeys
                        .isEmpty() &&
                    persisted.suspectTrackedEntities
                        .isEmpty() &&
                    !persisted
                        .verifiedResetInProgress
            ),
            FixtureResult(
                "farm-reuse-gate-blocks-and-persists-unclean-restart",
                crashBlocked.blocked &&
                    crashBlocked
                        .uncleanRestartSuspectedResidue &&
                    crashReloaded.blocked &&
                    crashReloaded
                        .uncleanRestartSuspectedResidue
            ),
            FixtureResult(
                "farm-reuse-gate-unclean-restart-clears-only-after-verified-reset",
                !crashCleared.blocked &&
                    !crashCleared
                        .uncleanRestartSuspectedResidue &&
                    !crashPersisted
                        .uncleanRestartSuspectedResidue
            )
        )
    }
}
