package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.ui.TowerPlacementFlowState
import org.bukkit.Bukkit
import java.util.UUID

/** Real Paper inventory equality; simulated close/open delivery and real placement-flow state. */
object BukkitMenuViewLifecycleSmoke {
    fun run() {
        val player = UUID(113, 1)
        val flow = TowerPlacementFlowState()
        var exits = 0
        val lifecycle = BukkitMenuViewLifecycle { check(it == player); exits++; flow.cancel() }
        val builder = Bukkit.createInventory(null, 45)
        val path = Bukkit.createInventory(null, 27)
        val settings = Bukkit.createInventory(null, 45)
        val point = BlockPos(1, 2, 3)

        flow.begin(point)
        check(lifecycle.replace(player, builder, true) { true })
        check(!lifecycle.close(player, settings) && lifecycle.owns(player, builder)) {
            "A foreign same-size inventory must not close the owned menu"
        }
        var dispatched = 0
        val currentClick = lifecycle.whenCurrent(player, builder) { dispatched++ }
        val staleClick = lifecycle.whenCurrent(player, builder) { dispatched++ }
        check(dispatched == 0) { "Capturing a click must not dispatch it synchronously" }
        currentClick.run()
        check(dispatched == 1)
        flow.chooseTower("archer")
        check(lifecycle.replace(player, path, true) { check(lifecycle.close(player, builder)); true })
        check(flow.pending?.clickedBlock == point && flow.pending?.towerId == "archer" && exits == 0)
        staleClick.run()
        check(dispatched == 1) { "Queued old-view click must not dispatch after replacement" }
        check(!lifecycle.close(player, builder) && lifecycle.owns(player, path)) {
            "An old close must not clear replacement ownership"
        }
        check(lifecycle.close(player, path) && flow.pending == null && exits == 1)

        flow.begin(point)
        check(lifecycle.replace(player, builder, true) { true })
        flow.chooseTower("archer")
        check(lifecycle.replace(player, settings, false) { check(lifecycle.close(player, builder)); true })
        check(flow.pending == null && exits == 2 && lifecycle.owns(player, settings))

        // A rejected open after a synchronous close clears its hidden placement target.
        flow.begin(point)
        check(!lifecycle.replace(player, path, true) { check(lifecycle.close(player, settings)); false })
        check(flow.pending == null && !lifecycle.owns(player, path))

        // A rejected open that leaves the old view visible must keep that view protected.
        check(lifecycle.replace(player, builder, true) { true })
        flow.begin(point)
        check(!lifecycle.replace(player, path, true) { false })
        check(flow.pending == null && lifecycle.owns(player, builder))

        flow.begin(point)
        check(runCatching {
            lifecycle.replace(player, path, true) { error("Synthetic cancelled open") }
        }.isFailure)
        check(flow.pending == null && lifecycle.owns(player, builder))
        flow.begin(point)
        check(lifecycle.close(player, builder) && flow.pending == null) {
            "Failed replacement must not leave close suppression armed"
        }
        check(lifecycle.replace(player, settings, false) { true })
        lifecycle.forget(player)
        check(!lifecycle.owns(player, settings) && !lifecycle.close(player, settings))
    }
}
