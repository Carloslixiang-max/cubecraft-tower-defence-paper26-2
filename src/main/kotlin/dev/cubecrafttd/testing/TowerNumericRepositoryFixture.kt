package dev.cubecrafttd.testing

import dev.cubecrafttd.tower.*

object TowerNumericRepositoryFixture {
    fun run(): List<FixtureResult> {
        val towers = RecommendedMatureTowerDefinitions
        val archer = towers.get("archer")
        val leach = towers.get("leach")
        val ice = towers.get("ice")
        val poison = towers.get("poison")
        val zeus = towers.get("zeus")
        val turret = towers.get("turret")

        return listOf(
            FixtureResult(
                "tower-numeric-eleven-families",
                towers.all().size == 11
            ),
            FixtureResult(
                "archer-2021-path1-rate-remains-unresolved",
                archer.stage(TowerPathOption.TOP,4).stats.attackIntervalSeconds == null &&
                    archer.stage(TowerPathOption.TOP,4).stats.baseline2020AttackIntervalSeconds == 1.2
            ),
            FixtureResult(
                "archer-bottom-2020-numeric",
                archer.stage(TowerPathOption.BOTTOM,4).stats.damage == 7.5 &&
                    archer.stage(TowerPathOption.BOTTOM,4).stats.extras["aoeRange"] == 2.0
            ),
            FixtureResult(
                "leach-2021-explicit-overrides",
                leach.stage(TowerPathOption.NONE,1).stats.damage == 50.0 &&
                    leach.stage(TowerPathOption.NONE,2).stats.extras["beamDamage2021"] == 45.0 &&
                    leach.teamLimit == 1
            ),
            FixtureResult(
                "ice-2021-level4",
                ice.stage(TowerPathOption.NONE,4).stats.damage == 4.0 &&
                    ice.stage(TowerPathOption.NONE,4).stats.extras["damageChance2021"] == 0.5
            ),
            FixtureResult(
                "poison-2021-path2-rate",
                poison.stage(TowerPathOption.BOTTOM,3).stats.attackIntervalSeconds == 6.0 &&
                    poison.stage(TowerPathOption.BOTTOM,3).stats.baseline2020AttackIntervalSeconds == 10.0
            ),
            FixtureResult(
                "zeus-2021-full-explicit-patches",
                zeus.stage(TowerPathOption.NONE,1).stats.cost == 420L &&
                    zeus.stage(TowerPathOption.NONE,2).stats.damage == 24.0 &&
                    zeus.stage(TowerPathOption.TOP,4).stats.attackIntervalSeconds == 3.0 &&
                    zeus.stage(TowerPathOption.BOTTOM,4).stats.damage == 60.0 &&
                    zeus.stage(TowerPathOption.BOTTOM,4).stats.attackIntervalSeconds == 2.0
            ),
            FixtureResult(
                "turret-path-stats",
                turret.stage(TowerPathOption.TOP,3).stats.rangeBlocks == 20.0 &&
                    turret.stage(TowerPathOption.TOP,3).stats.attackIntervalSeconds == 0.2 &&
                    turret.stage(TowerPathOption.BOTTOM,3).stats.damage == 30.0
            )
        )
    }
}
