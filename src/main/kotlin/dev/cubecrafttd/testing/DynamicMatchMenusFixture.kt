package dev.cubecrafttd.testing

import dev.cubecrafttd.match.*
import dev.cubecrafttd.progression.*
import dev.cubecrafttd.ui.*
import java.util.UUID

object DynamicMatchMenusFixture {
    fun run():List<FixtureResult> {
        val player=
            PlayerMatchSessionState(
                UUID.fromString(
                    "00000000-0000-0000-0000-000000032001"
                ),
                TroopProgressionState(
                    unlockedLevelByMob=
                        linkedMapOf(
                            "zombie" to 2,
                            "spider" to 1
                        )
                )
            )

        val summoner=
            DynamicMatchMenus
                .summoner(player)
        val progression=
            DynamicMatchMenus
                .progression(player)

        val bazaarLocked=
            DynamicMatchMenus
                .bazaar(player)
        player.bazaar.unlockedAoE +=
            "meteor"
        val bazaarUnlocked=
            DynamicMatchMenus
                .bazaar(player)

        return listOf(
            FixtureResult(
                "dynamic-summoner-uses-current-unlocked-level",
                summoner.slots.any {
                    it.actionId==
                        "summoner:mob:zombie:l2"
                } &&
                    summoner.slots.any {
                        it.actionId==
                            "summoner:send"
                    }
            ),
            FixtureResult(
                "dynamic-progression-has-upgrade-and-rollback",
                progression.slots.any {
                    it.actionId==
                        "progression:upgrade:zombie"
                } &&
                    progression.slots.any {
                        it.actionId==
                            "progression:rollback:zombie"
                    }
            ),
            FixtureResult(
                "dynamic-bazaar-potion-state-projection",
                bazaarLocked.slots.any {
                    it.actionId==
                        "bazaar:potion:unlock:meteor"
                } &&
                    bazaarUnlocked.slots.any {
                        it.actionId==
                            "bazaar:potion:use:meteor"
                    }
            ),
            FixtureResult(
                "dynamic-match-menu-layouts-remain-engineering",
                listOf(
                    summoner,
                    progression,
                    bazaarLocked
                ).all {
                    it.evidenceStatus==
                        UiEvidenceStatus
                            .ENGINEERING_FALLBACK &&
                    it.slots.all { slot ->
                        slot.evidenceStatus==
                            UiEvidenceStatus
                                .ENGINEERING_FALLBACK
                    }
                }
            )
        )
    }
}
