package dev.cubecrafttd.testing

import dev.cubecrafttd.paper.LiveMenuView
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

        val departurePassed=
            corePassed.copy(
                departureReconnectObserved=true,
                departedOwnerTeammateTakeoverObserved=true
            )
        val departurePassedRemaining=
            diagnostics
                .remainingLiveGates(
                    departurePassed
                )

        val uiPassed=
            corePassed.copy(
                uiTowerBuilderObserved=true,
                uiPathSelectorObserved=true,
                uiTowerMenuObserved=true,
                uiSettingsObserved=true,
                uiInventoryLayoutObserved=true
            )
        val uiPassedRemaining=
            diagnostics
                .remainingLiveGates(
                    uiPassed
                )

        val manualStillRequired=
            "regular-player queue/join/leave + pregame GUI/HUD/vote/countdown real-server certification" in
                remaining &&
            "player departure/reconnect and teammate tower takeover certification" in
                remaining

        val classifiedUiEvidence=
            listOf(
                PaperUiLiveEvidenceClassifier
                    .classify(
                        LiveMenuView(
                            "Tower builder",
                            45,
                            emptyMap()
                        )
                    ),
                PaperUiLiveEvidenceClassifier
                    .classify(
                        LiveMenuView(
                            "Select an upgrade path",
                            27,
                            emptyMap()
                        )
                    ),
                PaperUiLiveEvidenceClassifier
                    .classify(
                        LiveMenuView(
                            "Zeus Tower I",
                            54,
                            mapOf(
                                45 to
                                    "tower-manage:1:rangefinder",
                                49 to
                                    "noop:tower-manage:1:2021-book-unresolved",
                                53 to
                                    "noop:tower-manage:1:2021-barrier-unresolved"
                            )
                        )
                    ),
                PaperUiLiveEvidenceClassifier
                    .classify(
                        LiveMenuView(
                            "Settings",
                            45,
                            emptyMap()
                        )
                    ),
                PaperUiLiveEvidenceClassifier
                    .classify(
                        LiveMenuView(
                            "Change inventory layout",
                            36,
                            emptyMap()
                        )
                    )
            )

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
            ),
            FixtureResult(
                "live-certification-removes-departure-gate-only-after-reconnect-and-takeover",
                !corePassed.departureFlowPassed &&
                    departurePassed.departureFlowPassed &&
                    "player departure/reconnect and teammate tower takeover certification" !in
                        departurePassedRemaining
            ),
            FixtureResult(
                "live-certification-critical-ui-observation-does-not-auto-certify-visual-fidelity",
                !corePassed
                    .criticalUiSurfaceFlowPassed &&
                    uiPassed
                        .criticalUiSurfaceFlowPassed &&
                    "critical GUI surface real-server observation" !in
                        uiPassedRemaining &&
                    "production GUI icon/lore fidelity" in
                        uiPassedRemaining
            ),
            FixtureResult(
                "live-certification-classifies-five-critical-ui-surfaces",
                classifiedUiEvidence==
                    listOf(
                        PaperUiLiveEvidence
                            .TOWER_BUILDER,
                        PaperUiLiveEvidence
                            .PATH_SELECTOR,
                        PaperUiLiveEvidence
                            .TOWER_MENU,
                        PaperUiLiveEvidence
                            .SETTINGS,
                        PaperUiLiveEvidence
                            .INVENTORY_LAYOUT
                    )
            )
        )
    }
}
