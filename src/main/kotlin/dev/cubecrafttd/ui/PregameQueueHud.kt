package dev.cubecrafttd.ui

import dev.cubecrafttd.match.HistoricalPregameArmageddonVoteOption
import dev.cubecrafttd.match.HistoricalPregamePricingVoteOption

data class PregameQueueHudState(
    val waitingPosition: Int?,
    val countdownSeconds: Int?,
    val armageddonVote:
        HistoricalPregameArmageddonVoteOption?,
    val pricingVote:
        HistoricalPregamePricingVoteOption?
)

/**
 * Engineering-only non-invasive queue HUD.
 *
 * It deliberately uses the action bar instead of taking an inventory slot from
 * the player before the durable match snapshot has been captured.
 */
object PregameQueueHudProjection {
    fun text(
        state: PregameQueueHudState
    ): String {
        val armageddon=
            when(
                state.armageddonVote
            ) {
                null ->
                    "Random(default)"
                else ->
                    state.armageddonVote
                        .name
                        .replace('_',' ')
            }
        val pricing=
            when(
                state.pricingVote
            ) {
                null ->
                    "Normal(default)"
                else ->
                    state.pricingVote
                        .name
                        .replace('_',' ')
            }

        val prefix=
            state.countdownSeconds
                ?.let {
                    "TD starts in ${it}s"
                }
                ?: state.waitingPosition
                    ?.let {
                        "TD queue #${it}"
                    }
                ?: "TD queue"

        return "$prefix · A:$armageddon · P:$pricing · /ctdvote"
    }
}
