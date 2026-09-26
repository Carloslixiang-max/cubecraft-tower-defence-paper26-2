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
                cleanArenaRoundTrips=1,
                previousBootWasUnclean=false
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
                remaining

        val manualStillRequired=
            "regular-player queue/join/leave + pregame GUI/HUD/vote/countdown real-server certification" in
                remaining &&
            "player departure/reconnect and teammate tower takeover certification" in
                remaining &&
            "verified Farm reset/repair real-server certification" in
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
            )
        )
    }
}
