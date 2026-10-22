package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.ArenaContext
import dev.cubecrafttd.mob.MobFormState
import dev.cubecrafttd.mob.MobLifecycleState
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.Server
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Slime
import org.bukkit.entity.Zombie
import org.bukkit.inventory.ItemStack

/** Paper-only appearance. Historical evidence and approximation limits are in evidence/MOB_VISUALS.md. */
object BukkitMobAppearanceProjection {
    private val visualHelmetKey=NamespacedKey("cubecrafttd", "visual_helmet")
    fun initialize(entity: LivingEntity, mobId: String, level: Int, form: MobFormState) {
        if (mobId == "zombie" && entity is Zombie) {
            entity.isBaby = false
            entity.equipment?.let { equipment ->
                // Natural spawn gear is random; only the recovered tier helmet
                // belongs on this troop. No armor values enter core combat.
                equipment.clear()
                val helmet = when (level) {
                    2 -> Material.LEATHER_HELMET
                    3 -> Material.CHAINMAIL_HELMET
                    4 -> Material.IRON_HELMET
                    5 -> Material.DIAMOND_HELMET
                    else -> Material.AIR
                }
                equipment.setHelmet(ItemStack(helmet).apply {
                    if (helmet != Material.AIR) editMeta {
                        it.isUnbreakable = true
                        // Explicit modifiers replace the material's armor stats.
                        // Cosmetic helmets must not reduce event.finalDamage
                        // forwarded to the authoritative player weapon runtime.
                        it.addAttributeModifier(Attribute.ARMOR, AttributeModifier(visualHelmetKey,
                            0.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD))
                    }
                })
                equipment.helmetDropChance = 0.0f
            }
        }
        projectForm(entity, form)
    }

    fun project(server: Server, context: ArenaContext) {
        // Arena-local discovery only. No world scans, child entities or cache
        // that could survive teardown. Removed/dead troops are never respawned.
        context.entityIndex.mobsByUuid.values.forEach { mob ->
            if (mob.form.formId !in setOf("slime", "magma_cube") ||
                mob.combat.lifecycle in setOf(MobLifecycleState.DEAD, MobLifecycleState.REMOVED)) return@forEach
            val entity = server.getEntity(mob.identity.entityUuid) as? LivingEntity ?: return@forEach
            if (entity.isValid && !entity.isDead) projectForm(entity, mob.form)
        }
    }

    internal fun projectForm(entity: LivingEntity, form: MobFormState) {
        if (entity !is Slime || form.formId !in setOf("slime", "magma_cube")) return
        // The 2021 shrink mechanic is direct evidence. Exact original sizes are
        // unknown: 4 -> 2 -> 1 is an explicit visual approximation, not a new
        // phase cap or balance rule. Further configured phases remain size 1.
        val size = when (form.phaseIndex) { 0 -> 4; 1 -> 2; else -> 1 }
        if (entity.size == size) return
        // Bukkit setSize resets health and three attributes, even when used for
        // appearance. Preserve them so projection cannot heal or rebalance it.
        val attributes = listOf(Attribute.MAX_HEALTH, Attribute.MOVEMENT_SPEED, Attribute.ATTACK_DAMAGE)
            .mapNotNull { entity.getAttribute(it) }.associateWith { it.baseValue }
        val health = entity.health
        entity.size = size
        attributes.forEach { (attribute, value) -> attribute.baseValue = value }
        entity.health = health
    }
}
