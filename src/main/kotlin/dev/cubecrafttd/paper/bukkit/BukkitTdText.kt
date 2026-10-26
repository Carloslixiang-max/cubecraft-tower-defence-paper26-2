package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.ui.TdLanguage
import dev.cubecrafttd.ui.TdText
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/** Console evidence stays in English; player-facing text follows the server's selected language. */
object BukkitTdText {
    var language: TdLanguage = TdLanguage.EN_US
        private set

    fun configure(value: String) {
        language = TdLanguage.parse(value) ?: TdLanguage.ZH_CN
    }

    fun text(value: String): String = TdText.translate(value, language)

    fun action(result: Any): String {
        if (language == TdLanguage.EN_US) return "TD action: $result"
        return when (result) {
            is dev.cubecrafttd.ui.MatchMenuActionResult.SummonerDraftChanged ->
                "已选怪物：${text(result.receipt.key.mobId)} ${result.receipt.key.level} 级 × ${result.receipt.quantity}；共 ${result.receipt.totalDraftUnits} 只。"
            is dev.cubecrafttd.ui.MatchMenuActionResult.SummonerBatchSent ->
                "已派出 ${result.receipt.totalUnits} 只怪物，消耗 ${result.receipt.totalCost} 金币。"
            is dev.cubecrafttd.ui.MatchMenuActionResult.ProgressionChanged ->
                "${text(result.mobId)}已调整为 ${result.level} 级；经验变化：${result.expDelta}。"
            is dev.cubecrafttd.ui.MatchMenuActionResult.GoldmineUpgraded ->
                "金矿已升至 ${result.level} 级，每秒收入 ${result.coinsPerSecond} 金币。"
            is dev.cubecrafttd.ui.MatchMenuActionResult.WeaponTierUpgraded ->
                "${text(result.weapon.name)}已升至 ${result.tierIndex + 1} 阶。"
            is dev.cubecrafttd.ui.MatchMenuActionResult.PotionUnlocked ->
                "已解锁${text(result.potionId)}范围药水。"
            is dev.cubecrafttd.ui.MatchMenuActionResult.SettingsChanged -> "设置已更新。"
            is dev.cubecrafttd.ui.MatchMenuActionResult.HotbarEditorSelectionChanged -> "已选择物品，请点击目标快捷栏位。"
            is dev.cubecrafttd.ui.MatchMenuActionResult.HotbarLayoutChanged -> "快捷栏布局已保存。"
            is dev.cubecrafttd.ui.TowerManagementActionResult.Stats ->
                "${text(result.stats.towerId)} ${result.stats.level} 级 · ${text(result.stats.path?.name ?: "NONE")} · 累计投入 ${result.stats.currentGameCoinsInvested} 金币。"
            else -> "操作已完成。"
        }
    }
}

fun CommandSender.sendTdMessage(message: String) =
    sendMessage(if (this is Player) BukkitTdText.text(message) else message)
