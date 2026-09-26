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
            },
        slotDisplayNames=
            slots.mapNotNull {
                slot ->
                slot.displayName?.let {
                    slot.slot to it
                }
            }.toMap(),
        slotIconHints=
            slots.mapNotNull {
                slot ->
                slot.iconHint?.let {
                    slot.slot to it
                }
            }.toMap()
    )
