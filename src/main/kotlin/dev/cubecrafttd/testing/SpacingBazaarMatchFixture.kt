package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.castle.*
import dev.cubecrafttd.economy.*
import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.match.*
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.ui.*
import java.util.UUID

object SpacingBazaarMatchFixture {
    private val owner =
        UUID.fromString(
            "00000000-0000-0000-0000-000000004000"
        )

    private fun existingTower(
        id: Long,
        towerId: String,
        team: TeamId,
        x: Double
    ) = TowerRuntimeState(
        TowerIdentity(
            TowerInstanceId(id),
            towerId,owner,team
        ),
        TowerUpgradeState(
            1,
            dev.cubecrafttd.tower.visual
                .TowerPath.TOP
        ),
        TowerGeometryState(
            Vec3(x,0.0,0.0),
            Vec3(x,0.0,0.0),
            emptyList(),
            "fixture"
        )
    )

    fun run(): List<FixtureResult> {
        val context = ArenaContext(
            ArenaId("spacing"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000004001"
            ),
            TestingMapFactory.minimal()
        )
        val spacing =
            dev.cubecrafttd.tower.lifecycle
                .SameTowerSpacingPolicy()

        context.entityIndex.registerTower(
            existingTower(
                1,"ice",TeamId.RED,0.0
            )
        )
        context.entityIndex.registerTower(
            existingTower(
                2,"mage",TeamId.RED,30.0
            )
        )
        context.entityIndex.registerTower(
            existingTower(
                3,"poison",TeamId.RED,60.0
            )
        )
        context.entityIndex.registerTower(
            existingTower(
                4,"quake",TeamId.RED,90.0
            )
        )

        val ice11 =
            spacing.firstConflict(
                context,"ice",TeamId.RED,
                dev.cubecrafttd.map.BlockPos(
                    11,0,0
                )
            )
        val ice12 =
            spacing.firstConflict(
                context,"ice",TeamId.RED,
                dev.cubecrafttd.map.BlockPos(
                    12,0,0
                )
            )
        val mage7 =
            spacing.firstConflict(
                context,"mage",TeamId.RED,
                dev.cubecrafttd.map.BlockPos(
                    37,0,0
                )
            )
        val mage8 =
            spacing.firstConflict(
                context,"mage",TeamId.RED,
                dev.cubecrafttd.map.BlockPos(
                    38,0,0
                )
            )
        val poison14 =
            spacing.firstConflict(
                context,"poison",TeamId.RED,
                dev.cubecrafttd.map.BlockPos(
                    74,0,0
                )
            )
        val poison15 =
            spacing.firstConflict(
                context,"poison",TeamId.RED,
                dev.cubecrafttd.map.BlockPos(
                    75,0,0
                )
            )
        val quake9 =
            spacing.firstConflict(
                context,"quake",TeamId.RED,
                dev.cubecrafttd.map.BlockPos(
                    99,0,0
                )
            )
        val quake10 =
            spacing.firstConflict(
                context,"quake",TeamId.RED,
                dev.cubecrafttd.map.BlockPos(
                    100,0,0
                )
            )

        val ledger=EconomyLedger()
        val coinAccount=EconomyAccount(
            TeamId.RED,owner,
            EconomyCurrency.MATCH_COINS
        )
        val expAccount=EconomyAccount(
            TeamId.RED,owner,
            EconomyCurrency.MATCH_EXP
        )
        ledger.apply(
            EconomyTransaction(
                100,0,coinAccount,5000,
                EconomyReason.MATCH_INITIALIZATION,
                "bazaar-coins"
            )
        )
        ledger.apply(
            EconomyTransaction(
                101,0,expAccount,1000,
                EconomyReason.MATCH_INITIALIZATION,
                "bazaar-exp"
            )
        )
        val state=MatchBazaarState()
        val bazaar=BazaarTransactionService(
            ledger
        )
        bazaar.upgradeWeapon(
            state,TeamId.RED,owner,
            WeaponKind.SWORD,1,
            10,102,"sword-stone"
        )
        bazaar.unlockPotion(
            state,TeamId.RED,owner,
            "inferno",11,103,
            "unlock-inferno"
        )
        val token=bazaar.purchasePotionUse(
            state,TeamId.RED,owner,
            "inferno",12,104,
            "buy-inferno"
        )

        val guards=(1L..4L).map {
            GuardRuntime(
                GuardIdentity(
                    it,
                    if(it<=2) TeamId.RED
                    else TeamId.BLUE,
                    UUID.nameUUIDFromBytes(
                        "g-$it".toByteArray()
                    )
                )
            )
        }
        var starts=0
        val match=NormalMatchOrchestrator(
            armageddonType=
                ArmageddonType.HORDE,
            armageddonPort=
                ArmageddonStartPort {
                    _,_,_ -> starts++
                }
        )
        context.castles
            .getValue(TeamId.RED)
            .health=900.0
        context.castles
            .getValue(TeamId.BLUE)
            .health=800.0
        val pre=match.tick(
            context,29999,guards,
            TimeoutTiePolicy.DRAW
        )
        val start=match.tick(
            context,30000,guards,
            TimeoutTiePolicy.DRAW
        )
        val once=match.tick(
            context,30001,guards,
            TimeoutTiePolicy.DRAW
        )
        context.castles
            .getValue(TeamId.RED)
            .health=200.0
        context.castles
            .getValue(TeamId.BLUE)
            .health=150.0
        val end=match.tick(
            context,48000,guards,
            TimeoutTiePolicy.DRAW
        )

        return listOf(
            FixtureResult(
                "spacing-ice-12",
                ice11!=null && ice12==null
            ),
            FixtureResult(
                "spacing-mage-8",
                mage7!=null && mage8==null
            ),
            FixtureResult(
                "spacing-poison-15",
                poison14!=null &&
                    poison15==null
            ),
            FixtureResult(
                "spacing-quake-10",
                quake9!=null &&
                    quake10==null
            ),
            FixtureResult(
                "bazaar-weapon-and-potion-ledger",
                state.swordTierIndex==1 &&
                    "inferno" in
                        state.unlockedAoE &&
                    token.definition
                        .durationSeconds==8.0 &&
                    ledger.balance(
                        coinAccount
                    )==3600L &&
                    ledger.balance(
                        expAccount
                    )==700L
            ),
            FixtureResult(
                "match-armageddon-starts-once-at-25min",
                pre.phase==
                    MatchPhase.PRE_ARMAGEDDON &&
                    start.phase==
                        MatchPhase.ARMAGEDDON &&
                    start.armageddonActivated!=
                        null &&
                    once.armageddonActivated==
                        null &&
                    starts==1
            ),
            FixtureResult(
                "match-armageddon-caps-castles-and-disables-guards",
                guards.all{
                    it.lifecycle==
                        GuardLifecycleState
                            .DISABLED_ARMAGEDDON
                }
            ),
            FixtureResult(
                "match-hard-timeout-resolves-higher-health",
                end.phase==
                    MatchPhase.FINISHED &&
                    (end.outcome as?
                        MatchOutcome.Winner)
                        ?.team==TeamId.RED
            )
        )
    }
}
