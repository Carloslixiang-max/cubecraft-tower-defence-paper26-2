package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.admin.EngineeringMapPlacementBounds
import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.map.schematic.*
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin

data class FarmVisualPlacement(val volume: DecodedMapVolume, val origin: BlockPos)

/** Optional, generated visual approximation. The verified track drives all gameplay. */
object ReconstructedFarmAssets {
    private var cachedScene: DecodedMapVolume? = null

    fun enabled(plugin: JavaPlugin): Boolean = plugin.config.getBoolean("farm-scene.enabled", false)

    fun scene(plugin: JavaPlugin): DecodedMapVolume = cachedScene ?: run {
        val bytes = requireNotNull(plugin.getResource("reconstruction/FarmScenery.schem")) {
            "Missing bundled Farm reconstruction"
        }.use { it.readBytes() }
        SpongeSchematicV2Decoder().decode(bytes).also {
            check(it.dimensions == SchematicDimensions(245, 82, 285))
            check(it.offset == SchematicOffset(-60, -3, -45))
            check(it.blockEntities.isEmpty() && it.schematicEntityCount == 0)
            cachedScene = it
        }
    }

    fun shiftedOrigin(origin: BlockPos, volume: DecodedMapVolume): BlockPos = BlockPos(
        Math.addExact(origin.x, volume.offset.x), Math.addExact(origin.y, volume.offset.y),
        Math.addExact(origin.z, volume.offset.z))

    fun validate(plugin: JavaPlugin, world: World, origin: BlockPos) {
        if (enabled(plugin)) {
            val visual = scene(plugin)
            EngineeringMapPlacementBounds.validate(shiftedOrigin(origin, visual), visual.dimensions,
                world.minHeight, world.maxHeight)
        }
    }

    fun reservationBounds(plugin: JavaPlugin, origin: BlockPos): List<Vec3> {
        if (!enabled(plugin)) return emptyList()
        val visual = scene(plugin)
        val p = shiftedOrigin(origin, visual)
        return listOf(Vec3(p.x.toDouble(), p.y.toDouble(), p.z.toDouble()),
            Vec3((p.x + visual.dimensions.width - 1).toDouble(),
                (p.y + visual.dimensions.height - 1).toDouble(),
                (p.z + visual.dimensions.length - 1).toDouble()))
    }

    fun placement(plugin: JavaPlugin, track: DecodedMapVolume, origin: BlockPos): FarmVisualPlacement =
        if (!enabled(plugin)) FarmVisualPlacement(track, origin)
        else scene(plugin).let { FarmVisualPlacement(compose(track, it), shiftedOrigin(origin, it)) }

    /** Keep source blocks exactly and clear structural columns above the playable surfaces. */
    fun compose(track: DecodedMapVolume, scenery: DecodedMapVolume): DecodedMapVolume {
        val w = scenery.dimensions.width
        val h = scenery.dimensions.height
        val l = scenery.dimensions.length
        val tx = -scenery.offset.x
        val ty = -scenery.offset.y
        val tz = -scenery.offset.z
        check(tx >= 0 && ty >= 0 && tz >= 0 && tx + track.dimensions.width <= w &&
            ty + track.dimensions.height <= h && tz + track.dimensions.length <= l)
        val palette = scenery.palette.toMutableMap()
        track.palette.keys.forEach { if (it !in palette) palette[it] = palette.size }
        val inverse = palette.entries.associate { it.value to it.key }
        val ids = scenery.blockIds.copyOf()
        val air = palette.getValue("minecraft:air")
        val top = IntArray(track.dimensions.width * track.dimensions.length) { -1 }
        fun isAir(state: String) = state in setOf("minecraft:air", "minecraft:cave_air", "minecraft:void_air")
        for (y in 0 until track.dimensions.height) for (z in 0 until track.dimensions.length)
            for (x in 0 until track.dimensions.width) {
                if (!isAir(requireNotNull(track.blockAt(x, y, z)))) top[x + z * track.dimensions.width] = y
            }
        // One-block padding protects the edges of lanes/placement spots too.
        for (z in 0 until track.dimensions.length) for (x in 0 until track.dimensions.width) {
            val ceiling = (-1..1).flatMap { dz -> (-1..1).mapNotNull { dx ->
                val nx = x + dx; val nz = z + dz
                if (nx in 0 until track.dimensions.width && nz in 0 until track.dimensions.length)
                    top[nx + nz * track.dimensions.width] else null
            } }.maxOrNull() ?: -1
            if (ceiling >= 0) for (y in 0..minOf(h - ty - 1, ceiling + 18))
                ids[(x + tx) + (z + tz) * w + (y + ty) * w * l] = air
        }
        for (y in 0 until track.dimensions.height) for (z in 0 until track.dimensions.length)
            for (x in 0 until track.dimensions.width) {
                val state = requireNotNull(track.blockAt(x, y, z))
                if (!isAir(state)) ids[(x + tx) + (z + tz) * w + (y + ty) * w * l] = palette.getValue(state)
            }
        return scenery.copy(palette = palette, paletteInverse = inverse, blockIds = ids,
            sha256 = "reconstruction:${scenery.sha256}+track:${track.sha256}",
            blockEntities = track.blockEntities.map { it.copy(x = it.x + tx, y = it.y + ty, z = it.z + tz) })
    }
}
