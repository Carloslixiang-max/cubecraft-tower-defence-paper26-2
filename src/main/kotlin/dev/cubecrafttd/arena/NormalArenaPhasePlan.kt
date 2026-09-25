package dev.cubecrafttd.arena

data class NormalArenaPhasePlanEntry(
    val id: String,
    val order: Int
)

object RecommendedNormalArenaPhasePlan {
    val entries: List<NormalArenaPhasePlanEntry> =
        listOf(
            NormalArenaPhasePlanEntry(
                "goldmine",
                NormalArenaPhaseOrder.GOLDMINE
            ),
            NormalArenaPhasePlanEntry(
                "troop-spawn",
                NormalArenaPhaseOrder.TROOP_SPAWN
            ),
            NormalArenaPhasePlanEntry(
                "mob-movement",
                NormalArenaPhaseOrder.MOB_MOVEMENT
            ),
            NormalArenaPhasePlanEntry(
                "mob-support",
                NormalArenaPhaseOrder.MOB_SUPPORT
            ),
            NormalArenaPhasePlanEntry(
                "leach-charge",
                NormalArenaPhaseOrder.LEACH_CHARGE
            ),
            NormalArenaPhasePlanEntry(
                "tower-summons",
                NormalArenaPhaseOrder.SUMMON_MAINTENANCE
            ),
            NormalArenaPhasePlanEntry(
                "tower-combat",
                NormalArenaPhaseOrder.TOWER_COMBAT
            ),
            NormalArenaPhasePlanEntry(
                "castle-guards",
                NormalArenaPhaseOrder.CASTLE_GUARDS
            ),
            NormalArenaPhasePlanEntry(
                "castle-attacks",
                NormalArenaPhaseOrder.CASTLE_ATTACKS
            ),
            NormalArenaPhasePlanEntry(
                "mob-cleanup",
                NormalArenaPhaseOrder.MOB_CLEANUP
            )
        )

    init {
        require(
            entries.map { it.order }
                .zipWithNext()
                .all { (a,b) -> a < b }
        ) {
            "Normal arena phases must have strictly increasing order"
        }
    }
}
