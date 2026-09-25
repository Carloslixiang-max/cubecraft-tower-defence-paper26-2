package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.match.*
import dev.cubecrafttd.truth.*
import java.util.UUID

object ArmageddonFirstEventDelayFixture {
    private fun <T> fallback(v:T)=
        ResolvedTruth(
            v,
            ResolutionSource
                .ENGINEERING_FALLBACK
        )

    fun run():List<FixtureResult> {
        val context=ArenaContext(
            ArenaId("arm-delay"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000025100"
            ),
            TestingMapFactory.minimal(),
            rng=ArenaDeterministicRng(1)
        )
        context.state=ArenaState.RUNNING

        var destroyCalls=0
        val lightning=
            LightningArmageddonTickRuntime(
                LightningArmageddonConfig(
                    fallback(20L),
                    fallback(1)
                ),
                activationTick=100,
                firstStrikeDelayTicks=
                    fallback(10L),
                destroyPort=
                    ArmageddonTowerDestroyPort {
                        destroyCalls++
                        true
                    }
            )

        context.gameTick=109
        lightning.tick(context)
        val before=destroyCalls

        context.gameTick=110
        lightning.tick(context)

        return listOf(
            FixtureResult(
                "armageddon-first-event-delay-boundary",
                before==0 &&
                    lightning.metricsSnapshot()
                        .strikes==1
            )
        )
    }
}
