package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.match.*
import dev.cubecrafttd.tower.lifecycle.*
import dev.cubecrafttd.truth.*
import dev.cubecrafttd.troop.MobEntitySpawnPort
import dev.cubecrafttd.mob.MobPositionUpdatePort
import org.bukkit.plugin.Plugin

/**
 * Per-arena type-specific Armageddon runtime.
 *
 * Lightning, Horde and Wither are executable in Stage-4 once their explicit
 * type-specific engineering fallbacks are present.
 *
 * Wither visuals/ground decay remain a later visual-fidelity layer; its
 * gameplay runtime is executable here.
 */
class BukkitArenaArmageddonRuntime(
    private val plugin: Plugin,
    private val fallback:
        RuntimeFallbackConfigV1,
    private val bodyMutation:
        TowerBodyMutationService,
    private val routeAssignment:
        dev.cubecrafttd.mob
            .RouteAssignmentPolicy,
    private val mobSpawn:
        MobEntitySpawnPort,
    private val mobPosition:
        MobPositionUpdatePort
) : ArmageddonStartPort {
    private var activeType:
        ArmageddonType?=null
    private var lightning:
        LightningArmageddonTickRuntime?=
        null
    private var horde:
        HordeArmageddonTickRuntime?=
        null
    private var wither:
        WitherArmageddonTickRuntime?=
        null

    override fun start(
        type: ArmageddonType,
        context: ArenaContext,
        gameTick: Long
    ) {
        check(activeType==null) {
            "Armageddon already started"
        }

        when(type) {
            ArmageddonType.LIGHTNING -> {
                val externalDestroy=
                    TowerExternalDestructionService(
                        context,
                        bodyMutation
                    )
                lightning=
                    LightningArmageddonTickRuntime(
                        config=
                            ArmageddonFallbackBindings
                                .lightning(
                                    fallback
                                ),
                        activationTick=
                            gameTick,
                        firstStrikeDelayTicks=
                            ArmageddonFallbackBindings
                                .lightningFirstDelay(
                                    fallback
                                ),
                        destroyPort=
                            ArmageddonTowerDestroyPort {
                                id ->
                                runCatching {
                                    externalDestroy
                                        .destroy(id)
                                    true
                                }.onFailure {
                                    plugin.logger.warning(
                                        "Lightning failed to destroy tower ${id.value}: " +
                                            "${it.javaClass.simpleName}: ${it.message}"
                                    )
                                }.getOrDefault(
                                    false
                                )
                            }
                    )
            }

            ArmageddonType.HORDE -> {
                horde=
                    HordeArmageddonTickRuntime(
                        config=
                            ArmageddonFallbackBindings
                                .horde(
                                    fallback
                                ),
                        activationTick=
                            gameTick,
                        firstWaveDelayTicks=
                            ArmageddonFallbackBindings
                                .hordeFirstDelay(
                                    fallback
                                ),
                        routeAssignment=
                            routeAssignment,
                        entitySpawn=
                            mobSpawn
                    )
            }

            ArmageddonType.WITHER -> {
                val externalDestroy=
                    TowerExternalDestructionService(
                        context,
                        bodyMutation
                    )
                wither=
                    WitherArmageddonTickRuntime(
                        config=
                            ArmageddonFallbackBindings
                                .wither(
                                    fallback
                                ),
                        activationTick=
                            gameTick,
                        routeAssignment=
                            routeAssignment,
                        entitySpawn=
                            mobSpawn,
                        livePosition=
                            mobPosition,
                        destroyPort=
                            ArmageddonTowerDestroyPort {
                                id ->
                                runCatching {
                                    externalDestroy
                                        .destroy(id)
                                    true
                                }.onFailure {
                                    plugin.logger.warning(
                                        "Wither failed to destroy tower ${id.value}: " +
                                            "${it.javaClass.simpleName}: ${it.message}"
                                    )
                                }.getOrDefault(false)
                            }
                    ).also {
                        it.start(context)
                    }
            }
        }

        activeType=type
        plugin.logger.info(
            "Arena ${context.arenaId.value} type-specific Armageddon started: $type"
        )
    }

    fun tick(
        context: ArenaContext
    ) {
        when(activeType) {
            ArmageddonType.LIGHTNING ->
                lightning?.tick(context)
            ArmageddonType.HORDE ->
                horde?.tick(context)
            ArmageddonType.WITHER ->
                wither?.tick(context)
            null -> Unit
        }
    }

    fun type():ArmageddonType? =
        activeType
}
