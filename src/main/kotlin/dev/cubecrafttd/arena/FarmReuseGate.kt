package dev.cubecrafttd.arena

import java.util.UUID

data class FarmReuseGatePersistentState(
    val hardTowerConflictKeys:
        Set<String> = emptySet(),
    val suspectTrackedEntities:
        Set<UUID> = emptySet(),
    val verifiedResetInProgress:
        Boolean = false,
    val uncleanRestartSuspectedResidue:
        Boolean = false
)

data class FarmReuseGateSnapshot(
    val hardTowerConflictKeys:
        Set<String>,
    val liveTrackedEntityResidue:
        Set<UUID>,
    val verifiedResetInProgress:
        Boolean,
    val uncleanRestartSuspectedResidue:
        Boolean = false
) {
    val blocked: Boolean
        get() =
            verifiedResetInProgress ||
                uncleanRestartSuspectedResidue ||
                hardTowerConflictKeys.isNotEmpty() ||
                liveTrackedEntityResidue.isNotEmpty()

    fun summary(): String =
        "blocked=" + blocked +
            ", resetInProgress=" +
            verifiedResetInProgress +
            ", worldIntegrityUnknown=" +
            uncleanRestartSuspectedResidue +
            ", towerConflicts=" +
            hardTowerConflictKeys.size +
            ", liveEntityResidue=" +
            liveTrackedEntityResidue.size
}

/**
 * Prevents a physical arena from being reused after teardown residue.
 *
 * Entity residue is self-healing: every snapshot rechecks the live world and
 * automatically forgets UUIDs that no longer exist. Tower-body conflicts are
 * hard residue because teardown intentionally did not overwrite an unexpected
 * external block edit; they remain locked until an explicit verified map reset.
 */
class FarmReuseGate(
    initial:
        FarmReuseGatePersistentState =
            FarmReuseGatePersistentState(),
    private val entityPresence:
        TrackedEntityPresencePort,
    private val persist:
        (FarmReuseGatePersistentState)->Unit =
            {}
) {
    private val hardTowerConflictKeys=
        linkedSetOf<String>()
            .apply {
                addAll(
                    initial
                        .hardTowerConflictKeys
                )
            }

    private val suspectTrackedEntities=
        linkedSetOf<UUID>()
            .apply {
                addAll(
                    initial
                        .suspectTrackedEntities
                )
            }

    private var verifiedResetInProgress=
        initial.verifiedResetInProgress

    private var uncleanRestartSuspectedResidue=
        initial.uncleanRestartSuspectedResidue

    fun markWorldIntegrityUnknown() {
        uncleanRestartSuspectedResidue=true
        save()
    }

    fun markUncleanRestartSuspectedResidue() {
        markWorldIntegrityUnknown()
    }

    fun beginVerifiedWorldReset() {
        verifiedResetInProgress=true
        save()
    }

    fun abortVerifiedWorldResetBeforeMutation() {
        verifiedResetInProgress=false
        save()
    }

    fun record(
        report: ArenaTeardownReport
    ): FarmReuseGateSnapshot {
        report.towerBodyConflicts
            .forEach { conflict ->
                hardTowerConflictKeys +=
                    conflict.blockKey
                        .toString() +
                    "@tower=" +
                    conflict.towerInstanceId
            }

        suspectTrackedEntities +=
            report.failedEntityRemovals
        suspectTrackedEntities +=
            report.survivingTrackedEntities

        refreshEntityResidue()
        save()
        return currentSnapshot()
    }

    fun snapshot():
        FarmReuseGateSnapshot {
        val changed=
            refreshEntityResidue()
        if(changed) {
            save()
        }
        return currentSnapshot()
    }

    fun clearAfterVerifiedWorldReset() {
        hardTowerConflictKeys.clear()
        suspectTrackedEntities.clear()
        verifiedResetInProgress=false
        uncleanRestartSuspectedResidue=false
        save()
    }

    private fun refreshEntityResidue():
        Boolean {
        val before=
            suspectTrackedEntities.size
        suspectTrackedEntities
            .removeIf { uuid ->
                runCatching {
                    !entityPresence.exists(
                        uuid
                    )
                }.getOrDefault(false)
            }
        return suspectTrackedEntities
            .size != before
    }

    private fun currentSnapshot():
        FarmReuseGateSnapshot =
        FarmReuseGateSnapshot(
            hardTowerConflictKeys=
                hardTowerConflictKeys
                    .toSet(),
            liveTrackedEntityResidue=
                suspectTrackedEntities
                    .toSet(),
            verifiedResetInProgress=
                verifiedResetInProgress,
            uncleanRestartSuspectedResidue=
                uncleanRestartSuspectedResidue
        )

    private fun save() {
        persist(
            FarmReuseGatePersistentState(
                hardTowerConflictKeys=
                    hardTowerConflictKeys
                        .toSet(),
                suspectTrackedEntities=
                    suspectTrackedEntities
                        .toSet(),
                verifiedResetInProgress=
                    verifiedResetInProgress,
                uncleanRestartSuspectedResidue=
                    uncleanRestartSuspectedResidue
            )
        )
    }
}
