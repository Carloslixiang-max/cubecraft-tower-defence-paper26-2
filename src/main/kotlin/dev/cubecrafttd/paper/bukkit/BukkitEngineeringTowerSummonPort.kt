package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.tower.*
import dev.cubecrafttd.map.Vec3
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.*
import org.bukkit.event.entity.CreatureSpawnEvent
import java.util.UUID

/**
 * Stage-4 runtime placeholder only.
 *
 * Child combat is represented by core effective stage stats. These entities
 * are staged above the supplied body and follow upgrades; unrecovered Mature child visuals/AI
 * are not claimed to be original.
 */
class BukkitEngineeringTowerSummonPort(
    private val world: World,
    private val visualOrigin: (TowerRuntimeState) -> Vec3
) : TowerSummonSpawnPort {
    override fun spawn(
        command: TowerSummonSpawnCommand
    ): UUID {
        val location = location(command)

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

    override fun update(command: TowerSummonSpawnCommand, liveEntityUuid: UUID) {
        val entity = world.getEntity(liveEntityUuid)
            ?: error("Tower summon is not live: $liveEntityUuid")
        val destination = location(command)
        val current = entity.location
        if (current.x != destination.x || current.y != destination.y || current.z != destination.z) {
            destination.yaw = current.yaw
            destination.pitch = current.pitch
            check(entity.teleport(destination)) { "Tower summon projection failed: $liveEntityUuid" }
        }
    }

    private fun location(command: TowerSummonSpawnCommand): Location {
        // Inferred staging above the current supplied body, not historical child
        // movement. The old base-corner position buried children inside blocks.
        val p = visualOrigin(command.tower)
        val grouped = command.request.kind == SummonKind.SORCERER_ANIMAL
        val dx = if (!grouped) 0.0 else if (command.ordinal % 2 == 0) -0.65 else 0.65
        val dz = if (!grouped) 0.0 else if (command.ordinal / 2 == 0) -0.65 else 0.65
        // Shulkers snap to block-aligned feet in native spawn/teleport. Use the
        // first air block above the roof so update does not fight that snapping.
        val y=if(command.request.kind==SummonKind.SHULKER) kotlin.math.floor(p.y) else p.y
        return Location(world, p.x + dx, y, p.z + dz)
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
            // Combat remains core-owned; vanilla physics, equipment pickup and
            // survival damage must not remove or move these visual children.
            spawned.setGravity(false)
            spawned.isInvulnerable = true
            spawned.isCollidable = false
            spawned.setCanPickupItems(false)
            if (spawned is ArmorStand) {
                listOf(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND, EquipmentSlot.HEAD,
                    EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET).forEach { slot ->
                    spawned.addEquipmentLock(slot, ArmorStand.LockType.ADDING_OR_CHANGING)
                    spawned.addEquipmentLock(slot, ArmorStand.LockType.REMOVING_OR_CHANGING)
                }
            }
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
