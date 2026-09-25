package dev.cubecrafttd.testing

import dev.cubecrafttd.mob.*
import dev.cubecrafttd.tower.*
import java.util.UUID

object TowerTargetingAndScheduleFixture {
    fun run(): List<FixtureResult> {
        val archer = TowerAttackCapability(
            "archer",
            setOf(TargetLayer.GROUND, TargetLayer.AIR),
            setOf(DamageKind.PHYSICAL)
        )
        val queryFirst = TowerTargetQuery(
            archer, 15.0, true, TargetPriorityPolicy.FIRST
        )
        val normal = RecommendedMatureMobCombatProfiles.get("zombie")
        val cave = RecommendedMatureMobCombatProfiles.get("cave_spider")
        val a = TowerTargetCandidate(
            UUID.fromString("00000000-0000-0000-0000-000000000001"),
            10.0, normal, 5.0, true
        )
        val b = TowerTargetCandidate(
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            30.0, normal, 8.0, true
        )
        val caveHidden = TowerTargetCandidate(
            UUID.fromString("00000000-0000-0000-0000-000000000003"),
            99.0, cave, 4.0, true
        )
        val blocked = TowerTargetCandidate(
            UUID.fromString("00000000-0000-0000-0000-000000000004"),
            100.0, normal, 4.0, false
        )

        val selected = TowerTargetSelector.select(
            listOf(a,b,caveHidden,blocked), queryFirst
        )

        val turret = RecommendedMatureTowerDefinitions.get("turret")
        val schedule = TowerAttackScheduleFactory.fromStage(
            turret.stage(TowerPathOption.TOP,3), 100
        )

        val archerUnresolved = try {
            TowerAttackScheduleFactory.fromStage(
                RecommendedMatureTowerDefinitions
                    .get("archer")
                    .stage(TowerPathOption.TOP,4),
                100
            )
            false
        } catch (_: IllegalStateException) {
            true
        }

        return listOf(
            FixtureResult(
                "tower-target-first-route-progress",
                selected?.entityUuid == b.entityUuid
            ),
            FixtureResult(
                "tower-target-filters-direct-hidden-and-los",
                selected?.entityUuid != caveHidden.entityUuid &&
                    selected?.entityUuid != blocked.entityUuid
            ),
            FixtureResult(
                "tower-schedule-numeric-stage",
                schedule.intervalTicks == 4L && schedule.nextAttackTick == 100L
            ),
            FixtureResult(
                "tower-schedule-blocks-unresolved-archer-mature-rate",
                archerUnresolved
            )
        )
    }
}
