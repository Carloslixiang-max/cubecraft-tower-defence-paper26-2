package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.combat.*
import dev.cubecrafttd.match.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.tower.visual.TowerPath
import dev.cubecrafttd.truth.*
import dev.cubecrafttd.troop.*
import java.util.UUID

object WitherArmageddonLiveRuntimeFixture {
    private fun <T> fallback(v:T)=
        ResolvedTruth(
            v,
            ResolutionSource
                .ENGINEERING_FALLBACK
        )

    private fun config()=
        WitherArmageddonConfig(
            speedBlocksPerTick=
                fallback(2.0),
            regenerationHealthPerSecond=
                fallback(20.0),
            firstTowerSkullDelayTicks=
                fallback(1L),
            towerSkullIntervalTicks=
                fallback(20L),
            towerSkullRangeBlocks=
                fallback(100.0),
            towerTargetPolicy=
                fallback(
                    WitherTowerTargetPolicy
                        .NEAREST_IN_RANGE_ENGINEERING
                ),
            firstTrailWitherSkeletonDelayTicks=
                fallback(1L),
            trailWitherSkeletonIntervalTicks=
                fallback(20L),
            trailWitherSkeletonsPerEvent=
                fallback(2),
            trailWitherSkeletonLevel=
                fallback(4)
        )

    private fun tower(
        id:Long,
        team:TeamId,
        x:Double
    )=TowerRuntimeState(
        TowerIdentity(
            TowerInstanceId(id),
            "archer",
            UUID.nameUUIDFromBytes(
                "wither-tower-$id"
                    .toByteArray()
            ),
            team
        ),
        TowerUpgradeState(
            1,TowerPath.TOP
        ),
        TowerGeometryState(
            dev.cubecrafttd.map.Vec3(
                x,0.0,0.0
            ),
            dev.cubecrafttd.map.Vec3(
                x,0.0,0.0
            ),
            emptyList(),
            "fixture"
        )
    )

    fun run():List<FixtureResult> {
        val context=ArenaContext(
            ArenaId("wither-live"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000026100"
            ),
            TestingMapFactory.minimal(),
            rng=ArenaDeterministicRng(3)
        ).also {
            it.state=ArenaState.RUNNING
            it.gameTick=100
        }

        context.entityIndex
            .registerTower(
                tower(
                    1,TeamId.RED,0.5
                )
            )
        context.entityIndex
            .registerTower(
                tower(
                    2,TeamId.RED,50.0
                )
            )
        context.entityIndex
            .registerTower(
                tower(
                    3,TeamId.BLUE,0.5
                )
            )

        val spawned=
            linkedMapOf<
                UUID,
                MobEntitySpawnRequest
            >()
        val moved=
            linkedMapOf<
                UUID,
                dev.cubecrafttd.map.Vec3
            >()
        val destroyed=
            linkedSetOf<TowerInstanceId>()

        val runtime=
            WitherArmageddonTickRuntime(
                config=config(),
                activationTick=100,
                routeAssignment=
                    TeamConfiguredRouteAssignmentPolicy(
                        mapOf(
                            TeamId.RED to
                                "red-route",
                            TeamId.BLUE to
                                "blue-route"
                        )
                    ),
                entitySpawn=
                    MobEntitySpawnPort {
                        request ->
                        val uuid=
                            UUID.nameUUIDFromBytes(
                                "wither-spawn-${request.instanceId.value}"
                                    .toByteArray()
                            )
                        spawned[uuid]=request
                        uuid
                    },
                livePosition=
                    MobPositionUpdatePort {
                        uuid,pos ->
                        moved[uuid]=pos
                    },
                destroyPort=
                    ArmageddonTowerDestroyPort {
                        destroyed += it
                        true
                    }
            )

        runtime.start(context)
        val bosses=
            runtime.activeBosses()

        val redBoss=bosses.single {
            it.attackedTeam==
                TeamId.RED
        }
        val redMob=
            context.entityIndex
                .mobsByUuid
                .getValue(
                    redBoss.entityUuid
                )

        // Damage boss before the next Armageddon runtime tick.
        redMob.combat.health=4900.0

        context.gameTick=101
        runtime.tick(context)

        val metrics=
            runtime.metricsSnapshot()
        val redAfter=
            context.entityIndex
                .mobsByUuid
                .getValue(
                    redBoss.entityUuid
                )

        val trail=
            context.entityIndex
                .mobsByUuid
                .values
                .filter {
                    it.identity.mobId==
                        "skeleton" &&
                    it.identity.attackedTeam==
                        TeamId.RED
                }

        val witherProfile=
            RecommendedMatureMobCombatProfiles
                .get("wither")
        val archerCapability=
            TowerAttackCapability(
                "archer",
                setOf(
                    TargetLayer.GROUND,
                    TargetLayer.AIR,
                    TargetLayer.BOSS
                ),
                setOf(DamageKind.PHYSICAL)
            )
        val mageCapability=
            TowerAttackCapability(
                "mage",
                setOf(
                    TargetLayer.GROUND,
                    TargetLayer.BOSS
                ),
                setOf(DamageKind.FIRE)
            )

        return listOf(
            FixtureResult(
                "wither-spawns-one-boss-per-side",
                bosses.size==2 &&
                    bosses.all {
                        context.entityIndex
                            .mobsByUuid[
                                it.entityUuid
                            ]?.combat
                            ?.lifecycle==
                            MobLifecycleState
                                .ARMAGEDDON_BOSS
                    }
            ),
            FixtureResult(
                "wither-regen-syncs-with-shared-combat-state",
                redAfter.combat.health==
                    4901.0
            ),
            FixtureResult(
                "wither-nearest-skull-destroys-team-tower",
                TowerInstanceId(1) in
                    destroyed &&
                    metrics.towersDestroyed>=2
            ),
            FixtureResult(
                "wither-trail-skeleton-midroute",
                trail.size==2 &&
                    trail.all {
                        it.form.formId==
                            "wither_skeleton" &&
                        it.route.routeProgress>
                            0.0
                    }
            ),
            FixtureResult(
                "wither-profile-allows-archer-blocks-mage",
                MobCombatEligibility
                    .directTargetable(
                        witherProfile,
                        archerCapability
                    ) &&
                    !MobCombatEligibility
                        .directTargetable(
                            witherProfile,
                            mageCapability
                        )
            )
        )
    }
}
