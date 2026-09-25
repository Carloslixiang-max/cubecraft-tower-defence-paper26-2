package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.match.NormalGameplayConfigResolver
import dev.cubecrafttd.match.ResolvedNormalGameplayConfig
import dev.cubecrafttd.map.MapRuntimeDefinition
import dev.cubecrafttd.mob.TeamConfiguredRouteAssignmentPolicy
import dev.cubecrafttd.truth.RuntimeFallbackConfigV1
import java.io.File
import java.security.MessageDigest

data class BukkitLiveCompositionPreflightReport(
    val mapRuntime: MapRuntimeDefinition,
    val routePolicy: TeamConfiguredRouteAssignmentPolicy,
    val gameplay: ResolvedNormalGameplayConfig,
    val mapSha256: String,
    val worldName: String
)

class BukkitLiveCompositionPreflight(
    private val plugin:
        org.bukkit.plugin.java.JavaPlugin,
    private val mapBinding:
        PaperMapBindingConfig,
    private val fallback:
        RuntimeFallbackConfigV1
) {
    fun run():
        BukkitLiveCompositionPreflightReport {
        val world=
            mapBinding.resolveWorld(
                plugin.server
            ) ?: error(
                "Configured map world is not loaded"
            )
        val origin=
            mapBinding.origin
                ?: error(
                    "map-binding.origin is incomplete"
                )

        val file=File(
            plugin.dataFolder,
            "maps/ImprovedFarm.schem"
        )
        check(file.isFile) {
            "Missing ${file.path}"
        }
        val bytes=file.readBytes()
        val sha=
            MessageDigest
                .getInstance("SHA-256")
                .digest(bytes)
                .joinToString("") {
                    "%02x".format(it)
                }

        check(
            sha ==
                PaperGameplayReadinessService
                    .EXPECTED_FARM_SHA256
        ) {
            "Farm SHA-256 mismatch: $sha"
        }

        val plan=
            FarmPaperMapBinder()
                .prepare(
                    bytes,
                    world.uid,
                    origin
                )
        val bound=
            PaperMapRuntimeGeometryBinder
                .bind(
                    plan.runtime,
                    mapBinding
                )
        val gameplay=
            NormalGameplayConfigResolver
                .resolve(fallback)

        return BukkitLiveCompositionPreflightReport(
            mapRuntime=bound.runtime,
            routePolicy=bound.routePolicy,
            gameplay=gameplay,
            mapSha256=sha,
            worldName=world.name
        )
    }
}
