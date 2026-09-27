package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.ArenaContext
import dev.cubecrafttd.match.MatchSessionState
import dev.cubecrafttd.mob.MobRuntimeState
import dev.cubecrafttd.ui.MobCombatVisualProjection
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.entity.Display
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.TextDisplay
import org.bukkit.plugin.Plugin
import java.util.UUID

/**
 * Paper projection for the recovered Digital mob health / Damage indicators
 * settings.
 *
 * Visibility is viewer-scoped. Exact CubeCraft styling/timing is still
 * UNKNOWN, so the text layout and lifetime below remain Engineering fallback.
 */
class BukkitMobCombatVisualProjectionService(
    private val plugin: Plugin
) {
    private data class ObservedMob(
        val state: MobRuntimeState,
        var lastHealth: Double,
        var lastDamageAnchor: Location?
    )

    private data class ViewerScopedDisplay(
        val displayUuid: UUID,
        val viewers:
            MutableSet<UUID> =
            linkedSetOf()
    )

    private data class DamageDisplay(
        val displayUuid: UUID,
        val viewers:
            MutableSet<UUID> =
            linkedSetOf(),
        var accumulatedDamage: Double,
        var expiresAtTick: Long
    )

    private val server=
        plugin.server

    private val observedMobs=
        linkedMapOf<UUID,ObservedMob>()

    private val healthDisplays=
        linkedMapOf<
            UUID,
            ViewerScopedDisplay
        >()

    /**
     * Keyed by target mob, not display UUID. This deliberately bounds the
     * runtime to one active damage display per mob even under rapid tower fire.
     */
    private val damageDisplays=
        linkedMapOf<
            UUID,
            DamageDisplay
        >()

    fun beginTick(
        context: ArenaContext
    ) {
        context.entityIndex
            .mobsByUuid
            .forEach { (uuid,mob) ->
                val anchor=
                    damageAnchor(uuid)
                val existing=
                    observedMobs[uuid]
                if(existing==null) {
                    observedMobs[uuid]=
                        ObservedMob(
                            mob,
                            mob.combat.health,
                            anchor
                        )
                } else if(anchor!=null) {
                    existing.lastDamageAnchor=
                        anchor
                }
            }
    }

    fun project(
        context: ArenaContext,
        session: MatchSessionState
    ) {
        val healthViewers=
            session.players
                .values
                .filter {
                    MobCombatVisualProjection
                        .digitalHealthEnabled(
                            it.interaction
                                .settings
                        )
                }
                .mapTo(linkedSetOf()) {
                    it.playerUuid
                }

        val damageViewers=
            session.players
                .values
                .filter {
                    MobCombatVisualProjection
                        .damageIndicatorsEnabled(
                            it.interaction
                                .settings
                        )
                }
                .mapTo(linkedSetOf()) {
                    it.playerUuid
                }

        val currentMobIds=
            context.entityIndex
                .mobsByUuid.keys
                .toSet()

        observedMobs
            .values
            .toList()
            .forEach { observed ->
                val uuid=
                    observed.state
                        .identity
                        .entityUuid
                val currentHealth=
                    observed.state
                        .combat
                        .health
                val damage=
                    MobCombatVisualProjection
                        .damageAmount(
                            observed.lastHealth,
                            currentHealth
                        )

                if(
                    damage > 1.0e-9 &&
                    damageViewers.isNotEmpty()
                ) {
                    val anchor=
                        damageAnchor(uuid)
                            ?: observed
                                .lastDamageAnchor
                    if(anchor!=null) {
                        upsertDamageDisplay(
                            context,
                            uuid,
                            damage,
                            anchor,
                            damageViewers
                        )
                    }
                }

                observed.lastHealth=
                    currentHealth
                damageAnchor(uuid)
                    ?.let {
                        observed
                            .lastDamageAnchor=
                            it
                    }

                if(uuid !in currentMobIds) {
                    observedMobs
                        .remove(uuid)
                }
            }

        // Mobs spawned during this same core tick become the baseline for the
        // next projection. They do not inherit synthetic damage from 0 HP.
        context.entityIndex
            .mobsByUuid
            .forEach { (uuid,mob) ->
                observedMobs
                    .putIfAbsent(
                        uuid,
                        ObservedMob(
                            mob,
                            mob.combat.health,
                            damageAnchor(uuid)
                        )
                    )
            }

        if(
            context.gameTick % 2L ==
            0L
        ) {
            syncHealthDisplays(
                context,
                healthViewers
            )
        }

        syncDamageDisplays(
            context,
            damageViewers
        )
    }

    private fun syncHealthDisplays(
        context: ArenaContext,
        desiredViewers: Set<UUID>
    ) {
        val currentMobIds=
            context.entityIndex
                .mobsByUuid.keys
                .toSet()

        healthDisplays
            .keys
            .filter {
                it !in currentMobIds ||
                    desiredViewers
                        .isEmpty()
            }
            .toList()
            .forEach { mobUuid ->
                removeHealthDisplay(
                    context,
                    mobUuid
                )
            }

        if(desiredViewers.isEmpty()) {
            return
        }

        context.entityIndex
            .mobsByUuid
            .forEach { (uuid,mob) ->
                val anchor=
                    healthAnchor(uuid)
                        ?: return@forEach
                val text=
                    MobCombatVisualProjection
                        .healthText(
                            mob.combat.health,
                            mob.combat.maxHealth
                        )

                var state=
                    healthDisplays[uuid]
                var display=
                    state?.displayUuid
                        ?.let(server::getEntity)
                        as? TextDisplay

                if(display==null) {
                    state?.let {
                        context.entityIndex
                            .transientDisplays
                            .remove(
                                it.displayUuid
                            )
                    }
                    display=
                        spawnTextDisplay(
                            context,
                            anchor,
                            text,
                            teleportDuration=2
                        )
                    state=
                        ViewerScopedDisplay(
                            display.uniqueId
                        )
                    healthDisplays[uuid]=
                        state
                }

                display.text(
                    Component.text(text)
                )
                display.teleport(anchor)
                val displayState=
                    state
                        ?: error(
                            "Health display state missing after display creation"
                        )
                reconcileVisibility(
                    display,
                    displayState.viewers,
                    desiredViewers
                )
            }
    }

    private fun upsertDamageDisplay(
        context: ArenaContext,
        mobUuid: UUID,
        damage: Double,
        anchor: Location,
        desiredViewers: Set<UUID>
    ) {
        val current=
            damageDisplays[mobUuid]
        val live=
            current?.displayUuid
                ?.let(server::getEntity)
                as? TextDisplay

        if(
            current!=null &&
            live!=null
        ) {
            current.accumulatedDamage +=
                damage
            current.expiresAtTick=
                context.gameTick +
                    DAMAGE_LIFETIME_TICKS
            live.text(
                Component.text(
                    MobCombatVisualProjection
                        .damageText(
                            current
                                .accumulatedDamage
                        )
                )
            )
            live.teleport(anchor)
            reconcileVisibility(
                live,
                current.viewers,
                desiredViewers
            )
            return
        }

        current?.let {
            context.entityIndex
                .transientDisplays
                .remove(
                    it.displayUuid
                )
        }

        val display=
            spawnTextDisplay(
                context,
                anchor,
                MobCombatVisualProjection
                    .damageText(damage),
                teleportDuration=0
            )
        val state=
            DamageDisplay(
                displayUuid=
                    display.uniqueId,
                accumulatedDamage=
                    damage,
                expiresAtTick=
                    context.gameTick +
                        DAMAGE_LIFETIME_TICKS
            )
        damageDisplays[mobUuid]=
            state
        reconcileVisibility(
            display,
            state.viewers,
            desiredViewers
        )
    }

    private fun syncDamageDisplays(
        context: ArenaContext,
        desiredViewers: Set<UUID>
    ) {
        damageDisplays
            .toList()
            .forEach { (mobUuid,state) ->
                if(
                    context.gameTick >=
                    state.expiresAtTick
                ) {
                    removeDamageDisplay(
                        context,
                        mobUuid
                    )
                    return@forEach
                }

                val display=
                    server.getEntity(
                        state.displayUuid
                    )
                if(display==null) {
                    context.entityIndex
                        .transientDisplays
                        .remove(
                            state.displayUuid
                        )
                    damageDisplays
                        .remove(mobUuid)
                    return@forEach
                }

                reconcileVisibility(
                    display,
                    state.viewers,
                    desiredViewers
                )
            }
    }

    private fun spawnTextDisplay(
        context: ArenaContext,
        location: Location,
        text: String,
        teleportDuration: Int
    ): TextDisplay {
        val display=
            location.world.spawn(
                location,
                TextDisplay::class.java
            ) { spawned ->
                spawned.text(
                    Component.text(text)
                )
                spawned.setBillboard(
                    Display.Billboard.CENTER
                )
                spawned.setSeeThrough(true)
                spawned.setShadowed(true)
                spawned.setPersistent(false)
                spawned.setInvulnerable(true)
                spawned.setGravity(false)
                spawned.setVisibleByDefault(false)
                spawned.setTeleportDuration(
                    teleportDuration
                )
            }
        context.entityIndex
            .transientDisplays +=
            display.uniqueId
        return display
    }

    private fun reconcileVisibility(
        display: Entity,
        current: MutableSet<UUID>,
        desired: Set<UUID>
    ) {
        (
            current - desired
        ).toList()
            .forEach { uuid ->
                server.getPlayer(uuid)
                    ?.hideEntity(
                        plugin,
                        display
                    )
                current.remove(uuid)
            }

        (
            desired - current
        ).forEach { uuid ->
            val player=
                server.getPlayer(uuid)
                    ?: return@forEach
            player.showEntity(
                plugin,
                display
            )
            current += uuid
        }
    }

    private fun removeHealthDisplay(
        context: ArenaContext,
        mobUuid: UUID
    ) {
        val state=
            healthDisplays
                .remove(mobUuid)
                ?: return
        removeDisplay(
            context,
            state.displayUuid
        )
    }

    private fun removeDamageDisplay(
        context: ArenaContext,
        mobUuid: UUID
    ) {
        val state=
            damageDisplays
                .remove(mobUuid)
                ?: return
        removeDisplay(
            context,
            state.displayUuid
        )
    }

    private fun removeDisplay(
        context: ArenaContext,
        displayUuid: UUID
    ) {
        server.getEntity(
            displayUuid
        )?.remove()
        context.entityIndex
            .transientDisplays
            .remove(displayUuid)
    }

    private fun healthAnchor(
        mobUuid: UUID
    ): Location? =
        livingAnchor(
            mobUuid,
            HEALTH_VERTICAL_GAP
        )

    private fun damageAnchor(
        mobUuid: UUID
    ): Location? =
        livingAnchor(
            mobUuid,
            DAMAGE_VERTICAL_GAP
        )

    private fun livingAnchor(
        mobUuid: UUID,
        verticalGap: Double
    ): Location? {
        val living=
            server.getEntity(
                mobUuid
            ) as? LivingEntity
                ?: return null
        return living.location
            .clone()
            .add(
                0.0,
                living.boundingBox.height +
                    verticalGap,
                0.0
            )
    }

    companion object {
        /**
         * Engineering presentation values. Historical evidence confirms the
         * toggles, not the exact original TextDisplay offsets/lifetime.
         */
        private const val
            HEALTH_VERTICAL_GAP =
            0.35
        private const val
            DAMAGE_VERTICAL_GAP =
            0.85
        private const val
            DAMAGE_LIFETIME_TICKS =
            12L
    }
}
