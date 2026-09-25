package dev.cubecrafttd.arena

import dev.cubecrafttd.mob.MobRuntimeState
import dev.cubecrafttd.tower.TowerInstanceId
import dev.cubecrafttd.tower.TowerRuntimeState
import dev.cubecrafttd.castle.GuardRuntime
import java.util.UUID

/**
 * Authoritative arena-local indexes.
 * Runtime target discovery starts here, never from Bukkit.getWorlds()/world.entities scans.
 */
class ArenaEntityIndex {
    val mobsByUuid: MutableMap<UUID, MobRuntimeState> = linkedMapOf()
    val mobsByDefendingTeam: MutableMap<TeamId, MutableSet<UUID>> = mutableMapOf(
        TeamId.RED to linkedSetOf(), TeamId.BLUE to linkedSetOf()
    )
    val mobsByRoute: MutableMap<String, MutableSet<UUID>> = linkedMapOf()

    val towersByInstanceId: MutableMap<TowerInstanceId, TowerRuntimeState> = linkedMapOf()
    val towersByTeam: MutableMap<TeamId, MutableSet<TowerInstanceId>> = mutableMapOf(
        TeamId.RED to linkedSetOf(), TeamId.BLUE to linkedSetOf()
    )

    val guardsByUuid: MutableMap<UUID, GuardRuntime> = linkedMapOf()
    val guardsByTeam: MutableMap<TeamId, MutableSet<UUID>> = mutableMapOf(
        TeamId.RED to linkedSetOf(), TeamId.BLUE to linkedSetOf()
    )
    val transientDisplays: MutableSet<UUID> = linkedSetOf()
    val projectiles: MutableSet<UUID> = linkedSetOf()

    fun registerMob(mob: MobRuntimeState) {
        val uuid = mob.identity.entityUuid
        check(uuid !in mobsByUuid) { "Mob UUID already registered: $uuid" }
        mobsByUuid[uuid] = mob
        mobsByDefendingTeam.getValue(mob.identity.attackedTeam).add(uuid)
        mobsByRoute.getOrPut(mob.route.routeId) { linkedSetOf() }.add(uuid)
    }

    fun updateMobRoute(uuid: UUID, newRouteId: String) {
        val mob = mobsByUuid[uuid] ?: error("Mob not registered: $uuid")
        val oldRouteId = mob.route.routeId
        if (oldRouteId == newRouteId) return
        mobsByRoute[oldRouteId]?.remove(uuid)
        if (mobsByRoute[oldRouteId]?.isEmpty() == true) mobsByRoute.remove(oldRouteId)
        mobsByRoute.getOrPut(newRouteId) { linkedSetOf() }.add(uuid)
        mob.route = mob.route.copy(routeId = newRouteId)
    }

    fun unregisterMob(uuid: UUID): MobRuntimeState? {
        val mob = mobsByUuid.remove(uuid) ?: return null
        mobsByDefendingTeam.getValue(mob.identity.attackedTeam).remove(uuid)
        mobsByRoute[mob.route.routeId]?.remove(uuid)
        if (mobsByRoute[mob.route.routeId]?.isEmpty() == true) mobsByRoute.remove(mob.route.routeId)
        return mob
    }

    fun registerTower(tower: TowerRuntimeState) {
        val id = tower.identity.instanceId
        check(id !in towersByInstanceId) { "Tower already registered: ${id.value}" }
        towersByInstanceId[id] = tower
        towersByTeam.getValue(tower.identity.team).add(id)
    }

    fun unregisterTower(id: TowerInstanceId): TowerRuntimeState? {
        val tower = towersByInstanceId.remove(id) ?: return null
        towersByTeam.getValue(tower.identity.team).remove(id)
        return tower
    }


    fun registerGuard(guard: GuardRuntime) {
        val uuid = guard.identity.entityUuid
        check(uuid !in guardsByUuid) {
            "Guard already registered: $uuid"
        }
        guardsByUuid[uuid] = guard
        guardsByTeam
            .getValue(guard.identity.team)
            .add(uuid)
    }

    fun unregisterGuard(uuid: UUID): GuardRuntime? {
        val guard = guardsByUuid.remove(uuid)
            ?: return null
        guardsByTeam
            .getValue(guard.identity.team)
            .remove(uuid)
        return guard
    }

    fun assertConsistent() {
        mobsByUuid.forEach { (uuid, mob) ->
            check(uuid in mobsByDefendingTeam.getValue(mob.identity.attackedTeam))
            check(uuid in mobsByRoute.getValue(mob.route.routeId))
        }
        mobsByDefendingTeam.values.flatten().forEach { check(it in mobsByUuid) }
        mobsByRoute.values.flatten().forEach { check(it in mobsByUuid) }

        towersByInstanceId.forEach { (id, tower) ->
            check(id in towersByTeam.getValue(tower.identity.team))
        }
        towersByTeam.values.flatten().forEach {
            check(it in towersByInstanceId)
        }

        guardsByUuid.forEach { (uuid, guard) ->
            check(
                uuid in guardsByTeam
                    .getValue(guard.identity.team)
            )
        }
        guardsByTeam.values.flatten().forEach {
            check(it in guardsByUuid)
        }
    }

    fun isEmpty(): Boolean = mobsByUuid.isEmpty() &&
        mobsByDefendingTeam.values.all { it.isEmpty() } &&
        mobsByRoute.values.all { it.isEmpty() } &&
        towersByInstanceId.isEmpty() &&
        towersByTeam.values.all { it.isEmpty() } &&
        guardsByUuid.isEmpty() &&
        guardsByTeam.values.all { it.isEmpty() } &&
        transientDisplays.isEmpty() &&
        projectiles.isEmpty()

    fun clearAll() {
        mobsByUuid.clear()
        mobsByDefendingTeam.values.forEach { it.clear() }
        mobsByRoute.clear()
        towersByInstanceId.clear()
        towersByTeam.values.forEach { it.clear() }
        guardsByUuid.clear()
        guardsByTeam.values.forEach { it.clear() }
        transientDisplays.clear()
        projectiles.clear()
    }
}
