package dev.cubecrafttd.ui

data class HistoricalMatchSidebarView(
    val title: String,
    val coins: Long,
    val exp: Long,
    val footer: String
) {
    init {
        require(coins>=0L)
        require(exp>=0L)
    }

    val coinsLine: String
        get() = "$coins Coins"

    val expLine: String
        get() = "$exp EXP"
}

object HistoricalMatchSidebarProjector {
    fun project(
        coins: Long,
        exp: Long
    ): HistoricalMatchSidebarView =
        HistoricalMatchSidebarView(
            title="Tower Defence",
            coins=coins,
            exp=exp,
            footer="play.cubecraft.net"
        )
}
