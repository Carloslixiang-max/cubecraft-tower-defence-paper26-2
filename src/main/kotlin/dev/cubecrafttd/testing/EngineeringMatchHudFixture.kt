package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.TeamId
import dev.cubecrafttd.match.ArmageddonType
import dev.cubecrafttd.ui.EngineeringMatchHudProjector
import dev.cubecrafttd.ui.EngineeringMatchHudSnapshot

object EngineeringMatchHudFixture {
    fun run(): List<FixtureResult> {
        val pre =
            EngineeringMatchHudProjector.render(
                EngineeringMatchHudSnapshot(
                    team=TeamId.RED,
                    ownCastleHealth=1000.0,
                    ownCastleMaxHealth=1000.0,
                    enemyCastleHealth=750.0,
                    enemyCastleMaxHealth=1000.0,
                    coins=125L,
                    exp=30L,
                    elapsedTicks=65L*20L,
                    armageddonType=
                        ArmageddonType.WITHER,
                    armageddonStarted=false
                )
            )

        val active =
            EngineeringMatchHudProjector.render(
                EngineeringMatchHudSnapshot(
                    team=TeamId.BLUE,
                    ownCastleHealth=250.0,
                    ownCastleMaxHealth=1000.0,
                    enemyCastleHealth=100.0,
                    enemyCastleMaxHealth=1000.0,
                    coins=999L,
                    exp=321L,
                    elapsedTicks=26L*60L*20L,
                    armageddonType=
                        ArmageddonType.LIGHTNING,
                    armageddonStarted=true
                )
            )

        return listOf(
            FixtureResult(
                "engineering-hud-pre-armageddon",
                pre ==
                    "ENG RED | Castle 1000.0/1000.0 | Enemy 750.0/1000.0 | " +
                    "125C 30XP | 01:05 | ARM WITHER in 23:55"
            ),
            FixtureResult(
                "engineering-hud-active-armageddon",
                active ==
                    "ENG BLUE | Castle 250.0/1000.0 | Enemy 100.0/1000.0 | " +
                    "999C 321XP | 26:00 | ARM LIGHTNING"
            )
        )
    }
}
