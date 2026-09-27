package dev.cubecrafttd.player

import dev.cubecrafttd.arena.ArenaEntityIndex
import dev.cubecrafttd.combat.KillAttributionDecision
import dev.cubecrafttd.combat.KillAttributionService
import dev.cubecrafttd.mob.*
import java.util.UUID

enum class PlayerWeaponAttackKind {
    SWORD,
    BOW
}

data class PlayerWeaponHitCommand(
    val playerUuid: UUID,
    val targetMobUuid: UUID,
    val weapon: PlayerWeaponAttackKind,
    /**
     * Final hit damage comes from the Paper/vanilla combat event adapter.
     * Core deliberately does not reimplement the 1.9+ attack-cooldown formula.
     */
    val actualDamage: Double
) {
    init { require(actualDamage >= 0.0) }
}

data class PlayerWeaponHitResult(
    val targetMobUuid: UUID,
    val damageApplied: Double,
    val killed: Boolean,
    val attribution: KillAttributionDecision?
)

class PlayerWeaponCombatService(
    private val index: ArenaEntityIndex,
    private val lethalResolver:
        MobLethalHitResolver =
        MobLethalHitResolver(),
    private val playerDamageEvents:
        PlayerMobDamageEventPort =
        NoOpPlayerMobDamageEventPort
) {
    fun hit(
        command: PlayerWeaponHitCommand
    ): PlayerWeaponHitResult {
        val mob = index.mobsByUuid[
            command.targetMobUuid
        ] ?: error(
            "Unknown mob ${command.targetMobUuid}"
        )
        check(
            mob.combat.lifecycle !=
                MobLifecycleState.DEAD &&
            mob.combat.lifecycle !=
                MobLifecycleState.REMOVED
        ) { "Mob is no longer damageable" }

        val source = when (command.weapon) {
            PlayerWeaponAttackKind.SWORD ->
                DamageSourceIdentity.PlayerSword(
                    command.playerUuid
                )
            PlayerWeaponAttackKind.BOW ->
                DamageSourceIdentity.PlayerBow(
                    command.playerUuid
                )
        }

        val healthBefore=mob.combat.health
        mob.combat.lastEligibleDamageSource =
            source
        mob.combat.health =
            (
                mob.combat.health -
                    command.actualDamage
            ).coerceAtLeast(0.0)
        val appliedDamage=
            (healthBefore-mob.combat.health).coerceAtLeast(0.0)
        if(appliedDamage>0.0) {
            playerDamageEvents.record(
                PlayerMobDamageEvent(
                    command.targetMobUuid,
                    command.playerUuid,
                    appliedDamage
                )
            )
        }

        var killed = false
        var attribution:
            KillAttributionDecision? = null
        if(mob.combat.health <= 0.0) {
            val lethal=
                lethalResolver.resolve(
                    mob,source
                )
            killed=
                lethal.kind ==
                    MobLethalOutcomeKind
                        .FINAL_DEATH
            attribution=
                lethal.attribution
        }

        return PlayerWeaponHitResult(
            command.targetMobUuid,
            command.actualDamage,
            killed,
            attribution
        )
    }
}
