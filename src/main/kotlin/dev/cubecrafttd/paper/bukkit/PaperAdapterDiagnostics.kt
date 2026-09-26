package dev.cubecrafttd.paper.bukkit

data class PaperAdapterDiagnostics(
    val sourceReady: Set<String>,
    val stillLiveGateRequired: Set<String>
) {
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
                "same-world arena spatial/player reservation guard",
                "cross-restart recovery verification path",
                "route-facing live entity orientation",
                "tracked-mob vanilla side-effect shielding",
                "engineering Armageddon player vote bridge"
            ),
            stillLiveGateRequired = linkedSetOf(
                "player snapshot lossless roundtrip on real server",
                "entity movement visual smoothness",
                "raytrace fidelity vs original firing origins",
                "production GUI icon/lore fidelity",
                "restart recovery",
                "same-world non-overlap multi-arena real-server certification",
                "60+ tower real-server performance certification"
            )
        )
    }
}
