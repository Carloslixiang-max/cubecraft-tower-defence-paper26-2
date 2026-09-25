package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.ArenaTeardownReport
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

data class PaperStage4GateStatus(
    val domainFixturesPassed: Boolean,
    val farmMapCheckPassed: Boolean,
    val adapterSmokePassed: Boolean,
    val cleanRestartCycles: Int,
    val cleanArenaRoundTrips: Int,
    val previousBootWasUnclean: Boolean
) {
    val certified: Boolean
        get() =
            domainFixturesPassed &&
            farmMapCheckPassed &&
            adapterSmokePassed &&
            cleanRestartCycles >= 2 &&
            cleanArenaRoundTrips >= 1 &&
            !previousBootWasUnclean

    fun summary(): String =
        "certified=$certified " +
            "fixtures=$domainFixturesPassed " +
            "farm=$farmMapCheckPassed " +
            "smoke=$adapterSmokePassed " +
            "cleanRestarts=$cleanRestartCycles " +
            "arenaRoundTrips=$cleanArenaRoundTrips " +
            "previousUnclean=$previousBootWasUnclean"
}

/**
 * Durable live-gate evidence.
 *
 * There is intentionally no public "set certified" API and no admin command
 * that can skip individual gates. Evidence is only updated by observed events.
 */
class PaperStage4GateStore(
    private val plugin: JavaPlugin
) {
    private val path: Path =
        plugin.dataFolder.toPath()
            .resolve("stage4-live-gate.properties")

    private val props=Properties()

    private var previousUnclean=false

    init {
        if(Files.isRegularFile(path)) {
            Files.newInputStream(path).use(
                props::load
            )
        }

        val previousRunning=
            props.getProperty(
                "running","false"
            ).toBoolean()
        val previousClean=
            props.getProperty(
                "cleanShutdown","false"
            ).toBoolean()

        previousUnclean=
            previousRunning &&
                !previousClean

        if(previousUnclean) {
            props.setProperty(
                "cleanRestartCycles","0"
            )
        } else if(previousClean) {
            val current=
                int("cleanRestartCycles")
            props.setProperty(
                "cleanRestartCycles",
                (current+1).toString()
            )
        }

        props.setProperty(
            "running","true"
        )
        props.setProperty(
            "cleanShutdown","false"
        )
        props.setProperty(
            "pluginVersion",
            plugin.description.version
        )
        props.setProperty(
            "serverVersion",
            plugin.server.version
        )
        save()
    }

    fun recordDomainFixtures(
        passed: Boolean
    ) {
        if(passed) {
            props.setProperty(
                "domainFixturesPassed",
                "true"
            )
            save()
        }
    }

    fun recordFarmMapCheck(
        passed: Boolean
    ) {
        if(passed) {
            props.setProperty(
                "farmMapCheckPassed",
                "true"
            )
            save()
        }
    }

    fun runAndRecordAdapterSmoke():
        List<String> {
        val failures=
            mutableListOf<String>()

        fun checkStep(
            id: String,
            block: ()->Unit
        ) {
            runCatching(block)
                .onFailure {
                    failures +=
                        "$id: ${it.javaClass.simpleName}: ${it.message}"
                }
        }

        checkStep("blockdata") {
            Bukkit.createBlockData(
                "minecraft:stone"
            )
        }

        checkStep("inventory-component-title") {
            plugin.server.createInventory(
                null,
                9,
                Component.text(
                    "CubeCraft TD smoke"
                )
            )
        }

        checkStep("itemstack-bytes") {
            val bytes=
                ItemStack(
                    Material.STONE,1
                ).serializeAsBytes()
            check(bytes.isNotEmpty())
            ItemStack.deserializeBytes(bytes)
        }

        checkStep("scheduler-primary-thread") {
            check(
                Bukkit.isPrimaryThread()
            ) {
                "onEnable/smoke must run on primary thread"
            }
        }

        if(failures.isEmpty()) {
            props.setProperty(
                "adapterSmokePassed",
                "true"
            )
            save()
        }

        return failures
    }

    fun recordArenaRoundTrip(
        report: ArenaTeardownReport
    ) {
        if(report.fullyCleanNow) {
            props.setProperty(
                "cleanArenaRoundTrips",
                (
                    int(
                        "cleanArenaRoundTrips"
                    )+1
                ).toString()
            )
            save()
        }
    }

    fun markCleanShutdown(
        clean: Boolean
    ) {
        props.setProperty(
            "running","false"
        )
        props.setProperty(
            "cleanShutdown",
            clean.toString()
        )
        if(!clean) {
            props.setProperty(
                "cleanRestartCycles","0"
            )
        }
        save()
    }

    fun status():
        PaperStage4GateStatus =
        PaperStage4GateStatus(
            domainFixturesPassed=
                bool(
                    "domainFixturesPassed"
                ),
            farmMapCheckPassed=
                bool(
                    "farmMapCheckPassed"
                ),
            adapterSmokePassed=
                bool(
                    "adapterSmokePassed"
                ),
            cleanRestartCycles=
                int(
                    "cleanRestartCycles"
                ),
            cleanArenaRoundTrips=
                int(
                    "cleanArenaRoundTrips"
                ),
            previousBootWasUnclean=
                previousUnclean
        )

    private fun bool(
        key: String
    ): Boolean =
        props.getProperty(
            key,"false"
        ).toBoolean()

    private fun int(
        key: String
    ): Int =
        props.getProperty(
            key,"0"
        ).toIntOrNull() ?: 0

    private fun save() {
        Files.createDirectories(
            path.parent
        )
        val temp=
            path.resolveSibling(
                "${path.fileName}.tmp"
            )
        Files.newOutputStream(temp).use {
            props.store(
                it,
                "CubeCraft TD Stage-4 live evidence"
            )
        }
        try {
            Files.move(
                temp,path,
                java.nio.file.StandardCopyOption
                    .REPLACE_EXISTING,
                java.nio.file.StandardCopyOption
                    .ATOMIC_MOVE
            )
        } catch (_: Exception) {
            Files.move(
                temp,path,
                java.nio.file.StandardCopyOption
                    .REPLACE_EXISTING
            )
        }
    }
}
