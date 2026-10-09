package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.tower.visual.*
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

/** Checks bytes and model bounds; evidence labels are declarations, not visual certification. */
object BukkitTowerBodyCatalogLoader {
    fun load(plugin: JavaPlugin): TowerBodyCatalog {
        val strict = plugin.config.getBoolean("tower-models.require-complete", false)
        val root = plugin.dataFolder.toPath().resolve("tower-models")
        return loadDirectory(root, strict).also {
            plugin.logger.info("Tower body catalog: ${it.loadedCount()} supplied stages; ${it.missingStages().size} placeholder stages; strict=$strict; client fidelity remains unverified")
        }
    }

    fun loadDirectory(root: Path, strict: Boolean): TowerBodyCatalog {
        val manifest = root.resolve("catalog.yml")
        if (!Files.exists(manifest)) return TowerBodyCatalog(emptyList(), strict)
        val catalog = readBounded(manifest)
        require(catalog.get("format-version") == 1) { "Unsupported tower catalog format" }
        val entries = catalog.getMapList("models")
        require(entries.size <= 100) { "Too many model stages" }
        val models = entries.map { entry ->
            fun text(key: String) = entry[key] as? String ?: error("Missing model catalog $key")
            val relative = Path.of(text("file"))
            require(!relative.isAbsolute && relative.nameCount == 1 && relative.toString() !in setOf(".", ".."))
            val file = root.resolve(relative)
            require(file.toRealPath().startsWith(root.toRealPath())) { "Model file escapes catalog directory" }
            val bytes = boundedBytes(file)
            val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            require(text("sha256") == sha) { "Tower model checksum mismatch: $relative" }
            val body = YamlConfiguration()
            body.loadFromString(bytes.toString(Charsets.UTF_8))
            require(body.get("format-version") == 1)
            fun integer(map: Map<*, *>, key: String): Int = map[key] as? Int ?: error("Model $key must be an integer")
            val blocks = body.getMapList("blocks")
            require(blocks.size in 1..1600) { "Invalid model voxel count" }
            val path = TowerPath.valueOf(requireNotNull(body.getString("path")))
            val definition = TowerBodyDefinition(
                towerId = requireNotNull(body.getString("tower-id")),
                level = body.get("level") as? Int ?: error("Model level must be an integer"),
                path = path,
                bodyRevision = "sha256:$sha",
                blocks = blocks.map { block ->
                    val data = block["block-data"] as? String ?: error("Missing block-data")
                    val canonical = Bukkit.createBlockData(data).asString
                    RelativeBodyBlock(BlockPos(integer(block, "x"), integer(block, "y"), integer(block, "z")), canonical)
                },
                evidenceStatus = BodyEvidenceStatus.valueOf(requireNotNull(body.getString("evidence-status"))),
                evidenceRefs = body.getStringList("evidence-refs")
            )
            definition
        }
        return TowerBodyCatalog(models, strict)
    }

    private fun boundedBytes(path: Path): ByteArray {
        require(Files.size(path) in 1..1_048_576) { "Model file must be 1 byte to 1 MiB" }
        return Files.newInputStream(path).use { stream ->
            stream.readNBytes(1_048_577).also { require(it.size <= 1_048_576) }
        }
    }

    private fun readBounded(path: Path) = YamlConfiguration().also {
        it.loadFromString(boundedBytes(path).toString(Charsets.UTF_8))
    }
}
