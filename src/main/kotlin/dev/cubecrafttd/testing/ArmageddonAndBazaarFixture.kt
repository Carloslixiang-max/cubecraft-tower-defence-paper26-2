package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.castle.CastleRuntime
import dev.cubecrafttd.match.*
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.tower.visual.TowerPath
import dev.cubecrafttd.truth.*
import dev.cubecrafttd.ui.*
import java.util.UUID

object ArmageddonAndBazaarFixture {
    private fun <T> fallback(
        value: T
    ) = ResolvedTruth(
        value,
        ResolutionSource.ENGINEERING_FALLBACK
    )

    fun run(): List<FixtureResult> {
        val horde =
            HordeArmageddonEngine(
                HordeArmageddonConfig(
                    fallback(20L),
                    listOf(
                        HordeSpawnOrder(
                            "zombie",5,3
                        ),
                        HordeSpawnOrder(
                            "giant",1,1
                        )
                    )
                ),
                startTick=100
            )
        val beforeHorde =
            horde.tick(99)
        val firstHorde =
            horde.tick(100)
        val secondHorde =
            horde.tick(120)

        val context = ArenaContext(
            ArenaId("lightning"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000003000"
            ),
            TestingMapFactory.minimal(),
            rng=ArenaDeterministicRng(123)
        )
        context.state = ArenaState.RUNNING
        fun tower(
            id: Long,
            team: TeamId
        ) = TowerRuntimeState(
            TowerIdentity(
                TowerInstanceId(id),
                "mage",
                UUID.nameUUIDFromBytes(
                    "owner-$id".toByteArray()
                ),
                team
            ),
            TowerUpgradeState(
                1,TowerPath.TOP
            ),
            TowerGeometryState(
                dev.cubecrafttd.map.Vec3(
                    0.0,0.0,0.0
                ),
                dev.cubecrafttd.map.Vec3(
                    0.0,0.0,0.0
                ),
                emptyList(),
                "fixture"
            )
        )
        (1L..3L).forEach {
            context.entityIndex
                .registerTower(
                    tower(it,TeamId.RED)
                )
        }
        (11L..13L).forEach {
            context.entityIndex
                .registerTower(
                    tower(it,TeamId.BLUE)
                )
        }
        context.gameTick=200
        val lightning =
            LightningArmageddonEngine(
                LightningArmageddonConfig(
                    fallback(40L),
                    fallback(1)
                ),
                startTick=200
            )
        val strike1=lightning.tick(context)
        val noStrike=run {
            context.gameTick=201
            lightning.tick(context)
        }
        val lightningReplayContext =
            ArenaContext(
                ArenaId("lightning"),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000003000"
                ),
                TestingMapFactory.minimal(),
                rng=ArenaDeterministicRng(123)
            )
        (1L..3L).forEach {
            lightningReplayContext
                .entityIndex
                .registerTower(
                    tower(it,TeamId.RED)
                )
        }
        (11L..13L).forEach {
            lightningReplayContext
                .entityIndex
                .registerTower(
                    tower(it,TeamId.BLUE)
                )
        }
        lightningReplayContext.gameTick=200
        val strikeReplay =
            LightningArmageddonEngine(
                LightningArmageddonConfig(
                    fallback(40L),
                    fallback(1)
                ),
                startTick=200
            ).tick(lightningReplayContext)

        val castle=CastleRuntime(TeamId.BLUE)
        val wither=WitherArmageddonEngine(
            WitherArmageddonConfig(
                speedBlocksPerTick=
                    fallback(1.0),
                regenerationHealthPerSecond=
                    fallback(20.0),
                firstTowerSkullDelayTicks=
                    fallback(0L),
                towerSkullIntervalTicks=
                    fallback(10L),
                towerSkullRangeBlocks=
                    fallback(10.0),
                towerTargetPolicy=
                    fallback(
                        WitherTowerTargetPolicy
                            .NEAREST_IN_RANGE_ENGINEERING
                    ),
                firstTrailWitherSkeletonDelayTicks=
                    fallback(0L),
                trailWitherSkeletonIntervalTicks=
                    fallback(20L),
                trailWitherSkeletonsPerEvent=
                    fallback(1),
                trailWitherSkeletonLevel=
                    fallback(4)
            )
        )
        val witherState =
            WitherArmageddonState(
                TeamId.BLUE,
                health=4900.0,
                nextTowerSkullTick=10,
                nextTrailSpawnTick=20
            )
        val witherTick =
            wither.tick(
                witherState,
                gameTick=20,
                routeLength=1.0,
                targetCastle=castle
            )

        val inferno =
            RecommendedMatureBazaarDefinitions
                .potion("inferno")
        val zeus =
            RecommendedMatureBazaarDefinitions
                .potion("zeus")

        return listOf(
            FixtureResult(
                "armageddon-horde-explicit-plan",
                beforeHorde.isEmpty() &&
                    firstHorde.size==2 &&
                    firstHorde.all{
                        it.mobId=="zombie" &&
                            it.quantity==3
                    } &&
                    secondHorde.size==2 &&
                    secondHorde.all{
                        it.mobId=="giant"
                    } &&
                    horde.finished()
            ),
            FixtureResult(
                "armageddon-lightning-one-per-team",
                strike1?.towerIds?.size==2 &&
                    strike1.towerIds.any{
                        it in 1L..3L
                    } &&
                    strike1.towerIds.any{
                        it in 11L..13L
                    } &&
                    noStrike==null
            ),
            FixtureResult(
                "armageddon-lightning-replay-deterministic",
                strike1?.towerIds ==
                    strikeReplay?.towerIds
            ),
            FixtureResult(
                "armageddon-wither-5000-max-regen-and-instant-castle-loss",
                RECOMMENDED_WITHER_MAX_HEALTH==
                    5000.0 &&
                    witherState.health==4901.0 &&
                    witherTick.fireTowerSkull &&
                    witherTick.spawnWitherSkeletonTrail &&
                    witherTick.reachedCastleThisTick &&
                    castle.health==0.0
            ),
            FixtureResult(
                "bazaar-sword-bow-tiers",
                RecommendedMatureBazaarDefinitions
                    .swordTiers.map{
                        it.unlockCoins
                    }==listOf(0L,1000L,2000L) &&
                    RecommendedMatureBazaarDefinitions
                    .bowTiers.map{
                        it.unlockCoins
                    }==listOf(0L,1000L,2000L)
            ),
            FixtureResult(
                "bazaar-inferno-2021-duration",
                inferno.unlockExp==300L &&
                    inferno.useCostCoins==400L &&
                    inferno.durationSeconds==8.0 &&
                    inferno.baseline2020[
                        "durationSeconds"
                    ]==10.0
            ),
            FixtureResult(
                "bazaar-zeus-mature-exact-damage-remains-unresolved",
                zeus.unlockExp==450L &&
                    zeus.useCostCoins==1200L &&
                    zeus.maxAffectedTroops==4 &&
                    zeus.damagePerTickOrMeteor==
                        null &&
                    zeus.baseline2020[
                        "damagePerBolt"
                    ]==57.0
            ),
            FixtureResult(
                "bazaar-weapon-2021-charge-table-not-faked",
                RecommendedMatureBazaarDefinitions
                    .abilities.all{
                        it.recommendedMatureRequiredHits==
                            null
                    }
            )
        )
    }
}
