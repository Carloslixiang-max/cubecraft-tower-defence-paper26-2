package dev.cubecrafttd.paper.bukkit

import org.bukkit.inventory.Inventory
import java.util.UUID

/** Binds ownership to a concrete inventory, including synchronous close/open replacement. */
class BukkitMenuViewLifecycle(private val exit: (UUID) -> Unit) {
    private val owned = linkedMapOf<UUID, Inventory>()
    private val replacements = linkedMapOf<UUID, Boolean>()

    fun owns(player: UUID, inventory: Inventory): Boolean = owned[player] == inventory

    fun whenCurrent(player: UUID, inventory: Inventory, action: () -> Unit): Runnable = Runnable {
        if (owns(player, inventory)) action()
    }

    fun replace(player: UUID, inventory: Inventory, preservePlacement: Boolean, open: () -> Boolean): Boolean {
        val previous = replacements.put(player, preservePlacement)
        try {
            val opened = try { open() } catch (failure: Throwable) {
                exit(player)
                throw failure
            }
            if (opened) owned[player] = inventory else exit(player)
            return opened
        } finally {
            if (previous == null) replacements.remove(player) else replacements[player] = previous
        }
    }

    fun close(player: UUID, inventory: Inventory): Boolean {
        if (!owns(player, inventory)) return false
        owned.remove(player)
        if (replacements[player] != true) exit(player)
        return true
    }

    fun forget(player: UUID) { owned.remove(player) }
}
