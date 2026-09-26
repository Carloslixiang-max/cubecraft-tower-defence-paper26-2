package dev.cubecrafttd.tower.lifecycle

import dev.cubecrafttd.tower.*
import dev.cubecrafttd.tower.visual.TowerPath
import java.util.UUID
import kotlin.math.floor

enum class SellRefundBase {
    TOTAL_CURRENT_GAME_INVESTMENT,
    FIXED_CURRENT_STAGE_PRICE
}

data class TowerSellRefundPolicy(
    val rate: Double,
    val base: SellRefundBase,
    val evidenceStatus: String
) {
    init { require(rate in 0.0..1.0) }

    fun refund(
        currentGameInvestment: Long,
        fixedCurrentStagePrice: Long? = null
    ): Long {
        val baseValue = when (base) {
            SellRefundBase.TOTAL_CURRENT_GAME_INVESTMENT ->
                currentGameInvestment
            SellRefundBase.FIXED_CURRENT_STAGE_PRICE ->
                fixedCurrentStagePrice
                    ?: error("Fixed-stage sell price required")
        }
        return floor(baseValue.toDouble() * rate).toLong()
    }

    companion object {
        val RECOMMENDED_MATURE = TowerSellRefundPolicy(
            rate = 0.60,
            base = SellRefundBase.TOTAL_CURRENT_GAME_INVESTMENT,
            evidenceStatus =
                "SELL_RATE_MEDIUM_HIGH; TOTAL_INVESTMENT_BASE_PARTIAL"
        )
    }
}

interface TowerInteractionPolicy {
    fun mayUpgrade(actor: UUID, tower: TowerRuntimeState): Boolean
    fun maySell(actor: UUID, tower: TowerRuntimeState): Boolean
}

object EngineeringOwnerOnlyTowerInteractionPolicy :
    TowerInteractionPolicy {
    override fun mayUpgrade(
        actor: UUID,
        tower: TowerRuntimeState
    ): Boolean = actor == tower.identity.ownerUuid

    override fun maySell(
        actor: UUID,
        tower: TowerRuntimeState
    ): Boolean = actor == tower.identity.ownerUuid
}

/**
 * Historical Tower Defence behavior allowed remaining teammates to remove a
 * tower after its owner left the match. This policy preserves normal owner-only
 * interaction while the owner is present, then permits same-team teammates to
 * upgrade or sell that departed owner's tower.
 *
 * The departed-owner set is arena-local runtime state. Enemy players are never
 * granted access.
 */
class DepartedOwnerTeamTowerInteractionPolicy(
    private val context:
        dev.cubecrafttd.arena.ArenaContext,
    private val departedOwners:
        Set<UUID>
) : TowerInteractionPolicy {
    override fun mayUpgrade(
        actor: UUID,
        tower: TowerRuntimeState
    ): Boolean =
        mayInteract(actor,tower)

    override fun maySell(
        actor: UUID,
        tower: TowerRuntimeState
    ): Boolean =
        mayInteract(actor,tower)

    private fun mayInteract(
        actor: UUID,
        tower: TowerRuntimeState
    ): Boolean {
        if(actor==tower.identity.ownerUuid) {
            return true
        }
        if(
            tower.identity.ownerUuid !in
                departedOwners
        ) {
            return false
        }

        val members=
            when(tower.identity.team) {
                dev.cubecrafttd.arena.TeamId.RED ->
                    context.redTeam.players
                dev.cubecrafttd.arena.TeamId.BLUE ->
                    context.blueTeam.players
            }
        return actor in members &&
            actor !in departedOwners
    }
}

object TowerPathPreselection {
    /**
     * 2021 behavior: selection happens before placement; when no explicit
     * selection is made, Top is the documented default.
     */
    fun resolve(
        explicit: TowerPath?
    ): TowerPath = explicit ?: TowerPath.TOP
}
