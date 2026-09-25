package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.ArenaTaskHandle
import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.map.schematic.DecodedMapVolume
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

private enum class PastePhase { PREFLIGHT, PASTE, COMPLETE, FAILED, CANCELLED }

data class SchematicPasteProgress(
    val phase: String,
    val scannedBlocks: Int,
    val writtenBlocks: Int,
    val totalBlocks: Int
)

class BukkitSchematicPasteService(
    private val plugin: Plugin,
    private val world: World
) {
    /**
     * Initial production-safe policy: destination must be completely empty.
     * This intentionally refuses overwrite mode, so a failed paste can roll
     * back all written blocks to air without needing a huge world snapshot.
     */
    fun pasteIntoEmptyRegion(
        volume: DecodedMapVolume,
        origin: BlockPos,
        blocksPerTick: Int = 4_000,
        onProgress: (SchematicPasteProgress)->Unit = {},
        onComplete: ()->Unit = {},
        onFailure: (Throwable)->Unit = {}
    ): ArenaTaskHandle {
        require(blocksPerTick>0)
        check(volume.schematicEntityCount==0)
        check(volume.blockEntities.all { it.id=="minecraft:beacon" })

        val total=volume.dimensions.width *
            volume.dimensions.height *
            volume.dimensions.length
        val blockDataCache=hashMapOf<String,org.bukkit.block.data.BlockData>()
        var phase=PastePhase.PREFLIGHT
        var cursor=0
        var scanned=0
        var written=0
        val writtenPositions=ArrayList<BlockPos>()
        lateinit var task:BukkitTask

        fun local(index:Int):Triple<Int,Int,Int> {
            val w=volume.dimensions.width
            val l=volume.dimensions.length
            val x=index % w
            val z=(index / w) % l
            val y=index / (w*l)
            return Triple(x,y,z)
        }
        fun target(x:Int,y:Int,z:Int)=BlockPos(
            origin.x+x,origin.y+y,origin.z+z
        )
        fun report()=onProgress(
            SchematicPasteProgress(
                phase.name,scanned,written,total
            )
        )
        fun rollbackWritten() {
            writtenPositions.asReversed().forEach { p ->
                world.getBlockAt(p.x,p.y,p.z)
                    .setType(org.bukkit.Material.AIR,false)
            }
        }

        task=plugin.server.scheduler.runTaskTimer(
            plugin,
            Runnable {
                try {
                    var budget=blocksPerTick
                    while(budget-->0 && phase!=PastePhase.COMPLETE) {
                        when(phase) {
                            PastePhase.PREFLIGHT -> {
                                if(cursor>=total) {
                                    phase=PastePhase.PASTE
                                    cursor=0
                                    report()
                                    continue
                                }
                                val (x,y,z)=local(cursor++)
                                val p=target(x,y,z)
                                scanned++
                                check(world.getBlockAt(p.x,p.y,p.z).type.isAir) {
                                    "Destination is not empty at $p; paste aborted before mutation"
                                }
                            }
                            PastePhase.PASTE -> {
                                if(cursor>=total) {
                                    phase=PastePhase.COMPLETE
                                    report()
                                    task.cancel()
                                    onComplete()
                                    continue
                                }
                                val (x,y,z)=local(cursor++)
                                val state=volume.blockAt(x,y,z)
                                    ?: error("Missing palette state at $x,$y,$z")
                                if(state!="minecraft:air") {
                                    val p=target(x,y,z)
                                    val data=blockDataCache.getOrPut(state) {
                                        Bukkit.createBlockData(state)
                                    }
                                    world.getBlockAt(p.x,p.y,p.z)
                                        .setBlockData(data,false)
                                    writtenPositions += p
                                    written++
                                }
                            }
                            else -> Unit
                        }
                    }
                    report()
                } catch(t:Throwable) {
                    if(phase==PastePhase.PASTE) {
                        rollbackWritten()
                    }
                    phase=PastePhase.FAILED
                    task.cancel()
                    onFailure(t)
                }
            },
            1L,1L
        )

        return object:ArenaTaskHandle {
            override val isCancelled:Boolean
                get()=task.isCancelled
            override fun cancel() {
                if(!task.isCancelled) task.cancel()
                if(phase==PastePhase.PASTE) rollbackWritten()
                phase=PastePhase.CANCELLED
            }
        }
    }
}
