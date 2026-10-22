package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.map.BlockPos
import dev.cubecrafttd.testing.TestingMapFactory
import dev.cubecrafttd.tower.lifecycle.TowerBodyMutationService
import dev.cubecrafttd.tower.visual.QuarterTurn
import dev.cubecrafttd.tower.visual.RotationCache
import dev.cubecrafttd.map.Vec3
import dev.cubecrafttd.tower.*
import dev.cubecrafttd.tower.visual.TowerPath
import org.bukkit.World
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.entity.Snowman
import org.bukkit.Location
import org.bukkit.event.block.EntityBlockFormEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

/** Production port and real 82-stage catalog; restricted to the owned smoke world. */
object BukkitTowerSummonSmoke {
    fun run(plugin: JavaPlugin, world: World) {
        val models=BukkitTowerBodyCatalogLoader.loadBundled(plugin, true)
        val origin=BukkitTowerVisualOrigins(models)
        val port=BukkitEngineeringTowerSummonPort(world, origin::origin)
        val base=Vec3(world.spawnLocation.blockX + 16.0, world.maxHeight - 24.0,
            world.spawnLocation.blockZ + 16.0)
        val tower=TowerRuntimeState(TowerIdentity(TowerInstanceId(122900), "necromancer",
            UUID(0, 122900), TeamId.RED), TowerUpgradeState(1, TowerPath.TOP),
            TowerGeometryState(base, base, listOf(base), "smoke"))
        val children=mutableListOf<UUID>()
        try {
            SummonKind.entries.forEach { kind ->
                tower.upgrade.level=1
                val command=TowerSummonSpawnCommand(tower, SummonRequest(kind, 122900,
                    tower.identity.ownerUuid, null, 1), 0)
                val uuid=port.spawn(command)
                children += uuid
                val entity=plugin.server.getEntity(uuid) as LivingEntity
                check(!entity.hasGravity() && entity.isInvulnerable && !entity.isCollidable && !entity.canPickupItems) {
                    "$kind flags: gravity=${entity.hasGravity()} invulnerable=${entity.isInvulnerable} collidable=${entity.isCollidable} pickup=${entity.canPickupItems}"
                }
                check(BukkitTrackedEntityTag.TAG in entity.scoreboardTags) { "$kind missing ownership marker" }
                check(entity !is Mob || !entity.hasAI()) { "$kind vanilla AI enabled" }
                fun expectedY()=origin.origin(tower).y.let { if(kind==SummonKind.SHULKER) kotlin.math.floor(it) else it }
                check(entity.location.y==expectedY() && entity.location.y>base.y) {
                    "$kind spawn Y=${entity.location.y}, expected=${expectedY()}"
                }
                if (entity is Snowman) {
                    val block=world.getBlockAt(base.x.toInt(),base.y.toInt()-1,base.z.toInt())
                    val form=EntityBlockFormEvent(entity,block,block.state)
                    val change=EntityChangeBlockEvent(entity,block,org.bukkit.Material.SNOW.createBlockData())
                    plugin.server.pluginManager.callEvent(form)
                    plugin.server.pluginManager.callEvent(change)
                    check(form.isCancelled && change.isCancelled) { "TD child can mutate map terrain" }
                    // The same native event classes from an unmarked mob must
                    // retain their prior permissions in a shared server world.
                    val foreign=world.spawn(Location(world,base.x+6,base.y,base.z),Snowman::class.java)
                    try {
                        val outsideForm=EntityBlockFormEvent(foreign,block,block.state)
                        val outsideChange=EntityChangeBlockEvent(foreign,block,org.bukkit.Material.SNOW.createBlockData())
                        plugin.server.pluginManager.callEvent(outsideForm)
                        plugin.server.pluginManager.callEvent(outsideChange)
                        check(!outsideForm.isCancelled && !outsideChange.isCancelled)
                    } finally { foreign.remove() }
                }
                if (entity is ArmorStand) {
                    check(entity.hasEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.ADDING_OR_CHANGING)) { "Baby stand permits new equipment" }
                    check(entity.hasEquipmentLock(EquipmentSlot.HEAD, ArmorStand.LockType.REMOVING_OR_CHANGING)) { "Baby stand permits removing equipment" }
                }
                // Existing UUID follows upgraded body height instead of staying
                // at the old point inside the new roof. No additional entities.
                tower.upgrade.level=3
                port.update(command, uuid)
                check(plugin.server.getEntity(uuid)===entity) { "$kind UUID changed on update" }
                check(entity.location.y==expectedY()) { "$kind upgrade Y=${entity.location.y}, expected=${expectedY()}" }
            }
        } finally {
            children.forEach { plugin.server.getEntity(it)?.remove() }
        }
        check(children.none { plugin.server.getEntity(it)!=null }) { "Direct child projection smoke left residue" }
        val context=ArenaContext(ArenaId("summon-smoke"), world.uid, TestingMapFactory.minimal())
        context.state=ArenaState.RUNNING
        val mutations=TowerBodyMutationService(BukkitBlockWorldAdapter(world), context.towerBodyLedger,
            RotationCache(BukkitTowerBlockRotation::rotate))
        val adapter=BukkitEntityRuntimeAdapter(plugin.server)
        val phase=TowerSummonMaintenancePhase(
            RecommendedTowerSummonCountResolver.withExplicitSorcererCounts(mapOf(
                "sorcerer.none.l1" to 1, "sorcerer.none.l2" to 1, "sorcerer.bottom.l3" to 2)),
            port, TowerSummonRemovalPort(adapter::remove))
        val towers=listOf("necromancer", "zeus", "sorcerer").mapIndexed { index, id ->
            tower.copy(identity=tower.identity.copy(instanceId=TowerInstanceId(122910L + index), towerId=id),
                upgrade=TowerUpgradeState(if (id=="sorcerer") 4 else 1, TowerPath.BOTTOM),
                geometry=tower.geometry.copy(baseOrigin=base.copy(x=base.x + index * 12.0)))
        }
        val teardown=ArenaTeardownService(mutations, adapter, MatchPlayerRestorePort { true }, adapter)
        try {
            towers.forEach { value ->
                val p=value.geometry.baseOrigin
                mutations.place(value.identity.instanceId.value, BlockPos(p.x.toInt(),p.y.toInt(),p.z.toInt()),
                    models.definition(value.identity.towerId,value.upgrade.level,TowerPath.BOTTOM), QuarterTurn.R0,0)
                context.entityIndex.registerTower(value)
            }
            phase.tick(context)
            check(context.entityIndex.towerSummons.size==5) { "Unexpected base Zeus child or missing summons" }
            // Actual Necromancer upgrade changes kind despite retaining count 1.
            towers[0].upgrade.level=3
            towers[0].upgrade.path=TowerPath.TOP
            mutations.upgrade(towers[0].identity.instanceId.value,
                BlockPos(base.x.toInt(),base.y.toInt(),base.z.toInt()),
                models.definition("necromancer",3,TowerPath.TOP),QuarterTurn.R0,1)
            phase.tick(context)
            check(context.entityIndex.towerSummons.mapNotNull(plugin.server::getEntity)
                .count { it is org.bukkit.entity.Shulker }==1) { "Necromancer did not replace Golem with Shulker" }
            // Baby branch adds exactly one tracked child.
            towers[1].upgrade.level=3
            val zeusBase=towers[1].geometry.baseOrigin
            mutations.upgrade(towers[1].identity.instanceId.value,
                BlockPos(zeusBase.x.toInt(),zeusBase.y.toInt(),zeusBase.z.toInt()),
                models.definition("zeus",3,TowerPath.BOTTOM),QuarterTurn.R0,2)
            phase.tick(context)
            check(context.entityIndex.towerSummons.size==6) { "Baby Zeus upgrade ownership count != 6" }
            context.entityIndex.unregisterTower(towers[2].identity.instanceId)
            mutations.remove(towers[2].identity.instanceId.value)
            phase.tick(context)
            check(context.entityIndex.towerSummons.size==2) { "Sorcerer sale did not remove four children" }
            val remaining=context.entityIndex.towerSummons.toSet()
            val report=teardown.teardown(context)
            check(report.fullyCleanNow && remaining.none(adapter::exists) && context.entityIndex.isEmpty()) { "Summon teardown unclean: $report" }
        } finally {
            if(context.state!=ArenaState.CLOSED) teardown.teardown(context)
        }
        plugin.logger.info("Tower summon projection PASS: six kinds above model body, upgrade follows same UUID, no vanilla physics/pickup/terrain mutation, locked stand equipment and zero residue; real summon/upgrade/sell/end teardown checked")
    }
}
