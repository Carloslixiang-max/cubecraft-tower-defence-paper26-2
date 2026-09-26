package dev.cubecrafttd.ui

import dev.cubecrafttd.match.ArmageddonType
import dev.cubecrafttd.match.HistoricalPregameArmageddonVoteOption
import dev.cubecrafttd.match.HistoricalPregamePricingVoteOption

data class PregameVoteMenuState(
    val armageddonVote:
        HistoricalPregameArmageddonVoteOption?,
    val pricingVote:
        HistoricalPregamePricingVoteOption?,
    val waitingPosition: Int?,
    val countdownRunning: Boolean,
    val runnableArmageddonTypes:
        Set<ArmageddonType> =
        ArmageddonType.entries.toSet()
) {
    init {
        require(
            runnableArmageddonTypes
                .isNotEmpty()
        )
    }
}

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
        fun marker(
            selected: Boolean
        ): String =
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

        val slots=
            buildList {
                add(
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
                    )
                )

                fun addArmageddon(
                    type: ArmageddonType,
                    slot: Int,
                    action: String,
                    name: String,
                    vote:
                        HistoricalPregameArmageddonVoteOption
                ) {
                    if(
                        type !in
                            state.runnableArmageddonTypes
                    ) return

                    add(
                        MenuSlot(
                            slot,
                            action,
                            UiEvidenceStatus
                                .ENGINEERING_FALLBACK,
                            name +
                                marker(
                                    state.armageddonVote==
                                        vote
                                )
                        )
                    )
                }

                addArmageddon(
                    ArmageddonType.WITHER,
                    3,
                    "pregame:armageddon:wither",
                    "Armageddon: Wither",
                    HistoricalPregameArmageddonVoteOption
                        .WITHER
                )
                addArmageddon(
                    ArmageddonType.LIGHTNING,
                    5,
                    "pregame:armageddon:lightning",
                    "Armageddon: Lightning",
                    HistoricalPregameArmageddonVoteOption
                        .LIGHTNING
                )
                addArmageddon(
                    ArmageddonType.HORDE,
                    7,
                    "pregame:armageddon:horde",
                    "Armageddon: Horde",
                    HistoricalPregameArmageddonVoteOption
                        .HORDE
                )

                add(
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
                    )
                )
                add(
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
                    )
                )
                add(
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
                )
            }

        return MenuDefinition(
            title=
                "Tower Defence Voting · " +
                    status,
            size=27,
            slots=slots,
            evidenceStatus=
                UiEvidenceStatus
                    .ENGINEERING_FALLBACK
        )
    }
}
