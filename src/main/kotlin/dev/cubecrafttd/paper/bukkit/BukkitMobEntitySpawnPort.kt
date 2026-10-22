package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.mob.RecommendedMatureMobFormResolver
import dev.cubecrafttd.troop.*
import dev.cubecrafttd.map.Vec3
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.*
import org.bukkit.event.entity.CreatureSpawnEvent
import java.util.UUID

class BukkitMobEntitySpawnPort(
    private val world: World,
    private val projectPosition: (Vec3) -> Vec3 = { it }
) : MobEntitySpawnPort {
    override fun spawn(
        request: MobEntitySpawnRequest
    ): UUID {
        val form =
            RecommendedMatureMobFormResolver
                .initialForm(
                    request.mobId,
                    request.level
                )
        val p=projectPosition(request.spawnPosition)
        val location=
            Location(
                world,p.x,p.y,p.z
            )

        val clazz: Class<out LivingEntity> = when(form.formId) {
            "zombie" -> Zombie::class.java
            "spider" -> Spider::class.java
            "cave_spider" -> CaveSpider::class.java
            "pigman" -> PigZombie::class.java
            "skeleton" -> Skeleton::class.java
            "wither_skeleton" -> WitherSkeleton::class.java
            "creeper" -> Creeper::class.java
            "silverfish" -> Silverfish::class.java
            "endermite" -> Endermite::class.java
            "witch" -> Witch::class.java
            "slime" -> Slime::class.java
            "magma_cube" -> MagmaCube::class.java
            "blaze" -> Blaze::class.java
            "giant" -> Giant::class.java
            "wither" -> Wither::class.java
            else -> error("No Paper living-entity class mapping for form ${form.formId}")
        }
        val entity = spawn(location, clazz) {
            BukkitMobAppearanceProjection.initialize(it, request.mobId, request.level, form)
        }

        return entity.uniqueId
    }

    private fun <T:LivingEntity> spawn(
        location: Location,
        clazz: Class<T>,
        initialize: (T) -> Unit
    ): T =
        world.spawn(
            location,
            clazz,
            CreatureSpawnEvent
                .SpawnReason.CUSTOM
        ) { spawned ->
            initialize(spawned)
            // Core owns vertical movement, including while paused at a castle.
            spawned.setGravity(false)
            BukkitTrackedEntityTag.mark(
                spawned
            )
            spawned.setRemoveWhenFarAway(
                false
            )
            spawned.setCollidable(
                false
            )
            spawned.setCanPickupItems(
                false
            )
            (spawned as? Mob)
                ?.setAI(false)
        }
}
