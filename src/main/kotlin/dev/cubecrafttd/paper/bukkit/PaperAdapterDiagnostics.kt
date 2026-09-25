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
                "engineering playtest hotbar projection"
            ),
            stillLiveGateRequired = linkedSetOf(
                "player snapshot lossless roundtrip on real server",
                "entity movement visual smoothness",
                "raytrace fidelity vs original firing origins",
                "GUI item renderer/final icons",
                "restart recovery",
                "same-world multi-arena isolation",
                "60+ tower live profiler"
            )
        )
    }
}
