package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.match.*
import dev.cubecrafttd.mob.TeamConfiguredRouteAssignmentPolicy
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.tower.visual.TowerPath
import dev.cubecrafttd.truth.*
import dev.cubecrafttd.troop.*
import java.util.UUID

object ArmageddonLiveRuntimeServicesFixture {
    private fun <T> fallback(v:T)=
        ResolvedTruth(
            v,
            ResolutionSource
                .ENGINEERING_FALLBACK
        )

    private fun tower(
        id:Long,
        team:TeamId
    )=TowerRuntimeState(
        TowerIdentity(
            TowerInstanceId(id),
            "mage",
            UUID.nameUUIDFromBytes(
                "arm-tower-$id".toByteArray()
            ),
            team
        ),
        TowerUpgradeState(
            1,TowerPath.TOP
        ),
        TowerGeometryState(
            dev.cubecrafttd.map.Vec3(
                id.toDouble(),0.0,0.0
            ),
            dev.cubecrafttd.map.Vec3(
                id.toDouble(),0.0,0.0
            ),
            emptyList(),"fixture"
        )
    )

    fun run():List<FixtureResult> {
        val lightningContext=
            ArenaContext(
                ArenaId("lightning-live"),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000024100"
                ),
                TestingMapFactory.minimal(),
                rng=ArenaDeterministicRng(7)
            ).also {
                it.state=ArenaState.RUNNING
                it.gameTick=100
            }

        (1L..3L).forEach {
            lightningContext.entityIndex
                .registerTower(
                    tower(it,TeamId.RED)
                )
        }
        (11L..13L).forEach {
            lightningContext.entityIndex
                .registerTower(
                    tower(it,TeamId.BLUE)
                )
        }

        val destroyed=
            linkedSetOf<TowerInstanceId>()
        val lightning=
            LightningArmageddonTickRuntime(
                LightningArmageddonConfig(
                    fallback(20L),
                    fallback(1)
                ),
                activationTick=100,
                firstStrikeDelayTicks=
                    fallback(0L),
                destroyPort=
                    ArmageddonTowerDestroyPort {
                        destroyed += it
                        true
                    }
            )
        lightning.tick(
            lightningContext
        )
        val lm=
            lightning.metricsSnapshot()

        val hordeContext=
            ArenaContext(
                ArenaId("horde-live"),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000024200"
                ),
                TestingMapFactory.minimal()
            ).also {
                it.state=ArenaState.RUNNING
                it.gameTick=200
            }
        val liveSpawns=
            linkedMapOf<
                UUID,
                MobEntitySpawnRequest
            >()
        val horde=
            HordeArmageddonTickRuntime(
                HordeArmageddonConfig(
                    fallback(20L),
                    listOf(
                        HordeSpawnOrder(
                            "zombie",1,2
                        )
                    )
                ),
                activationTick=200,
                firstWaveDelayTicks=
                    fallback(0L),
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
                                "arm-spawn-${request.instanceId.value}"
                                    .toByteArray()
                            )
                        liveSpawns[uuid]=request
                        uuid
                    }
            )
        horde.tick(hordeContext)
        val hm=horde.metricsSnapshot()

        return listOf(
            FixtureResult(
                "lightning-runtime-balanced-selection",
                destroyed.size==2 &&
                    destroyed.any {
                        it.value in 1L..3L
                    } &&
                    destroyed.any {
                        it.value in 11L..13L
                    } &&
                    lm.strikes==1 &&
                    lm.towersDestroyed==2
            ),
            FixtureResult(
                "horde-runtime-spawns-both-sides",
                liveSpawns.size==4 &&
                    hordeContext.entityIndex
                        .mobsByUuid.size==4 &&
                    hordeContext.entityIndex
                        .mobsByDefendingTeam
                        .getValue(
                            TeamId.RED
                        ).size==2 &&
                    hordeContext.entityIndex
                        .mobsByDefendingTeam
                        .getValue(
                            TeamId.BLUE
                        ).size==2 &&
                    hm.mobsSpawned==4
            ),
            FixtureResult(
                "horde-runtime-system-sender",
                hordeContext.entityIndex
                    .mobsByUuid.values
                    .all {
                        it.identity
                            .senderPlayerUuid==
                            ARMAGEDDON_SYSTEM_SENDER_UUID
                    }
            )
        )
    }
}
