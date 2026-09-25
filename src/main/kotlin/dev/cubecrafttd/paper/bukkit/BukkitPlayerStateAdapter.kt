package dev.cubecrafttd.paper.bukkit

import dev.cubecrafttd.player.*
import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import java.io.*
import java.util.UUID

class BukkitPlayerStateAdapter(
    private val server: Server
) : PlayerStateAdapter {
    override fun capture(
        playerUuid: UUID,
        arenaTick: Long
    ): PlayerSnapshot {
        val p = requirePlayer(playerUuid)
        return PlayerSnapshot(
            playerUuid = playerUuid,
            capturedAtArenaTick = arenaTick,
            locationPayload = encodeLocation(p.location),
            gameModeName = p.gameMode.name,
            inventoryPayload = ItemStack.serializeItemsAsBytes(
                p.inventory.storageContents
            ),
            armorPayload = ItemStack.serializeItemsAsBytes(
                p.inventory.armorContents
            ),
            offhandPayload = p.inventory.itemInOffHand
                .serializeAsBytes(),
            level = p.level,
            expProgress = p.exp,
            totalExperience = p.totalExperience,
            health = p.health,
            absorption = p.absorptionAmount,
            foodLevel = p.foodLevel,
            saturation = p.saturation,
            exhaustion = p.exhaustion,
            fireTicks = p.fireTicks,
            remainingAir = p.remainingAir,
            allowFlight = p.allowFlight,
            flying = p.isFlying,
            fallDistance = p.fallDistance,
            potionEffectsPayload = encodeEffects(p),
            velocityPayload = encodeVector(
                p.velocity.x,p.velocity.y,p.velocity.z
            ),
            heldItemSlot =
                p.inventory.heldItemSlot,
            cursorItemPayload =
                p.itemOnCursor.serializeAsBytes()
        )
    }

    override fun prepareForMatch(
        playerUuid: UUID
    ) {
        val p = requirePlayer(playerUuid)

        // The cursor is separately snapshotted. Clear it before closing the
        // current view so Bukkit cannot merge/drop it into the inventory that
        // is about to be cleared for the match.
        p.setItemOnCursor(null)
        p.closeInventory()

        p.inventory.clear()
        p.inventory.setArmorContents(arrayOfNulls<org.bukkit.inventory.ItemStack>(4))
        p.inventory.setItemInOffHand(ItemStack.empty())
        p.clearActivePotionEffects()
        p.fireTicks = 0
        p.fallDistance = 0f
        p.gameMode = GameMode.ADVENTURE
        p.foodLevel = 20
        p.saturation = 20f
        p.exhaustion = 0f
        p.isFlying = false
        p.allowFlight = false
        p.velocity = org.bukkit.util.Vector(0,0,0)
        p.level = 0
        p.exp = 0f
        p.totalExperience = 0
    }

    override fun restore(snapshot: PlayerSnapshot) {
        val p = requirePlayer(snapshot.playerUuid)
        p.closeInventory()

        check(p.teleport(decodeLocation(snapshot.locationPayload))) {
            "Failed to restore player ${snapshot.playerUuid} location"
        }
        p.gameMode = GameMode.valueOf(snapshot.gameModeName)
        p.inventory.setStorageContents(
            ItemStack.deserializeItemsFromBytes(
                snapshot.inventoryPayload
            )
        )
        p.inventory.setArmorContents(
            ItemStack.deserializeItemsFromBytes(
                snapshot.armorPayload
            )
        )
        p.inventory.setItemInOffHand(
            ItemStack.deserializeBytes(
                snapshot.offhandPayload
            )
        )
        p.inventory.heldItemSlot =
            snapshot.heldItemSlot

        p.totalExperience = snapshot.totalExperience
        p.level = snapshot.level
        p.exp = snapshot.expProgress
        p.health = snapshot.health
        p.absorptionAmount = snapshot.absorption
        p.foodLevel = snapshot.foodLevel
        p.saturation = snapshot.saturation
        p.exhaustion = snapshot.exhaustion
        p.fireTicks = snapshot.fireTicks
        p.remainingAir = snapshot.remainingAir
        p.allowFlight = snapshot.allowFlight
        p.isFlying = snapshot.flying && snapshot.allowFlight
        p.fallDistance = snapshot.fallDistance

        p.clearActivePotionEffects()
        decodeEffects(snapshot.potionEffectsPayload)
            .forEach(p::addPotionEffect)

        val (x,y,z)=decodeVector(snapshot.velocityPayload)
        p.velocity = org.bukkit.util.Vector(x,y,z)

        // v1 recovery files decode with an empty cursor payload.
        // v2 restores the exact serialized ItemStack.
        if(snapshot.cursorItemPayload.isEmpty()) {
            p.setItemOnCursor(null)
        } else {
            p.setItemOnCursor(
                ItemStack.deserializeBytes(
                    snapshot.cursorItemPayload
                )
            )
        }
    }

    override fun isOnline(
        playerUuid: UUID
    ): Boolean = server.getPlayer(playerUuid)?.isOnline == true

    private fun requirePlayer(uuid: UUID): Player =
        server.getPlayer(uuid)
            ?: error("Player $uuid is not online")

    private fun encodeLocation(location: Location): ByteArray =
        ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { out ->
                val world = location.world
                    ?: error("Player location has no world")
                out.writeLong(world.uid.mostSignificantBits)
                out.writeLong(world.uid.leastSignificantBits)
                out.writeDouble(location.x)
                out.writeDouble(location.y)
                out.writeDouble(location.z)
                out.writeFloat(location.yaw)
                out.writeFloat(location.pitch)
            }
            bytes.toByteArray()
        }

    private fun decodeLocation(payload: ByteArray): Location =
        DataInputStream(ByteArrayInputStream(payload)).use { input ->
            val worldId = UUID(input.readLong(),input.readLong())
            val world = server.getWorld(worldId)
                ?: error("Snapshot world $worldId is not loaded")
            Location(
                world,
                input.readDouble(),
                input.readDouble(),
                input.readDouble(),
                input.readFloat(),
                input.readFloat()
            )
        }

    private fun encodeVector(x:Double,y:Double,z:Double): ByteArray =
        ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { out ->
                out.writeDouble(x); out.writeDouble(y); out.writeDouble(z)
            }
            bytes.toByteArray()
        }

    private fun decodeVector(payload: ByteArray): Triple<Double,Double,Double> =
        DataInputStream(ByteArrayInputStream(payload)).use {
            Triple(it.readDouble(),it.readDouble(),it.readDouble())
        }

    private fun encodeEffects(player: Player): ByteArray =
        ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { out ->
                val effects = player.activePotionEffects
                    .sortedBy { Registry.MOB_EFFECT.getKeyOrThrow(it.type).asString() }
                out.writeInt(effects.size)
                effects.forEach { effect ->
                    out.writeUTF(
                        Registry.MOB_EFFECT.getKeyOrThrow(effect.type).asString()
                    )
                    out.writeInt(effect.duration)
                    out.writeInt(effect.amplifier)
                    out.writeBoolean(effect.isAmbient)
                    out.writeBoolean(effect.hasParticles())
                    out.writeBoolean(effect.hasIcon())
                }
            }
            bytes.toByteArray()
        }

    private fun decodeEffects(payload: ByteArray): List<PotionEffect> =
        DataInputStream(ByteArrayInputStream(payload)).use { input ->
            List(input.readInt()) {
                val key = NamespacedKey.fromString(input.readUTF())
                    ?: error("Invalid potion effect key in snapshot")
                val type = Registry.MOB_EFFECT.getOrThrow(key)
                PotionEffect(
                    type,
                    input.readInt(),
                    input.readInt(),
                    input.readBoolean(),
                    input.readBoolean(),
                    input.readBoolean()
                )
            }
        }
}
