package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.paper.*
import dev.cubecrafttd.ui.*
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import java.util.UUID

fun interface BukkitMenuItemRenderer {
    fun render(
        actionId: String,
        iconHint: String?
    ): ItemStack
}

fun interface BukkitMenuActionSink {
    fun accept(
        invocation: MenuActionInvocation
    )
}

fun interface BukkitDynamicMenuRefreshProvider {
    fun project(
        playerUuid: UUID,
        kind: String
    ): LiveMenuView?
}

fun interface BukkitMenuOpenObservationSink {
    fun observe(
        playerUuid: UUID,
        menu: LiveMenuView
    )
}

fun interface BukkitMenuExitSink {
    fun accept(playerUuid: UUID)
}

class BukkitMenuBridge(
    private val plugin: Plugin,
    private val renderer:
        BukkitMenuItemRenderer,
    private val actionSink:
        BukkitMenuActionSink,
    private val openObservation:
        BukkitMenuOpenObservationSink =
        BukkitMenuOpenObservationSink {
            _,_ -> Unit
        },
    exitSink: BukkitMenuExitSink = BukkitMenuExitSink { }
) : LiveMenuOpenPort, Listener {
    private val viewLifecycle = BukkitMenuViewLifecycle(exitSink::accept)
    private val openActions =
        linkedMapOf<
            UUID,
            Map<Int,String>
        >()
    private val openRefreshKinds=
        linkedMapOf<UUID,String>()
    private var refreshProvider:
        BukkitDynamicMenuRefreshProvider? =
        null
    private var refreshTaskStarted=
        false

    init {
        plugin.server.pluginManager
            .registerEvents(
                this,plugin
            )
    }

    fun enableDynamicRefresh(
        provider:
            BukkitDynamicMenuRefreshProvider
    ) {
        check(!refreshTaskStarted) {
            "Dynamic menu refresh already enabled"
        }
        refreshProvider=provider
        refreshTaskStarted=true
        plugin.server.scheduler
            .runTaskTimer(
                plugin,
                Runnable {
                    refreshDynamicMenus()
                },
                DynamicMenuRefreshPolicy
                    .INTERVAL_TICKS,
                DynamicMenuRefreshPolicy
                    .INTERVAL_TICKS
            )
    }

    override fun open(
        playerUuid: UUID,
        menu: LiveMenuView
    ) = openMenu(playerUuid, menu, preservePlacement = false)

    /** Only the explicit world-placement builder/path transition preserves its pending target. */
    fun openPlacementMenu(playerUuid: UUID, menu: LiveMenuView) =
        openMenu(playerUuid, menu, preservePlacement = true)

    private fun openMenu(playerUuid: UUID, menu: LiveMenuView, preservePlacement: Boolean) {
        val player = plugin.server
            .getPlayer(playerUuid)
            ?: return
        requireValidMenu(menu)

        val inventory =
            Bukkit.createInventory(
                null,
                menu.size,
                Component.text(
                    menu.title
                )
            )
        // Opening a replacement inventory closes the previous view and may fire
        // InventoryCloseEvent synchronously. Register the NEW state only after
        // that transition so the old close cannot delete the refreshed state.
        val opened = viewLifecycle.replace(playerUuid, inventory, preservePlacement) {
            populate(inventory, menu)
            val view = player.openInventory(inventory)
            view != null && view.topInventory == inventory && player.openInventory.topInventory == inventory
        }
        if (!opened) return
        rememberOpenMenu(
            playerUuid,
            menu
        )
        runCatching {
            openObservation.observe(
                playerUuid,
                menu
            )
        }.onFailure {
            plugin.logger.warning(
                "Could not persist TD menu-open live evidence: " +
                    it.javaClass.simpleName +
                    ": " +
                    it.message
            )
        }
    }

    /**
     * Redraws an already-owned menu in-place. This avoids close/open flicker
     * for affordability, rollback-window and send-cooldown state changes.
     */
    fun refreshIfOpen(
        playerUuid: UUID,
        menu: LiveMenuView
    ): Boolean {
        val player=
            plugin.server
                .getPlayer(
                    playerUuid
                ) ?: return false
        if(
            playerUuid !in
                openActions
        ) {
            return false
        }
        requireValidMenu(menu)

        val top=
            player.openInventory
                .topInventory
        if(
            top.size!=menu.size || !viewLifecycle.owns(playerUuid, top)
        ) {
            return false
        }

        val previousKind=
            openRefreshKinds[
                playerUuid
            ]
        val nextKind=
            DynamicMenuRefreshPolicy
                .kindForTitle(
                    menu.title
                )
        if(
            previousKind!=null &&
            previousKind!=nextKind
        ) {
            return false
        }

        populate(
            top,
            menu
        )
        rememberOpenMenu(
            playerUuid,
            menu
        )
        return true
    }

    private fun refreshDynamicMenus() {
        val provider=
            refreshProvider
                ?: return

        openRefreshKinds
            .toMap()
            .forEach {
                (playerUuid,kind) ->
                val player=
                    plugin.server
                        .getPlayer(
                            playerUuid
                        )
                if(
                    player==null ||
                    !player.isOnline ||
                    playerUuid !in
                        openActions
                ) {
                    openActions.remove(
                        playerUuid
                    )
                    openRefreshKinds
                        .remove(
                            playerUuid
                        )
                    viewLifecycle.forget(playerUuid)
                    return@forEach
                }

                val next=
                    runCatching {
                        provider.project(
                            playerUuid,
                            kind
                        )
                    }.getOrNull()
                        ?: return@forEach

                refreshIfOpen(
                    playerUuid,
                    next
                )
            }
    }

    private fun requireValidMenu(
        menu: LiveMenuView
    ) {
        require(
            menu.size > 0 &&
                menu.size % 9 == 0
        )
        require(
            menu.slotActionIds.keys
                .all {
                    it in
                        0 until
                        menu.size
                }
        )
    }

    private fun populate(
        inventory: Inventory,
        menu: LiveMenuView
    ) {
        inventory.clear()
        menu.slotActionIds.keys.forEach { slot ->
            inventory.setItem(slot, BukkitMenuItemProjection.render(menu, slot, renderer))
        }
    }

    private fun rememberOpenMenu(
        playerUuid: UUID,
        menu: LiveMenuView
    ) {
        openActions[playerUuid] =
            LinkedHashMap(
                menu.slotActionIds
            )
        val kind=
            DynamicMenuRefreshPolicy
                .kindForTitle(
                    menu.title
                )
        if(kind==null) {
            openRefreshKinds.remove(
                playerUuid
            )
        } else {
            openRefreshKinds[
                playerUuid
            ]=kind
        }
    }

    @EventHandler
    fun onClick(
        event: InventoryClickEvent
    ) {
        val player=
            event.whoClicked as? Player
                ?: return
        val actions=
            openActions[player.uniqueId]
                ?: return
        if (!viewLifecycle.owns(player.uniqueId, event.view.topInventory)) return
        val invocation = BukkitMenuInteractionGuard.click(event, actions) ?: return
        val inventory = event.view.topInventory
        val rawSlot = event.rawSlot
        // InventoryClickEvent is inside an inventory transaction. View changes
        // must run next tick, and stale queued clicks must not target a new view.
        plugin.server.scheduler.runTask(plugin, viewLifecycle.whenCurrent(player.uniqueId, inventory) {
            if (openActions[invocation.playerUuid]?.get(rawSlot) == invocation.actionId)
                actionSink.accept(invocation)
        })
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        if (player.uniqueId in openActions && viewLifecycle.owns(player.uniqueId, event.view.topInventory))
            BukkitMenuInteractionGuard.drag(event)
    }

    @EventHandler
    fun onClose(
        event: InventoryCloseEvent
    ) {
        val uuid=
            event.player.uniqueId
        if (!viewLifecycle.close(uuid, event.inventory)) return
        openActions.remove(
            uuid
        )
        openRefreshKinds.remove(
            uuid
        )
    }
}
