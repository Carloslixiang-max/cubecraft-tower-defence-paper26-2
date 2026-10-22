package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.testing.TestingMapFactory
import dev.cubecrafttd.troop.MobEntitySpawnRequest
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.AbstractCubeMob
import org.bukkit.entity.Zombie
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

/** Real Paper assertions, opt-in CI-owned dimension only. */
object BukkitMobAppearanceSmoke {
    fun run(plugin: JavaPlugin, world: World) {
        val context=ArenaContext(ArenaId("appearance-smoke"), world.uid, TestingMapFactory.minimal())
        val spawned=mutableListOf<UUID>()
        val position=Vec3(world.spawnLocation.blockX + 8.5, world.maxHeight - 12.0,
            world.spawnLocation.blockZ + 8.5)
        fun spawn(id: String, level: Int): LivingEntity {
            val uuid=BukkitMobEntitySpawnPort(world).spawn(MobEntitySpawnRequest(
                MobInstanceId(122000L + spawned.size), UUID(0, 122000), TeamId.RED,
                id, level, "appearance-smoke", position))
            spawned += uuid
            return plugin.server.getEntity(uuid) as LivingEntity
        }
        try {
            val helmets=listOf(Material.AIR, Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET,
                Material.IRON_HELMET, Material.DIAMOND_HELMET)
            helmets.forEachIndexed { index, helmet ->
                val zombie=spawn("zombie", index + 1) as Zombie
                check(!zombie.isBaby && !zombie.canPickupItems && !zombie.hasAI())
                val equipment=zombie.equipment!!
                check((equipment.helmet?.type ?: Material.AIR) == helmet)
                check(equipment.armorContents.take(3).all { it == null || it.type.isAir })
                check(equipment.itemInMainHand.type.isAir && equipment.itemInOffHand.type.isAir)
                check(equipment.helmetDropChance == 0.0f)
                if (helmet != Material.AIR) {
                    val modifiers=equipment.helmet!!.itemMeta.attributeModifiers!!
                    check(modifiers.size()==1 && modifiers.values().all { it.amount==0.0 })
                    val roundtrip=org.bukkit.inventory.ItemStack.deserializeBytes(equipment.helmet!!.serializeAsBytes())
                    check(roundtrip.itemMeta.attributeModifiers==modifiers)
                }
            }
            // Paper 26.2 Slime and MagmaCube share AbstractCubeMob, not Slime.
            for (level in listOf(1, 4)) {
                val entity=spawn("slime", level) as AbstractCubeMob
                check(entity.size == 4)
                val attributes=listOf(Attribute.MAX_HEALTH, Attribute.MOVEMENT_SPEED, Attribute.ATTACK_DAMAGE, Attribute.ARMOR)
                    .map { entity.getAttribute(it)!! }
                attributes.zip(listOf(37.0, 0.31, 7.0, 13.0)).forEach { (attribute, value) -> attribute.baseValue=value }
                entity.health=11.0
                val values=attributes.map { it.baseValue }
                val mob=MobRuntimeState(MobIdentity(MobInstanceId(122500L + level), entity.uniqueId,
                    "slime", UUID(0, 122000), TeamId.RED, level),
                    MobRouteState("appearance-smoke", 0, 0.0, 0.0),
                    MobCombatState(100.0, 100.0, MobLifecycleState.MOVING))
                context.entityIndex.registerMob(mob)
                for (expectedSize in listOf(2, 1)) {
                    val result=SlimeShrinkPhaseService.shrinkAfterLethalHit(mob.form, mob.combat.maxHealth, 2)
                    mob.form=result.nextForm
                    mob.combat.maxHealth=result.nextMaxHealth
                    mob.combat.health=result.nextHealth
                    val coreBefore=mob.copy(combat=mob.combat.copy())
                    BukkitMobAppearanceProjection.project(plugin.server, context)
                    check(entity.size==expectedSize && plugin.server.getEntity(entity.uniqueId)===entity)
                    check(entity.health==11.0 && attributes.map { it.baseValue }==values)
                    check(mob==coreBefore) { "Appearance mutated core health or identity" }
                    // Repeated projection must not call setSize and heal it.
                    BukkitMobAppearanceProjection.project(plugin.server, context)
                    check(entity.health==11.0)
                }
                mob.combat.lifecycle=MobLifecycleState.DEAD
                mob.form=mob.form.copy(phaseIndex=0)
                BukkitMobAppearanceProjection.project(plugin.server, context)
                check(entity.size==1) { "Dead troop was projected again" }
                context.entityIndex.unregisterMob(entity.uniqueId)
            }
        } finally {
            spawned.forEach { plugin.server.getEntity(it)?.remove() }
        }
        check(spawned.none { plugin.server.getEntity(it)!=null })
        plugin.logger.info("Mob appearance PASS: five Zombie helmet tiers, Slime/Magma shrink on same UUID, health/attributes preserved, dead skip and zero residue")
    }
}
