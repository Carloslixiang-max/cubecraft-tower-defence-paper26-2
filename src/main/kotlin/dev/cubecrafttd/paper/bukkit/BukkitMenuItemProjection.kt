package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.paper.LiveMenuView
import org.bukkit.inventory.ItemStack

/** One production path for menu population, dynamic redraw and Paper item checks. */
object BukkitMenuItemProjection {
    fun render(menu: LiveMenuView, slot: Int, renderer: BukkitMenuItemRenderer): ItemStack {
        val action = requireNotNull(menu.slotActionIds[slot]) { "No menu action at $slot" }
        val item = renderer.render(action, menu.slotIconHints[slot])
        // An AIR action slot stays visually empty, without attempting item metadata.
        if (item.type.isAir) return item
        item.editMeta { meta ->
            val style = menu.slotTextStyles[slot]
            menu.slotDisplayNames[slot]?.let { name ->
                meta.displayName(BukkitMenuText.render(BukkitTdText.text(name), style?.nameColor,
                    dev.cubecrafttd.ui.TdText.spans(name, style?.nameBoldSpans.orEmpty(), BukkitTdText.language)))
            }
            menu.slotLore[slot]?.let { lore ->
                meta.lore(lore.mapIndexed { index, text ->
                    BukkitMenuText.render(BukkitTdText.text(text), style?.loreColors?.get(index),
                        dev.cubecrafttd.ui.TdText.spans(text, style?.loreBoldSpans?.get(index).orEmpty(), BukkitTdText.language))
                })
            }
        }
        return item
    }
}
