package dev.cubecrafttd.testing

import dev.cubecrafttd.paper.bukkit.*

object PaperLiveCertificationFixture {
    fun run(): List<FixtureResult> {
        val corePassed=
            PaperStage4GateStatus(
                domainFixturesPassed=true,
                farmMapCheckPassed=true,
                adapterSmokePassed=true,
                playerSnapshotRoundTripPassed=true,
                restartRecoveryPassed=true,
                towerStress60Passed=true,
                cleanRestartCycles=2,
                cleanArenaRoundTrips=2,
                previousBootWasUnclean=false,
                consecutiveCleanArenaRoundTrips=2,
                verifiedFarmResetPassed=true
            )
        val diagnostics=
            PaperAdapterDiagnostics
                .current()
        val remaining=
            diagnostics
                .remainingLiveGates(
                    corePassed
                )

        val coreLinkedRemoved=
            "player snapshot lossless roundtrip on real server" !in
                remaining &&
            "restart recovery" !in
                remaining &&
            "60+ tower real-server performance certification" !in
                remaining &&
            "consecutive-round world residue/reuse real-server certification" !in
                remaining &&
            "verified Farm reset/repair real-server certification" !in
                remaining

        val oneCleanOnly=
            corePassed.copy(
                cleanArenaRoundTrips=1,
                consecutiveCleanArenaRoundTrips=1
            )
        val oneCleanRemaining=
            diagnostics
                .remainingLiveGates(
                    oneCleanOnly
                )

        val resetNotPassed=
            corePassed.copy(
                verifiedFarmResetPassed=false
            )
        val resetNotPassedRemaining=
            diagnostics
                .remainingLiveGates(
                    resetNotPassed
                )

        val queuePassed=
            corePassed.copy(
                queueJoinObserved=true,
                queueHudObserved=true,
                queueArmageddonGuiVoteObserved=true,
                queuePricingGuiVoteObserved=true,
                queueCountdownStartObserved=true,
                queueActiveLeaveObserved=true
            )
        val queuePassedRemaining=
            diagnostics
                .remainingLiveGates(
                    queuePassed
                )

        val manualStillRequired=
            "regular-player queue/join/leave + pregame GUI/HUD/vote/countdown real-server certification" in
                remaining &&
            "player departure/reconnect and teammate tower takeover certification" in
                remaining

        return listOf(
            FixtureResult(
                "live-certification-core-pass-does-not-imply-full-certification",
                corePassed.coreCertified &&
                    corePassed.certified &&
                    !diagnostics
                        .fullRealServerCertified(
                            corePassed
                        ) &&
                    remaining.isNotEmpty()
            ),
            FixtureResult(
                "live-certification-dynamically-removes-recorded-core-gates-only",
                coreLinkedRemoved &&
                    manualStillRequired
            ),
            FixtureResult(
                "live-certification-requires-two-consecutive-clean-rounds",
                !oneCleanOnly.coreCertified &&
                    "consecutive-round world residue/reuse real-server certification" in
                        oneCleanRemaining
            ),
            FixtureResult(
                "live-certification-requires-verified-farm-reset-evidence",
                !resetNotPassed.coreCertified &&
                    "verified Farm reset/repair real-server certification" in
                        resetNotPassedRemaining
            ),
            FixtureResult(
                "live-certification-removes-queue-gate-only-after-full-observed-flow",
                !corePassed.queueFlowPassed &&
                    queuePassed.queueFlowPassed &&
                    "regular-player queue/join/leave + pregame GUI/HUD/vote/countdown real-server certification" !in
                        queuePassedRemaining
            )
        )
    }
}
