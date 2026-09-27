package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.ArenaContext
import dev.cubecrafttd.match.MatchSessionState
import dev.cubecrafttd.mob.PlayerMobDamageEvent
import dev.cubecrafttd.mob.PlayerMobDamageEventPort
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
 * Settings options.
 *
 * Damage indicators consume authoritative player-owned damage events. Exact
 * original styling, offsets and lifetime remain Engineering fallback.
 */
class BukkitMobCombatVisualProjectionService(
    private val plugin: Plugin
) : PlayerMobDamageEventPort {
    private data class ViewerScopedDisplay(
        val displayUuid: UUID,
        val viewers: MutableSet<UUID> =
            linkedSetOf()
    )

    private data class DamageDisplayKey(
        val mobUuid: UUID,
        val playerUuid: UUID
    )

    private data class PendingDamage(
        var amount: Double,
        var anchor: Location
    )

    private data class DamageDisplay(
        val displayUuid: UUID,
        val viewers: MutableSet<UUID> =
            linkedSetOf(),
        var accumulatedDamage: Double,
        var expiresAtTick: Long
    )

    private val server=plugin.server
    private val healthDisplays=
        linkedMapOf<UUID,ViewerScopedDisplay>()
    private val pendingDamage=
        linkedMapOf<DamageDisplayKey,PendingDamage>()

    /**
     * Bounded by tracked mobs x active players: repeated hits aggregate into
     * one display per mob/player pair instead of spawning an entity per hit.
     */
    private val damageDisplays=
        linkedMapOf<DamageDisplayKey,DamageDisplay>()

    override fun record(
        event: PlayerMobDamageEvent
    ) {
        if(event.amount<=1.0e-9) return
        val anchor=
            damageAnchor(event.targetMobUuid)
                ?: return
        val key=
            DamageDisplayKey(
                event.targetMobUuid,
                event.playerUuid
            )
        val pending=pendingDamage[key]
        if(pending==null) {
            pendingDamage[key]=
                PendingDamage(
                    event.amount,
                    anchor
                )
        } else {
            pending.amount += event.amount
            pending.anchor=anchor
        }
    }

    fun project(
        context: ArenaContext,
        session: MatchSessionState
    ) {
        val healthViewers=
            session.players.values
                .filter {
                    MobCombatVisualProjection
                        .digitalHealthEnabled(
                            it.interaction.settings
                        )
                }
                .mapTo(linkedSetOf()) {
                    it.playerUuid
                }

        val damageViewers=
            session.players.values
                .filter {
                    MobCombatVisualProjection
                        .damageIndicatorsEnabled(
                            it.interaction.settings
                        )
                }
                .mapTo(linkedSetOf()) {
                    it.playerUuid
                }

        pendingDamage.toList()
            .forEach { (key,pending) ->
                if(key.playerUuid in damageViewers) {
                    upsertDamageDisplay(
                        context,
                        key,
                        pending.amount,
                        pending.anchor
                    )
                }
            }
        pendingDamage.clear()

        if(context.gameTick%2L==0L) {
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
            context.entityIndex.mobsByUuid.keys.toSet()

        healthDisplays.keys
            .filter {
                it !in currentMobIds ||
                    desiredViewers.isEmpty()
            }
            .toList()
            .forEach {
                removeHealthDisplay(
                    context,it
                )
            }

        if(desiredViewers.isEmpty()) return

        context.entityIndex.mobsByUuid
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
                var state=healthDisplays[uuid]
                var display=
                    state?.displayUuid
                        ?.let(server::getEntity)
                        as? TextDisplay

                if(display==null) {
                    state?.let {
                        context.entityIndex
                            .transientDisplays
                            .remove(it.displayUuid)
                    }
                    display=
                        spawnTextDisplay(
                            context,
                            anchor,
                            text,
                            2
                        )
                    state=
                        ViewerScopedDisplay(
                            display.uniqueId
                        )
                    healthDisplays[uuid]=state
                }

                display.text(Component.text(text))
                display.teleport(anchor)
                reconcileVisibility(
                    display,
                    requireNotNull(state).viewers,
                    desiredViewers
                )
            }
    }

    private fun upsertDamageDisplay(
        context: ArenaContext,
        key: DamageDisplayKey,
        damage: Double,
        anchor: Location
    ) {
        val current=damageDisplays[key]
        val live=
            current?.displayUuid
                ?.let(server::getEntity)
                as? TextDisplay

        if(current!=null && live!=null) {
            current.accumulatedDamage += damage
            current.expiresAtTick=
                context.gameTick+
                    DAMAGE_LIFETIME_TICKS
            live.text(
                Component.text(
                    MobCombatVisualProjection
                        .damageText(
                            current.accumulatedDamage
                        )
                )
            )
            live.teleport(anchor)
            reconcileVisibility(
                live,
                current.viewers,
                setOf(key.playerUuid)
            )
            return
        }

        current?.let {
            context.entityIndex.transientDisplays
                .remove(it.displayUuid)
        }

        val display=
            spawnTextDisplay(
                context,
                anchor,
                MobCombatVisualProjection
                    .damageText(damage),
                0
            )
        val state=
            DamageDisplay(
                display.uniqueId,
                accumulatedDamage=damage,
                expiresAtTick=
                    context.gameTick+
                        DAMAGE_LIFETIME_TICKS
            )
        damageDisplays[key]=state
        reconcileVisibility(
            display,
            state.viewers,
            setOf(key.playerUuid)
        )
    }

    private fun syncDamageDisplays(
        context: ArenaContext,
        enabledViewers: Set<UUID>
    ) {
        damageDisplays.toList()
            .forEach { (key,state) ->
                if(
                    key.playerUuid !in enabledViewers ||
                    server.getPlayer(key.playerUuid)==null ||
                    context.gameTick>=state.expiresAtTick
                ) {
                    removeDamageDisplay(
                        context,key
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
                        .remove(state.displayUuid)
                    damageDisplays.remove(key)
                    return@forEach
                }
                reconcileVisibility(
                    display,
                    state.viewers,
                    setOf(key.playerUuid)
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
                spawned.text(Component.text(text))
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
        context.entityIndex.transientDisplays +=
            display.uniqueId
        return display
    }

    private fun reconcileVisibility(
        display: Entity,
        current: MutableSet<UUID>,
        desired: Set<UUID>
    ) {
        (current-desired).toList()
            .forEach { uuid ->
                server.getPlayer(uuid)
                    ?.hideEntity(
                        plugin,display
                    )
                current.remove(uuid)
            }
        (desired-current).forEach { uuid ->
            val player=
                server.getPlayer(uuid)
                    ?: return@forEach
            player.showEntity(
                plugin,display
            )
            current += uuid
        }
    }

    private fun removeHealthDisplay(
        context: ArenaContext,
        mobUuid: UUID
    ) {
        val state=healthDisplays.remove(mobUuid)
            ?: return
        removeDisplay(
            context,state.displayUuid
        )
    }

    private fun removeDamageDisplay(
        context: ArenaContext,
        key: DamageDisplayKey
    ) {
        val state=damageDisplays.remove(key)
            ?: return
        removeDisplay(
            context,state.displayUuid
        )
    }

    private fun removeDisplay(
        context: ArenaContext,
        displayUuid: UUID
    ) {
        server.getEntity(displayUuid)?.remove()
        context.entityIndex.transientDisplays
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
            server.getEntity(mobUuid)
                as? LivingEntity
                ?: return null
        return living.location.clone()
            .add(
                0.0,
                living.boundingBox.height+
                    verticalGap,
                0.0
            )
    }

    companion object {
        private const val HEALTH_VERTICAL_GAP=0.35
        private const val DAMAGE_VERTICAL_GAP=0.85
        private const val DAMAGE_LIFETIME_TICKS=12L
    }
}
