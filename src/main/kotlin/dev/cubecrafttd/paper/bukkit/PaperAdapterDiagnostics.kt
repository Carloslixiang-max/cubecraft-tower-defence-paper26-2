package dev.cubecrafttd.paper.bukkit

data class PaperAdapterDiagnostics(
    val sourceReady: Set<String>,
    val stillLiveGateRequired: Set<String>
) {
    fun remainingLiveGates(
        stage4: PaperStage4GateStatus
    ): Set<String> =
        stillLiveGateRequired
            .filterTo(
                linkedSetOf()
            ) { gate ->
                when(gate) {
                    "player snapshot lossless roundtrip on real server" ->
                        !stage4.playerSnapshotRoundTripPassed
                    "restart recovery" ->
                        !stage4.restartRecoveryPassed
                    "60+ tower real-server performance certification" ->
                        !stage4.towerStress60Passed
                    "consecutive-round world residue/reuse real-server certification" ->
                        stage4.consecutiveCleanArenaRoundTrips < 2
                    else -> true
                }
            }

    fun fullRealServerCertified(
        stage4: PaperStage4GateStatus
    ): Boolean =
        stage4.coreCertified &&
            remainingLiveGates(stage4)
                .isEmpty()

    companion object {
        fun current() = PaperAdapterDiagnostics(
            sourceReady = linkedSetOf(
                "scheduler",
                "entity lookup/remove/move",
                "block data world adapter",
                "block raytrace LOS",
                "tower geometry",
                "guard geometry",
                "mob spawn",
                "tracked-mob player damage bridge",
                "inventory bridge",
                "action bar",
                "player snapshot codec",
                "runtime fallback YAML loader",
                "full gameplay fallback completeness",
                "mob support/lethal resolved composition",
                "summon-count/chain-policy fallback binding",
                "target-priority/per-tower-LOS fallback binding",
                "Wither boss/trail/skull live runtime",
                "Java25/Paper26.2 compile + shaded JAR CI",
                "Paper26.2 double live-boot smoke CI",
                "Castle Guard live geometry anchors",
                "engineering playtest hotbar projection",
                "live engineering rangefinder projection",
                "live tower-attack feedback projection",
                "readable refreshed live menu projection",
                "status-aware mob movement",
                "world-targeted AoE potion execution",
                "durable player hotbar customization",
                "live player snapshot roundtrip gate command",
                "live per-arena tick/phase profiler",
                "Engineering 60+ tower real-server performance gate harness",
                "same-world arena spatial/player reservation guard",
                "cross-restart recovery verification path",
                "repeat-match snapshot recapture and prepare-failure rollback hardening",
                "fail-closed corrupt recovery-journal readiness gate",
                "durable journal delete-before-memory-restore commit ordering",
                "bounded automatic retry for online pending player recovery",
                "truthful Stage-4 core vs full real-server certification reporting",
                "two-consecutive-clean-round reuse certification counter",
                "unclean-restart Farm hard gate with persistent tagged-entity cleanup",
                "queue start commit boundary prevents post-start ghost requeue",
                "start-failure teardown residue feeds persistent Farm reuse interlock",
                "route-facing live entity orientation",
                "tracked-mob vanilla side-effect shielding",
                "engineering Armageddon player vote bridge",
                "historical-style player departure lifecycle",
                "departed-owner teammate tower management",
                "engineering match-end UI cleanup and result presentation",
                "regular-player Engineering 1v1 queue/join/leave bridge",
                "historical direct-log 3-second pre-game countdown",
                "historical pregame Armageddon vote/no-vote Random resolution",
                "historical pregame Pricing vote with live economy binding",
                "safe pregame vote GUI projection and Bukkit bridge",
                "non-invasive pregame queue/countdown action-bar HUD",
                "pregame GUI filtering for unrunnable Armageddon modes",
                "post-teardown tracked-entity residue verification",
                "persistent Farm reuse interlock across restarts",
                "verified Farm reset scan/apply/verify with rollback and crash lock"
            ),
            stillLiveGateRequired = linkedSetOf(
                "player snapshot lossless roundtrip on real server",
                "entity movement visual smoothness",
                "raytrace fidelity vs original firing origins",
                "production GUI icon/lore fidelity",
                "match-end title/menu cleanup real-server certification",
                "consecutive-round world residue/reuse real-server certification",
                "verified Farm reset/repair real-server certification",
                "regular-player queue/join/leave + pregame GUI/HUD/vote/countdown real-server certification",
                "player departure/reconnect and teammate tower takeover certification",
                "restart recovery",
                "same-world non-overlap multi-arena real-server certification",
                "60+ tower real-server performance certification"
            )
        )
    }
}
