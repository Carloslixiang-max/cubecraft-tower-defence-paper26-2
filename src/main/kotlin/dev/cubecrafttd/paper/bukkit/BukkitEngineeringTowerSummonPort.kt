package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.tower.*
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.*
import org.bukkit.event.entity.CreatureSpawnEvent
import java.util.UUID

/**
 * Stage-4 runtime placeholder only.
 *
 * Child combat is represented by core effective stage stats. These entities
 * validate spawn/remove lifecycle only; unrecovered Mature child visuals/AI
 * are not claimed to be original.
 */
class BukkitEngineeringTowerSummonPort(
    private val world: World
) : TowerSummonSpawnPort {
    override fun spawn(
        command: TowerSummonSpawnCommand
    ): UUID {
        val p=
            command.tower.geometry
                .rangeOrigin
        val location=
            Location(
                world,
                p.x +
                    (command.ordinal % 2) *
                    0.35,
                p.y,
                p.z +
                    (command.ordinal / 2) *
                    0.35
            )

        val entity: LivingEntity =
            when(command.request.kind) {
                SummonKind.IRON_GOLEM ->
                    spawn(
                        location,
                        IronGolem::class.java
                    )
                SummonKind.SHULKER ->
                    spawn(
                        location,
                        Shulker::class.java
                    )
                SummonKind.SNOWMAN ->
                    spawn(
                        location,
                        Snowman::class.java
                    )

                // Engineering visual placeholders only.
                SummonKind.SORCERER_ANIMAL ->
                    spawn(
                        location,
                        Cow::class.java
                    )
                SummonKind.SORCERER_KAMIKAZE ->
                    spawn(
                        location,
                        Sheep::class.java
                    )
                SummonKind.BABY_ZEUS ->
                    spawn(
                        location,
                        ArmorStand::class.java
                    ).apply {
                        setGravity(false)
                        isInvulnerable=true
                    }
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
            BukkitTrackedEntityTag.mark(
                spawned
            )
            spawned.setRemoveWhenFarAway(
                false
            )
            (spawned as? Mob)
                ?.setAI(false)
        }
}
