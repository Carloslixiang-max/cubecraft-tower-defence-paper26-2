package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.ui.*
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

/** Real Paper items through the same projection used by menu opening and redraw. */
object BukkitMenuProjectionSmoke {
    fun run() {
        val renderer = BukkitEngineeringMenuRenderer()
        val fallback = MenuDefinition("Projection smoke", 9, listOf(
            MenuSlot(0, "noop:plain", UiEvidenceStatus.ENGINEERING_FALLBACK,
                "Plain name", "book", listOf("Plain lore", "")),
            MenuSlot(1, "noop:unnamed", UiEvidenceStatus.UNKNOWN, iconHint = "paper"),
            MenuSlot(2, "hotbar:place:3", UiEvidenceStatus.ENGINEERING_FALLBACK,
                "Empty slot", "empty-slot")
        ), UiEvidenceStatus.ENGINEERING_FALLBACK).toLiveView()
        val menus = listOf(
            fallback,
            TowerBuilderMenus.combinedMatureEngineering.toLiveView(),
            TowerBuilderMenus.pathSelector2021("archer").toLiveView(),
            TowerBuilderMenus.pathSelector2021("zeus").toLiveView(),
            TowerManagementMenus.mature2021(1, "Zeus Tower", 1, false).toLiveView(),
            PregamePreferenceMenus.settings(PlayerMatchSettings(lifetimeWins = 20)).toLiveView(),
            PregamePreferenceMenus.hotbar(HotbarLayout.ENGINEERING_RUNTIME_DEFAULT, null).toLiveView()
        )
        menus.forEach { menu ->
            menu.slotActionIds.keys.forEach { slot ->
                val item = BukkitMenuItemProjection.render(menu, slot, renderer)
                if (item.type.isAir) {
                    check(menu.slotIconHints[slot] == "empty-slot" && !item.hasItemMeta())
                } else {
                    val before = item.itemMeta
                    val saved = ItemStack.deserializeBytes(item.serializeAsBytes()).itemMeta
                    check(saved.displayName() == before.displayName() && saved.lore() == before.lore()) { "Text roundtrip changed ${menu.title}/$slot" }
                    val name = requireNotNull(saved.displayName())
                    check(name.decoration(TextDecoration.ITALIC) == TextDecoration.State.FALSE) { "Italic name ${menu.title}/$slot" }
                    check(name.decoration(TextDecoration.BOLD) == TextDecoration.State.FALSE) { "Bold name root ${menu.title}/$slot" }
                    saved.lore().orEmpty().forEach { line ->
                        check(line.decoration(TextDecoration.ITALIC) == TextDecoration.State.FALSE) { "Italic lore ${menu.title}/$slot" }
                        check(line.decoration(TextDecoration.BOLD) == TextDecoration.State.FALSE) { "Bold lore root ${menu.title}/$slot" }
                    }
                }
            }
        }
        val archer = TowerBuilderMenus.pathSelector2021("archer").toLiveView()
        val top = ItemStack.deserializeBytes(BukkitMenuItemProjection.render(archer, 11, renderer).serializeAsBytes())
        val bottom = ItemStack.deserializeBytes(BukkitMenuItemProjection.render(archer, 15, renderer).serializeAsBytes())
        check(top.type == Material.ARROW && bottom.type == Material.ARROW)
        check(!top.itemMeta.hasEnchantmentGlintOverride() &&
            bottom.itemMeta.hasEnchantmentGlintOverride() && bottom.itemMeta.enchantmentGlintOverride == true) {
            "Archer path glint changed in production projection/byte roundtrip"
        }
        val zeus = TowerBuilderMenus.pathSelector2021("zeus").toLiveView()
        val description = BukkitMenuItemProjection.render(zeus, 15, renderer).itemMeta.lore()!![1]
        check((description.children()[1] as TextComponent).content() == "baby Zeus")
        check(description.children()[1].decoration(TextDecoration.BOLD) == TextDecoration.State.TRUE)
        // A later plain projection must not inherit a previous recovered style.
        val plain = BukkitMenuItemProjection.render(fallback, 0, renderer).itemMeta
        check((plain.displayName() as TextComponent).content() == "Plain name")
        check(plain.displayName()!!.children().isEmpty())
        check((plain.lore()!![1] as TextComponent).content().isEmpty())
    }
}
