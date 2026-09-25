package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.map.*
import dev.cubecrafttd.match.*
import dev.cubecrafttd.paper.PaperBlockWorldAdapter
import dev.cubecrafttd.stats.*
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.tower.lifecycle.*
import dev.cubecrafttd.tower.visual.*
import java.util.UUID

private class TeardownFakeWorld :
    PaperBlockWorldAdapter {
    val blocks=
        linkedMapOf<
            BlockKey,BlockSnapshot
        >()

    override fun snapshot(
        block: BlockKey
    ): BlockSnapshot =
        blocks[block] ?:
            BlockSnapshot("minecraft:air")

    override fun apply(
        block: BlockKey,
        snapshot: BlockSnapshot,
        applyPhysics: Boolean
    ) {
        blocks[block]=snapshot
    }

    override fun currentBlockData(
        block: BlockKey
    ): String =
        snapshot(block).blockData
}

object ArenaTeardownFixture {
    fun run(): List<FixtureResult> {
        val redPlayer=
            UUID.fromString(
                "00000000-0000-0000-0000-000000007001"
            )
        val bluePlayer=
            UUID.fromString(
                "00000000-0000-0000-0000-000000007002"
            )
        val context=ArenaContext(
            ArenaId("teardown"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000007100"
            ),
            TestingMapFactory.minimal()
        )
        context.state=ArenaState.RUNNING
        context.redTeam.players += redPlayer
        context.blueTeam.players += bluePlayer

        val world=TeardownFakeWorld()
        val bodyService=
            TowerBodyMutationService(
                world,
                context.towerBodyLedger,
                RotationCache { b,_ -> b }
            )

        val body=TowerBodyDefinition(
            "mage",1,TowerPath.TOP,
            "teardown-body",
            listOf(
                RelativeBodyBlock(
                    BlockPos(0,0,0),
                    "minecraft:stone"
                )
            ),
            BodyEvidenceStatus
                .ENGINEERING_PLACEHOLDER,
            listOf("fixture")
        )
        bodyService.place(
            1L,BlockPos(0,0,2),
            body,QuarterTurn.R0,0L
        )
        context.entityIndex.registerTower(
            TowerRuntimeState(
                TowerIdentity(
                    TowerInstanceId(1),
                    "mage",redPlayer,
                    TeamId.RED
                ),
                TowerUpgradeState(
                    1,TowerPath.TOP
                ),
                TowerGeometryState(
                    Vec3(0.0,0.0,2.0),
                    Vec3(0.0,0.0,2.0),
                    emptyList(),
                    "teardown-body"
                )
            )
        )

        val mobUuid=
            UUID.fromString(
                "00000000-0000-0000-0000-000000007200"
            )
        val projectileUuid=
            UUID.fromString(
                "00000000-0000-0000-0000-000000007201"
            )
        val displayUuid=
            UUID.fromString(
                "00000000-0000-0000-0000-000000007202"
            )
        context.entityIndex
            .projectiles += projectileUuid
        context.entityIndex
            .transientDisplays += displayUuid

        val mob=
            dev.cubecrafttd.mob
                .MobRuntimeState(
                    dev.cubecrafttd.mob
                        .MobIdentity(
                            dev.cubecrafttd.mob
                                .MobInstanceId(20),
                            mobUuid,"zombie",
                            redPlayer,TeamId.BLUE,1
                        ),
                    dev.cubecrafttd.mob
                        .MobRouteState(
                            "blue-route",0,0.0,1.0
                        ),
                    dev.cubecrafttd.mob
                        .MobCombatState(
                            40.0,40.0,
                            dev.cubecrafttd.mob
                                .MobLifecycleState
                                .MOVING
                        )
                )
        context.entityIndex.registerMob(mob)

        val removed=
            linkedSetOf<UUID>()
        val failedEntity=
            projectileUuid
        val restored=
            linkedSetOf<UUID>()

        val teardown=
            ArenaTeardownService(
                bodyService,
                TrackedEntityRemovalPort {
                    uuid ->
                    if(uuid==failedEntity) false
                    else {
                        removed += uuid
                        true
                    }
                },
                MatchPlayerRestorePort {
                    uuid ->
                    if(uuid==bluePlayer) false
                    else {
                        restored += uuid
                        true
                    }
                }
            )

        val stats=MatchStatsRecorder()
        val end=MatchEndCoordinator(
            stats,teardown
        ).finish(
            context,
            MatchOutcome.Winner(
                TeamId.RED,"fixture"
            )
        )

        val redStats=end.stats
            .byPlayer.getValue(redPlayer)
        val blueStats=end.stats
            .byPlayer.getValue(bluePlayer)

        return listOf(
            FixtureResult(
                "teardown-closes-arena-and-core-indexes",
                context.state==
                    ArenaState.CLOSED &&
                    context.entityIndex.isEmpty() &&
                    context.taskGroup.closed &&
                    context.towerBodyLedger
                        .isEmpty()
            ),
            FixtureResult(
                "teardown-restores-tower-body",
                world.blocks.values.all {
                    it.blockData==
                        "minecraft:air"
                }
            ),
            FixtureResult(
                "teardown-reports-paper-entity-residue",
                end.teardown
                    .failedEntityRemovals ==
                    setOf(failedEntity) &&
                    end.teardown
                        .externalResiduePossible
            ),
            FixtureResult(
                "teardown-reports-offline-player-pending",
                end.teardown
                    .pendingPlayerRestores ==
                    setOf(bluePlayer) &&
                    restored==setOf(redPlayer)
            ),
            FixtureResult(
                "match-end-records-win-loss",
                redStats.win==1 &&
                    blueStats.loss==1
            )
        )
    }
}
