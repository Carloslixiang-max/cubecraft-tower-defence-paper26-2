package dev.cubecrafttd.testing

import dev.cubecrafttd.ui.*
import kotlin.test.*

class TdLanguageTest {
    private fun zh(text: String) = TdText.translate(text, TdLanguage.ZH_CN)

    @Test fun localizesMenusWithoutChangingActions() {
        val menus = listOf(TowerBuilderMenus.combinedMatureEngineering,
            TowerBuilderMenus.pathSelector2021("archer"),
            TowerManagementMenus.mature2021(1, "Archer Tower", 3, false),
            PregamePreferenceMenus.settings(PlayerMatchSettings(lifetimeWins = 20)))
        menus.forEach { menu ->
            assertNotEquals(menu.title, zh(menu.title))
            menu.slots.forEach { slot ->
                slot.displayName?.let { assertNotEquals(it, zh(it), "Untranslated name: $it") }
                slot.lore.filter { it.isNotEmpty() }.forEach { assertNotEquals(it, zh(it)) }
            }
        }
        assertEquals("弓箭塔 III", zh("Archer Tower III"))
        assertEquals("僵尸 2 级", zh("zombie L2"))
        assertEquals("路线 2", zh("BOTTOM"))
    }

    @Test fun translatesNestedErrorsAndPreservesCommandTokensAndUnknownData() {
        val text = zh("ctdinstall ERROR: Wait for Managed Farm READY before visiting")
        assertTrue(text.contains("地图安装提示") && text.contains("/ctdinstall status"))
        assertEquals("用法：/ctdinstall <create|status|visit>", zh("Usage: /ctdinstall <create|status|visit>"))
        assertEquals("custom-player-UUID-42", zh("custom-player-UUID-42"))
        assertTrue(zh("Joined TD 1v1 queue at position #1. Start is currently blocked by MANAGED_FARM_NOT_READY.")
            .contains("MANAGED_FARM_NOT_READY"))
    }

    @Test fun remapsPartialBoldToTranslatedWords() {
        val original = "Click to Enable this tower's rangefinder"
        val translated = zh(original)
        val span = TdText.spans(original, listOf(MenuBoldSpan(9, 15, MenuTextColor.GREEN)), TdLanguage.ZH_CN).single()
        assertEquals("开启", translated.substring(span.start, span.endExclusive))
        assertEquals(MenuTextColor.GREEN, span.color)
        val zeus = TdText.spans("Summons baby Zeus", listOf(MenuBoldSpan(8, 17)), TdLanguage.ZH_CN).single()
        assertEquals("小宙斯", zh("Summons baby Zeus").substring(zeus.start, zeus.endExclusive))
    }

    @Test fun englishPassesThroughAndLocaleParsingIsExplicit() {
        assertEquals(TdLanguage.ZH_CN, TdLanguage.parse("zh-CN"))
        assertEquals(TdLanguage.EN_US, TdLanguage.parse("en_US"))
        assertNull(TdLanguage.parse("unknown"))
        assertEquals("Archer Tower III", TdText.translate("Archer Tower III", TdLanguage.EN_US))
        assertEquals("", zh(""))
    }
}
