package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.paper.*
import dev.cubecrafttd.ui.*
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
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

class BukkitMenuBridge(
    private val plugin: Plugin,
    private val renderer:
        BukkitMenuItemRenderer,
    private val actionSink:
        BukkitMenuActionSink
) : LiveMenuOpenPort, Listener {
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
    ) {
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
        populate(
            inventory,
            menu
        )

        // Opening a replacement inventory closes the previous view and may fire
        // InventoryCloseEvent synchronously. Register the NEW state only after
        // that transition so the old close cannot delete the refreshed state.
        player.openInventory(
            inventory
        )
        rememberOpenMenu(
            playerUuid,
            menu
        )
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
            top.size!=menu.size
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
        menu.slotActionIds.forEach {
            (slot,action) ->
            val item=
                renderer.render(
                    action,
                    menu.slotIconHints[
                        slot
                    ]
                )
            menu.slotDisplayNames[
                slot
            ]?.let {
                displayName ->
                item.editMeta {
                    meta ->
                    meta.displayName(
                        Component.text(
                            displayName
                        )
                    )
                }
            }
            inventory.setItem(
                slot,
                item
            )
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
        val raw=event.rawSlot
        if(
            raw < 0 ||
            raw >=
                event.view
                    .topInventory.size
        ) return

        event.isCancelled=true
        val action=
            actions[raw]
                ?: return

        if(
            action.startsWith(
                "noop:"
            )
        ) {
            return
        }

        val click=when(event.click) {
            ClickType.LEFT ->
                ClickKind.LEFT
            ClickType.RIGHT ->
                ClickKind.RIGHT
            ClickType.SHIFT_LEFT ->
                ClickKind.SHIFT_LEFT
            ClickType.SHIFT_RIGHT ->
                ClickKind.SHIFT_RIGHT
            else -> return
        }

        actionSink.accept(
            MenuActionInvocation(
                player.uniqueId,
                action,
                click
            )
        )
    }

    @EventHandler
    fun onClose(
        event: InventoryCloseEvent
    ) {
        val uuid=
            event.player.uniqueId
        openActions.remove(
            uuid
        )
        openRefreshKinds.remove(
            uuid
        )
    }
}
