package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.ArenaTaskHandle
import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.map.schematic.DecodedMapVolume
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.block.BlockState
import org.bukkit.block.data.BlockData
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

private enum class VerifiedResetPhase {
    SCAN,
    ENTITY_CLEANUP,
    APPLY,
    VERIFY,
    COMPLETE,
    FAILED,
    CANCELLED
}

private data class VerifiedResetChange(
    val x: Int,
    val y: Int,
    val z: Int,
    val previousState: BlockState,
    val targetData: BlockData
)

/**
 * Crash-conscious Farm repair/reset.
 *
 * SCAN never mutates blocks and stores snapshots only for blocks that differ
 * from the verified schematic. Once the full volume has been scanned (and its
 * chunks therefore loaded), ENTITY_CLEANUP removes only persistently-tagged TD
 * entities in that verified volume. APPLY changes block differences in bounded
 * batches. VERIFY then scans the entire schematic volume again and also proves
 * that no tagged TD entity survived. Any APPLY or VERIFY failure attempts to
 * restore every changed BlockState snapshot.
 *
 * A persistent reuse/maintenance gate lives outside this service. Callers mark
 * reset-in-progress before scheduling and clear that gate only from onComplete.
 */
class BukkitVerifiedSchematicResetService(
    private val plugin: Plugin,
    private val world: World
) {
    fun resetToVolume(
        volume: DecodedMapVolume,
        origin: BlockPos,
        blocksPerTick: Int = 4_000,
        onProgress:
            (SchematicPasteProgress)->Unit =
            {},
        onComplete:()->Unit = {},
        onFailure:(Throwable)->Unit = {}
    ): ArenaTaskHandle {
        require(blocksPerTick>0)
        check(volume.schematicEntityCount==0)
        check(
            volume.blockEntities.all {
                it.id=="minecraft:beacon"
            }
        )

        val total=
            volume.dimensions.width *
                volume.dimensions.height *
                volume.dimensions.length

        val targetCache=
            hashMapOf<String,BlockData>()
        val changes=
            ArrayList<VerifiedResetChange>()

        var phase=
            VerifiedResetPhase.SCAN
        var cursor=0
        var scanned=0
        var written=0
        var applied=0
        lateinit var task: BukkitTask

        fun local(
            index: Int
        ): Triple<Int,Int,Int> {
            val width=
                volume.dimensions.width
            val length=
                volume.dimensions.length
            val x=index % width
            val z=
                (index / width) %
                    length
            val y=
                index /
                    (width*length)
            return Triple(x,y,z)
        }

        fun target(
            x: Int,
            y: Int,
            z: Int
        ) = BlockPos(
            origin.x+x,
            origin.y+y,
            origin.z+z
        )

        fun targetData(
            x: Int,
            y: Int,
            z: Int
        ): BlockData {
            val state=
                volume.blockAt(
                    x,y,z
                ) ?: error(
                    "Missing palette state at " +
                        x + "," + y + "," + z
                )
            return targetCache
                .getOrPut(state) {
                    Bukkit.createBlockData(
                        state
                    )
                }
        }

        fun report() {
            onProgress(
                SchematicPasteProgress(
                    phase=phase.name,
                    scannedBlocks=scanned,
                    writtenBlocks=written,
                    totalBlocks=total
                )
            )
        }

        fun rollbackApplied():
            Throwable? {
            var first:
                Throwable? = null
            changes
                .take(applied)
                .asReversed()
                .forEach { change ->
                    try {
                        check(
                            change.previousState
                                .update(
                                    true,
                                    false
                                )
                        ) {
                            "BlockState rollback returned false at " +
                                change.x + "," +
                                change.y + "," +
                                change.z
                        }
                    } catch(t:Throwable) {
                        if(first==null) {
                            first=t
                        } else {
                            first!!.addSuppressed(t)
                        }
                    }
                }
            return first
        }

        task=
            plugin.server.scheduler
                .runTaskTimer(
                    plugin,
                    Runnable {
                        try {
                            var budget=
                                blocksPerTick
                            while(
                                budget-->0 &&
                                phase !=
                                    VerifiedResetPhase
                                        .COMPLETE
                            ) {
                                when(phase) {
                                    VerifiedResetPhase
                                        .SCAN -> {
                                        if(
                                            cursor>=total
                                        ) {
                                            phase=
                                                VerifiedResetPhase
                                                    .ENTITY_CLEANUP
                                            cursor=0
                                            report()
                                            continue
                                        }

                                        val (x,y,z)=
                                            local(
                                                cursor++
                                            )
                                        val p=
                                            target(
                                                x,y,z
                                            )
                                        val block=
                                            world.getBlockAt(
                                                p.x,
                                                p.y,
                                                p.z
                                            )
                                        val expected=
                                            targetData(
                                                x,y,z
                                            )
                                        scanned++
                                        if(
                                            block.blockData
                                                .asString !=
                                            expected
                                                .asString
                                        ) {
                                            changes +=
                                                VerifiedResetChange(
                                                    p.x,
                                                    p.y,
                                                    p.z,
                                                    block.state,
                                                    expected
                                                        .clone()
                                                )
                                        }
                                    }

                                    VerifiedResetPhase
                                        .ENTITY_CLEANUP -> {
                                        val dimensions=
                                            volume.dimensions
                                        BukkitTrackedEntityTag
                                            .taggedInVolume(
                                                world,
                                                origin,
                                                dimensions.width,
                                                dimensions.height,
                                                dimensions.length
                                            )
                                            .forEach {
                                                it.remove()
                                            }

                                        val survivors=
                                            BukkitTrackedEntityTag
                                                .taggedInVolume(
                                                    world,
                                                    origin,
                                                    dimensions.width,
                                                    dimensions.height,
                                                    dimensions.length
                                                )
                                        check(
                                            survivors.isEmpty()
                                        ) {
                                            "Verified Farm reset could not remove tagged TD entities: " +
                                                survivors.joinToString {
                                                    it.uniqueId
                                                        .toString()
                                                }
                                        }

                                        phase=
                                            VerifiedResetPhase
                                                .APPLY
                                        cursor=0
                                        report()
                                        continue
                                    }

                                    VerifiedResetPhase
                                        .APPLY -> {
                                        if(
                                            cursor>=
                                                changes.size
                                        ) {
                                            phase=
                                                VerifiedResetPhase
                                                    .VERIFY
                                            cursor=0
                                            scanned=0
                                            report()
                                            continue
                                        }

                                        val change=
                                            changes[
                                                cursor++
                                            ]
                                        world.getBlockAt(
                                            change.x,
                                            change.y,
                                            change.z
                                        ).setBlockData(
                                            change.targetData
                                                .clone(),
                                            false
                                        )
                                        applied=cursor
                                        written=applied
                                    }

                                    VerifiedResetPhase
                                        .VERIFY -> {
                                        if(
                                            cursor>=total
                                        ) {
                                            val dimensions=
                                                volume.dimensions
                                            val survivors=
                                                BukkitTrackedEntityTag
                                                    .taggedInVolume(
                                                        world,
                                                        origin,
                                                        dimensions.width,
                                                        dimensions.height,
                                                        dimensions.length
                                                    )
                                            check(
                                                survivors.isEmpty()
                                            ) {
                                                "Verified Farm reset finished block verification but tagged TD entities remain: " +
                                                    survivors.joinToString {
                                                        it.uniqueId
                                                            .toString()
                                                    }
                                            }

                                            phase=
                                                VerifiedResetPhase
                                                    .COMPLETE
                                            report()
                                            task.cancel()
                                            onComplete()
                                            continue
                                        }

                                        val (x,y,z)=
                                            local(
                                                cursor++
                                            )
                                        val p=
                                            target(
                                                x,y,z
                                            )
                                        val expected=
                                            targetData(
                                                x,y,z
                                            )
                                        val current=
                                            world.getBlockAt(
                                                p.x,
                                                p.y,
                                                p.z
                                            ).blockData
                                        scanned++
                                        check(
                                            current.asString==
                                                expected.asString
                                        ) {
                                            "Verified Farm reset mismatch at " +
                                                p +
                                                ": current=" +
                                                current.asString +
                                                " expected=" +
                                                expected.asString
                                        }
                                    }

                                    else -> Unit
                                }
                            }
                            report()
                        } catch(t:Throwable) {
                            val rollbackFailure=
                                if(
                                    applied>0 &&
                                    phase !=
                                        VerifiedResetPhase
                                            .COMPLETE
                                )
                                    rollbackApplied()
                                else
                                    null
                            rollbackFailure
                                ?.let(
                                    t::addSuppressed
                                )
                            phase=
                                VerifiedResetPhase
                                    .FAILED
                            task.cancel()
                            report()
                            onFailure(t)
                        }
                    },
                    1L,
                    1L
                )

        return object:
            ArenaTaskHandle {
            override val isCancelled:
                Boolean
                get()=task.isCancelled

            override fun cancel() {
                if(
                    !task.isCancelled
                ) {
                    task.cancel()
                }
                if(
                    phase==
                        VerifiedResetPhase.APPLY ||
                    phase==
                        VerifiedResetPhase.VERIFY
                ) {
                    rollbackApplied()
                }
                phase=
                    VerifiedResetPhase
                        .CANCELLED
                report()
            }
        }
    }
}
