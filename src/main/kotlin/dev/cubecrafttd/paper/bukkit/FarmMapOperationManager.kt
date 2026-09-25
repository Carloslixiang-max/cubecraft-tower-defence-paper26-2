package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.ArenaTaskHandle
import java.io.File

data class FarmMapPlanSummary(
    val worldName: String,
    val worldUid: java.util.UUID,
    val origin:
        dev.cubecrafttd.map.BlockPos,
    val sha256: String,
    val dimensions: String,
    val blockEntityCount: Int,
    val schematicEntityCount: Int,
    val routeIds: List<String>,
    val guardAnchorCounts:
        Map<dev.cubecrafttd.arena.TeamId,Int>
)

class FarmMapOperationManager(
    private val plugin:
        org.bukkit.plugin.java.JavaPlugin,
    private val binding:
        PaperMapBindingConfig
) {
    private var activePaste:
        ArenaTaskHandle? = null

    fun plan(): FarmMapPlanSummary {
        val world=binding.resolveWorld(
            plugin.server
        ) ?: error(
            "Configured map world is missing/not loaded"
        )
        val origin=binding.origin
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
            java.security.MessageDigest
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

        val plan=FarmPaperMapBinder()
            .prepare(
                bytes,
                world.uid,
                origin
            )
        val d=plan.volume.dimensions
        val bound=
            runCatching {
                PaperMapRuntimeGeometryBinder
                    .bind(
                        plan.runtime,binding
                    )
            }.getOrNull()

        return FarmMapPlanSummary(
            worldName=world.name,
            worldUid=world.uid,
            origin=origin,
            sha256=sha,
            dimensions=
                "${d.width}x${d.height}x${d.length}",
            blockEntityCount=
                plan.volume.blockEntities.size,
            schematicEntityCount=
                plan.volume.schematicEntityCount,
            routeIds=
                plan.runtime.routesById
                    .keys.sorted(),
            guardAnchorCounts=
                dev.cubecrafttd.arena.TeamId
                    .entries.associateWith {
                        binding.guardAnchors
                            .getValue(it).size
                    }
        )
    }

    fun startSafePaste(
        onProgress:
            (SchematicPasteProgress)->Unit,
        onComplete:()->Unit,
        onFailure:(Throwable)->Unit
    ) {
        check(binding.allowFarmPaste) {
            "Farm paste is disabled in config"
        }
        check(
            activePaste?.isCancelled != false
        ) {
            "A Farm paste is already active"
        }

        val world=binding.resolveWorld(
            plugin.server
        ) ?: error(
            "Configured map world is missing/not loaded"
        )
        val origin=binding.origin
            ?: error(
                "map-binding.origin is incomplete"
            )
        val file=File(
            plugin.dataFolder,
            "maps/ImprovedFarm.schem"
        )
        val bytes=file.readBytes()
        val sha=
            java.security.MessageDigest
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
            "Farm SHA-256 mismatch"
        }
        val plan=FarmPaperMapBinder()
            .prepare(
                bytes,world.uid,origin
            )

        activePaste=
            BukkitSchematicPasteService(
                plugin,world
            ).pasteIntoEmptyRegion(
                plan.volume,
                origin,
                binding.pasteBlocksPerTick,
                onProgress,
                {
                    activePaste=null
                    onComplete()
                },
                {
                    activePaste=null
                    onFailure(it)
                }
            )
    }

    fun cancelActivePaste() {
        activePaste?.cancel()
        activePaste=null
    }

    fun hasActivePaste(): Boolean =
        activePaste?.isCancelled == false
}
