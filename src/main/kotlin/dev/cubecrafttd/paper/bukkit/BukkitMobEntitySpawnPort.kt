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
                .formId
        val p=projectPosition(request.spawnPosition)
        val location=
            Location(
                world,p.x,p.y,p.z
            )

        val entity: LivingEntity =
            when(form) {
                "zombie" ->
                    spawn(location,Zombie::class.java)
                "spider" ->
                    spawn(location,Spider::class.java)
                "cave_spider" ->
                    spawn(
                        location,
                        CaveSpider::class.java
                    )
                "pigman" ->
                    spawn(
                        location,
                        PigZombie::class.java
                    )
                "skeleton" ->
                    spawn(
                        location,
                        Skeleton::class.java
                    )
                "wither_skeleton" ->
                    spawn(
                        location,
                        WitherSkeleton::class.java
                    )
                "creeper" ->
                    spawn(
                        location,
                        Creeper::class.java
                    )
                "silverfish" ->
                    spawn(
                        location,
                        Silverfish::class.java
                    )
                "endermite" ->
                    spawn(
                        location,
                        Endermite::class.java
                    )
                "witch" ->
                    spawn(
                        location,
                        Witch::class.java
                    )
                "slime" ->
                    spawn(
                        location,
                        Slime::class.java
                    )
                "magma_cube" ->
                    spawn(
                        location,
                        MagmaCube::class.java
                    )
                "blaze" ->
                    spawn(
                        location,
                        Blaze::class.java
                    )
                "giant" ->
                    spawn(
                        location,
                        Giant::class.java
                    )
                "wither" ->
                    spawn(
                        location,
                        Wither::class.java
                    )
                else ->
                    error(
                        "No Paper living-entity class mapping for form $form"
                    )
            }

        return entity.uniqueId
    }

    private fun <T:LivingEntity> spawn(
        location: Location,
        clazz: Class<T>
    ): T =
        world.spawn(
            location,
            clazz,
            CreatureSpawnEvent
                .SpawnReason.CUSTOM
        ) { spawned ->
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
