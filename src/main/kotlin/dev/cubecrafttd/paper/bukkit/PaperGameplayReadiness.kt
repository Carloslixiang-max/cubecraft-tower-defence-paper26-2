package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.truth.*
import java.io.File

enum class ReadinessSeverity {
    BLOCKING,
    WARNING,
    INFO
}

data class ReadinessIssue(
    val code: String,
    val severity: ReadinessSeverity,
    val detail: String
)

data class PaperGameplayReadinessReport(
    val issues: List<ReadinessIssue>
) {
    val blockers: List<ReadinessIssue>
        get() = issues.filter {
            it.severity ==
                ReadinessSeverity.BLOCKING
        }

    val readyForLiveArena: Boolean
        get() = blockers.isEmpty()

    fun summary(): String =
        "ready=$readyForLiveArena " +
            "blocking=${blockers.size} " +
            "warnings=${issues.count { it.severity==ReadinessSeverity.WARNING }}"
}

class PaperGameplayReadinessService(
    private val plugin:
        org.bukkit.plugin.java.JavaPlugin,
    private val fallbackConfig:
        RuntimeFallbackConfigV1,
    private val mapBinding:
        PaperMapBindingConfig,
    private val pendingRecoveryCount:
        () -> Int,
    private val recoveryJournalFailureCount:
        () -> Int,
    private val liveGate:
        PaperStage4GateStore
) {
    companion object {
        const val EXPECTED_FARM_SHA256 =
            "28d24136afe80358b89556d0fbe3b08d00e518af38c7c518819fa7fc225e613e"
    }

    fun inspect():
        PaperGameplayReadinessReport {
        val issues=
            mutableListOf<ReadinessIssue>()

        GameplayFallbackCompletenessValidator
            .validate(
                fallbackConfig
            )
            .forEach { missing ->
                issues += ReadinessIssue(
                    code="FALLBACK_${missing.key}",
                    severity=
                        if(
                            missing.level ==
                                FallbackRequirementLevel
                                    .BLOCKS_MECHANIC
                        )
                            ReadinessSeverity.BLOCKING
                        else
                            ReadinessSeverity.WARNING,
                    detail=missing.reason
                )
            }

        val farm=File(
            plugin.dataFolder,
            "maps/ImprovedFarm.schem"
        )
        if(!farm.isFile) {
            issues += ReadinessIssue(
                "FARM_MAP_MISSING",
                ReadinessSeverity.BLOCKING,
                "Missing ${farm.path}"
            )
        } else {
            val bytes=runCatching {
                farm.readBytes()
            }.getOrElse {
                issues += ReadinessIssue(
                    "FARM_MAP_READ_ERROR",
                    ReadinessSeverity.BLOCKING,
                    "${it.javaClass.simpleName}: ${it.message}"
                )
                null
            }
            if(bytes!=null) {
                val sha=sha256(bytes)
                if(sha!=EXPECTED_FARM_SHA256) {
                    issues += ReadinessIssue(
                        "FARM_MAP_HASH_MISMATCH",
                        ReadinessSeverity.BLOCKING,
                        "expected=$EXPECTED_FARM_SHA256 actual=$sha"
                    )
                } else {
                    runCatching {
                        val world =
                            mapBinding.resolveWorld(
                                plugin.server
                            )
                        val origin =
                            mapBinding.origin
                        if(
                            world!=null &&
                            origin!=null
                        ) {
                            val plan=
                                FarmPaperMapBinder()
                                    .prepare(
                                        bytes,
                                        world.uid,
                                        origin
                                    )
                            PaperMapRuntimeGeometryBinder
                                .bind(
                                    plan.runtime,
                                    mapBinding
                                )
                        }
                    }.onFailure {
                        issues += ReadinessIssue(
                            "FARM_MAP_PLAN_ERROR",
                            ReadinessSeverity.BLOCKING,
                            "${it.javaClass.simpleName}: ${it.message}"
                        )
                    }
                }
            }
        }

        if(
            mapBinding.worldName==null
        ) {
            issues += ReadinessIssue(
                "MAP_WORLD_UNSET",
                ReadinessSeverity.BLOCKING,
                "map-binding.world is not configured"
            )
        } else if(
            mapBinding.resolveWorld(
                plugin.server
            )==null
        ) {
            issues += ReadinessIssue(
                "MAP_WORLD_NOT_LOADED",
                ReadinessSeverity.BLOCKING,
                "World '${mapBinding.worldName}' is not loaded"
            )
        }

        if(mapBinding.origin==null) {
            issues += ReadinessIssue(
                "MAP_ORIGIN_UNSET",
                ReadinessSeverity.BLOCKING,
                "map-binding.origin x/y/z must all be configured"
            )
        }

        dev.cubecrafttd.arena.TeamId.entries
            .forEach { team ->
                if(
                    mapBinding.routeIdByAttackedTeam[
                        team
                    ]==null
                ) {
                    issues += ReadinessIssue(
                        "ROUTE_${team.name}_UNSET",
                        ReadinessSeverity.BLOCKING,
                        "Explicit Farm branch route is required for $team while the original branch-selection rule remains unresolved"
                    )
                }

                val count=
                    mapBinding.guardAnchors
                        .getValue(team).size
                if(count!=2) {
                    issues += ReadinessIssue(
                        "GUARD_ANCHORS_${team.name}",
                        ReadinessSeverity.BLOCKING,
                        "$team requires exactly 2 configured Guard anchors; found $count"
                    )
                }

                if(
                    mapBinding.playerSpawns[
                        team
                    ]==null
                ) {
                    issues += ReadinessIssue(
                        "PLAYER_SPAWN_${team.name}",
                        ReadinessSeverity.BLOCKING,
                        "Explicit player spawn is required for $team; the verified Farm schematic does not provide original player-spawn truth"
                    )
                }
            }

        val journalFailures=
            recoveryJournalFailureCount()
        if(journalFailures>0) {
            issues += ReadinessIssue(
                "RECOVERY_JOURNAL_CORRUPT",
                ReadinessSeverity.BLOCKING,
                "$journalFailures recovery snapshot file(s) could not be decoded at startup. Original files were preserved; repair/restore them and restart before starting another TD match."
            )
        }

        val pending=
            pendingRecoveryCount()
        if(pending>0) {
            issues += ReadinessIssue(
                "PENDING_PLAYER_RECOVERY",
                ReadinessSeverity.WARNING,
                "$pending player snapshot(s) still need reconnect/restore"
            )
        }

        if(!mapBinding.allowFarmPaste) {
            issues += ReadinessIssue(
                "FARM_PASTE_DISABLED",
                ReadinessSeverity.INFO,
                "Safe default: map paste is disabled until explicitly enabled"
            )
        }

        val stage4=
            liveGate.status()
        if(!stage4.certified) {
            issues += ReadinessIssue(
                "PAPER_LIVE_GATE_NOT_CERTIFIED",
                ReadinessSeverity.BLOCKING,
                "Stage-4 evidence incomplete: ${stage4.summary()}"
            )
        }

        return PaperGameplayReadinessReport(
            issues
        )
    }

    private fun sha256(
        bytes: ByteArray
    ): String {
        val digest=
            java.security.MessageDigest
                .getInstance("SHA-256")
                .digest(bytes)
        return digest.joinToString("") {
            "%02x".format(it)
        }
    }
}
