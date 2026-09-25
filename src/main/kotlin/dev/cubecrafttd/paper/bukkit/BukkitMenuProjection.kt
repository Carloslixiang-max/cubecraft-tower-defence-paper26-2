package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.paper.LiveMenuView
import dev.cubecrafttd.ui.MenuDefinition

fun MenuDefinition.toLiveView():
    LiveMenuView =
    LiveMenuView(
        title=title,
        size=size,
        slotActionIds=
            slots.associate {
                it.slot to it.actionId
            }
    )
