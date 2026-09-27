package dev.cubecrafttd.ui

object DynamicMenuRefreshPolicy {
    /**
     * Engineering refresh cadence only. Historical evidence establishes the
     * state changes, not an exact client refresh frequency.
     */
    const val INTERVAL_TICKS: Long = 10L

    fun kindForTitle(
        title: String
    ): String? =
        when(title) {
            "Mob Summoner" ->
                "summoner"
            "Troop upgrades" ->
                "progression"
            "Bazaar" ->
                "bazaar"
            "Settings" ->
                "settings"
            "Hotbar editor" ->
                "hotbar"
            else ->
                null
        }
}
