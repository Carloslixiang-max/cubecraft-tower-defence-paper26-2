package dev.cubecrafttd.testing

import dev.cubecrafttd.admin.EngineeringMapPlacementBounds
import dev.cubecrafttd.admin.EngineeringPlaytestSetupRequest
import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.map.schematic.SchematicDimensions

object EngineeringPlaytestSetupRequestFixture {
    fun run(): List<FixtureResult> {
        fun parse(vararg args: String) = EngineeringPlaytestSetupRequest.parse(args.toList())
        val dimensions=SchematicDimensions(125,18,191)
        fun fits(origin: BlockPos)=runCatching {
            EngineeringMapPlacementBounds.validate(origin,dimensions,-64,320)
        }.isSuccess
        return listOf(
            FixtureResult("playtest-setup-player-mode-retained",
                parse("apply")==EngineeringPlaytestSetupRequest.StandingPlayer),
            FixtureResult("playtest-setup-explicit-origin-is-not-offset",
                parse("apply","farm","-125","80","191")==
                    EngineeringPlaytestSetupRequest.ExplicitOrigin("farm",BlockPos(-125,80,191))),
            FixtureResult("playtest-setup-rejects-incomplete-and-extra-arguments",
                listOf(emptyList(),listOf("apply","farm","1","2"),listOf("apply","farm","1","2","3","4"))
                    .all { runCatching { EngineeringPlaytestSetupRequest.parse(it) }.isFailure }),
            FixtureResult("playtest-setup-rejects-fractional-overflow-and-blank-world",
                runCatching { parse("apply","farm","0.5","80","0") }.isFailure &&
                    runCatching { parse("apply","farm","2147483648","80","0") }.isFailure &&
                    runCatching { parse("apply"," ","0","80","0") }.isFailure),
            FixtureResult("playtest-setup-checks-complete-vertical-volume",
                fits(BlockPos(0,-64,0)) && fits(BlockPos(0,302,0)) &&
                    !fits(BlockPos(0,-65,0)) && !fits(BlockPos(0,303,0))),
            FixtureResult("playtest-setup-checks-entire-horizontal-volume-without-overflow",
                fits(BlockPos(-30_000_000,80,-30_000_000)) &&
                    fits(BlockPos(29_999_875,80,29_999_809)) &&
                    !fits(BlockPos(29_999_876,80,0)) && !fits(BlockPos(0,80,29_999_810)) &&
                    !fits(BlockPos(Int.MAX_VALUE,80,0)) && !fits(BlockPos(Int.MIN_VALUE,80,0)))
        )
    }
}
