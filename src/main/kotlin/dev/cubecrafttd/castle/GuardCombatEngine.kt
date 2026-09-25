package dev.cubecrafttd.castle

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.combat.*
import dev.cubecrafttd.mob.*
import java.util.UUID

data class GuardGeometryView(
    val distanceBlocks: Double,
    val lineOfSight: Boolean
)

fun interface GuardGeometryProvider {
    fun geometry(
        guard: GuardRuntime,
        mobUuid: UUID
    ): GuardGeometryView?
}

data class GuardAttackReport(
    val guardId: Long,
    val targetUuid: UUID?,
    val damageApplied: Double,
    val killed: Boolean,
    val attribution: KillAttributionDecision?
)

class GuardCombatEngine(
    private val context: ArenaContext,
    private val config: GuardResolvedCombatConfig,
    private val geometryProvider: GuardGeometryProvider,
    private val lethalResolver:
        MobLethalHitResolver =
        MobLethalHitResolver()
) {
    fun tryAttack(
        guard: GuardRuntime
    ): GuardAttackReport {
        if (
            guard.lifecycle !=
                GuardLifecycleState.ACTIVE
        ) {
            return GuardAttackReport(
                guard.identity.guardInstanceId,
                null,0.0,false,null
            )
        }
        if (
            context.gameTick <
                guard.nextAttackTick
        ) {
            return GuardAttackReport(
                guard.identity.guardInstanceId,
                null,0.0,false,null
            )
        }

        val candidates = context.entityIndex
            .mobsByDefendingTeam
            .getValue(guard.identity.team)
            .mapNotNull { uuid ->
                val mob =
                    context.entityIndex
                        .mobsByUuid[uuid]
                        ?: return@mapNotNull null
                if (
                    mob.combat.lifecycle !=
                        MobLifecycleState.MOVING &&
                    mob.combat.lifecycle !=
                        MobLifecycleState.ATTACKING_CASTLE
                ) return@mapNotNull null

                val profile =
                    RecommendedMatureMobCombatProfiles
                        .get(mob.form.formId)
                if (
                    profile.targetLayer ==
                        TargetLayer.BOSS
                ) return@mapNotNull null

                val geometry =
                    geometryProvider.geometry(
                        guard,uuid
                    ) ?: return@mapNotNull null
                if (
                    geometry.distanceBlocks >
                        config.rangeBlocks.value ||
                    !geometry.lineOfSight
                ) return@mapNotNull null

                GuardCandidate(
                    mob,geometry.distanceBlocks
                )
            }

        val selected = select(candidates)
            ?: return GuardAttackReport(
                guard.identity.guardInstanceId,
                null,0.0,false,null
            )

        val mob = selected.mob
        val damage =
            config.damagePerArrow.value
        val source =
            DamageSourceIdentity.CastleGuard(
                guard.identity.team
            )
        mob.combat.lastEligibleDamageSource =
            source
        mob.combat.health =
            (mob.combat.health - damage)
                .coerceAtLeast(0.0)

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

        advanceClock(guard)

        return GuardAttackReport(
            guard.identity.guardInstanceId,
            mob.identity.entityUuid,
            damage,
            killed,
            attribution
        )
    }

    private fun select(
        candidates: List<GuardCandidate>
    ): GuardCandidate? {
        if (candidates.isEmpty()) return null
        return when (
            config.targetPriority.value
        ) {
            GuardTargetPriorityPolicy.FIRST ->
                candidates.maxWithOrNull(
                    compareBy<GuardCandidate> {
                        it.mob.route.routeProgress
                    }.thenByDescending {
                        it.mob.identity.entityUuid
                            .toString()
                    }
                )
            GuardTargetPriorityPolicy.LAST ->
                candidates.minWithOrNull(
                    compareBy<GuardCandidate> {
                        it.mob.route.routeProgress
                    }.thenBy {
                        it.mob.identity.entityUuid
                            .toString()
                    }
                )
            GuardTargetPriorityPolicy.RANDOM ->
                candidates[
                    context.rng.nextInt(
                        candidates.size
                    )
                ]
            GuardTargetPriorityPolicy
                .EXPLICIT_ENGINEERING ->
                candidates.minByOrNull {
                    it.mob.identity
                        .entityUuid.toString()
                }
        }
    }

    private fun advanceClock(
        guard: GuardRuntime
    ) {
        var next =
            guard.nextAttackTick +
                config.fireIntervalTicks.value
        while (next <= context.gameTick) {
            next +=
                config.fireIntervalTicks.value
        }
        guard.nextAttackTick = next
    }

    private data class GuardCandidate(
        val mob: MobRuntimeState,
        val distance: Double
    )
}

class GuardCombatTickPhase(
    private val config: GuardResolvedCombatConfig,
    private val geometryProvider:
        GuardGeometryProvider,
    private val lethalResolver:
        dev.cubecrafttd.mob.MobLethalHitResolver =
        dev.cubecrafttd.mob.MobLethalHitResolver()
) : ArenaTickPhase {
    override val order: Int = 70
    override val id: String = "castle-guards"

    override fun tick(
        context: ArenaContext
    ) {
        val engine = GuardCombatEngine(
            context,
            config,
            geometryProvider,
            lethalResolver
        )
        context.entityIndex.guardsByUuid
            .values
            .toList()
            .sortedBy {
                it.identity.guardInstanceId
            }
            .forEach(engine::tryAttack)
    }
}
