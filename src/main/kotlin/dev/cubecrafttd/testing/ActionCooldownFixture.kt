package dev.cubecrafttd.testing

import dev.cubecrafttd.economy.*
import dev.cubecrafttd.player.AoEPotionCooldownGate
import java.util.UUID

object ActionCooldownFixture {
    fun run(): List<FixtureResult> {
        val player = UUID.fromString(
            "00000000-0000-0000-0000-000000014001"
        )
        val tracker = DeterministicCooldownTracker()
        val aoe = AoEPotionCooldownGate(tracker)

        val sendKey =
            CooldownKey("sender:$player")

        val sendStart = tracker.consume(
            sendKey,
            100L,
            NormalModeConstants
                .SEND_COOLDOWN_TICKS
        )
        val sendTooSoon =
            !tracker.isReady(sendKey,399L)
        val sendReady =
            tracker.isReady(sendKey,400L)

        // Separate action scope: troop send does not implicitly lock AoE.
        val aoeStart =
            aoe.consumeSuccessfulThrow(
                player,100L
            )
        val aoeTooSoon =
            !aoe.isReady(player,399L)
        val aoeReady =
            aoe.isReady(player,400L)

        return listOf(
            FixtureResult(
                "action-cooldown-send-15s",
                sendStart &&
                    sendTooSoon &&
                    sendReady &&
                    NormalModeConstants
                        .SEND_COOLDOWN_TICKS ==
                        300L
            ),
            FixtureResult(
                "action-cooldown-aoe-15s",
                aoeStart.readyAtTick ==
                    400L &&
                    aoeTooSoon &&
                    aoeReady &&
                    NormalModeConstants
                        .AOE_POTION_COOLDOWN_TICKS ==
                        300L
            ),
            FixtureResult(
                "action-cooldown-scopes-independent",
                tracker.readyAtTick(
                    sendKey
                ) == 400L &&
                    tracker.readyAtTick(
                        aoe.key(player)
                    ) == 400L &&
                    sendKey != aoe.key(player)
            )
        )
    }
}
