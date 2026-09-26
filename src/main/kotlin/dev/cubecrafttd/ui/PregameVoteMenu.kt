package dev.cubecrafttd.ui

import dev.cubecrafttd.match.HistoricalPregameArmageddonVoteOption
import dev.cubecrafttd.match.HistoricalPregamePricingVoteOption

data class PregameVoteMenuState(
    val armageddonVote:
        HistoricalPregameArmageddonVoteOption?,
    val pricingVote:
        HistoricalPregamePricingVoteOption?,
    val waitingPosition: Int?,
    val countdownRunning: Boolean
)

/**
 * Safe Engineering projection of the historically confirmed pregame vote
 * categories/options.
 *
 * Historical evidence confirms the vote categories/options and an End Crystal
 * inventory entry point, but the exact inner-menu slot layout/icons have not
 * been recovered strongly enough. Therefore this layout remains explicitly
 * ENGINEERING_FALLBACK and does not claim original GUI fidelity.
 */
object PregameVoteMenuProjection {
    fun menu(
        state: PregameVoteMenuState
    ): MenuDefinition {
        fun marker(selected: Boolean):
            String =
            if(selected)
                " ✓ YOUR VOTE"
            else
                ""

        val status=
            if(state.countdownRunning)
                "Starting"
            else
                state.waitingPosition
                    ?.let {
                        "Queue #$it"
                    }
                    ?: "Waiting"

        return MenuDefinition(
            title=
                "Tower Defence Voting · " +
                    status,
            size=27,
            slots=listOf(
                MenuSlot(
                    1,
                    "pregame:armageddon:random",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Armageddon: Random" +
                        marker(
                            state.armageddonVote==
                                HistoricalPregameArmageddonVoteOption
                                    .RANDOM
                        )
                ),
                MenuSlot(
                    3,
                    "pregame:armageddon:wither",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Armageddon: Wither" +
                        marker(
                            state.armageddonVote==
                                HistoricalPregameArmageddonVoteOption
                                    .WITHER
                        )
                ),
                MenuSlot(
                    5,
                    "pregame:armageddon:lightning",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Armageddon: Lightning" +
                        marker(
                            state.armageddonVote==
                                HistoricalPregameArmageddonVoteOption
                                    .LIGHTNING
                        )
                ),
                MenuSlot(
                    7,
                    "pregame:armageddon:horde",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Armageddon: Horde" +
                        marker(
                            state.armageddonVote==
                                HistoricalPregameArmageddonVoteOption
                                    .HORDE
                        )
                ),
                MenuSlot(
                    11,
                    "pregame:pricing:normal",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Pricing: Normal" +
                        marker(
                            state.pricingVote==
                                HistoricalPregamePricingVoteOption
                                    .NORMAL
                        )
                ),
                MenuSlot(
                    13,
                    "pregame:pricing:double_income",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Pricing: Double Income" +
                        marker(
                            state.pricingVote==
                                HistoricalPregamePricingVoteOption
                                    .DOUBLE_INCOME
                        )
                ),
                MenuSlot(
                    15,
                    "pregame:pricing:quick_start",
                    UiEvidenceStatus
                        .ENGINEERING_FALLBACK,
                    "Pricing: Quick Start" +
                        marker(
                            state.pricingVote==
                                HistoricalPregamePricingVoteOption
                                    .QUICK_START
                        )
                )
            ),
            evidenceStatus=
                UiEvidenceStatus
                    .ENGINEERING_FALLBACK
        )
    }
}
