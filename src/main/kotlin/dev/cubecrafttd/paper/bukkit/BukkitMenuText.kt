package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.ui.MenuBoldSpan
import dev.cubecrafttd.ui.MenuTextColor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

/** Same component construction for inventory rendering and the real Paper smoke. */
object BukkitMenuText {
    fun render(
        text: String,
        color: MenuTextColor?,
        explicitStyle: Boolean,
        boldSpans: List<MenuBoldSpan> = emptyList()
    ): Component {
        require(boldSpans.all { it.endExclusive <= text.length })
        require(boldSpans.zipWithNext().all { (a, b) -> a.endExclusive <= b.start })
        var component = if (boldSpans.isEmpty()) Component.text(text) else Component.empty()
        if (color != null) component = component.color(when (color) {
            MenuTextColor.GREEN -> NamedTextColor.GREEN
            MenuTextColor.RED -> NamedTextColor.RED
            MenuTextColor.GRAY -> NamedTextColor.GRAY
            MenuTextColor.GOLD -> NamedTextColor.GOLD
        })
        if (explicitStyle) component = component
            .decoration(TextDecoration.ITALIC, false)
            .decoration(TextDecoration.BOLD, false)
        var offset = 0
        boldSpans.forEach { span ->
            if (offset < span.start) component = component.append(Component.text(text.substring(offset, span.start)))
            component = component.append(Component.text(text.substring(span.start, span.endExclusive))
                .decoration(TextDecoration.BOLD, true))
            offset = span.endExclusive
        }
        if (boldSpans.isNotEmpty() && offset < text.length)
            component = component.append(Component.text(text.substring(offset)))
        return component
    }
}
