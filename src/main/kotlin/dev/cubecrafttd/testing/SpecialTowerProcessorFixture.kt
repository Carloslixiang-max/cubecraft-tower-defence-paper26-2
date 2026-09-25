package dev.cubecrafttd.testing

import dev.cubecrafttd.tower.*
import dev.cubecrafttd.truth.*
import java.util.UUID

object SpecialTowerProcessorFixture {
    fun run(): List<FixtureResult> {
        val owner = UUID.fromString("00000000-0000-0000-0000-000000008888")
        val ids = (1..6).map {
            UUID.fromString("00000000-0000-0000-0000-%012d".format(it))
        }
        val ctx = TowerAttackContext(88, owner, 200)

        val zeus = SpecialTowerProcessors.zeusChain(
            RecommendedMatureTowerDefinitions.get("zeus")
                .stage(TowerPathOption.TOP,4),
            ids[0],
            ids.drop(1),
            ctx
        )
        val turret = SpecialTowerProcessors.turretBounce(
            RecommendedMatureTowerDefinitions.get("turret")
                .stage(TowerPathOption.BOTTOM,3),
            ids[0],
            ids.drop(1),
            ctx
        )
        val sorcerer = SpecialTowerProcessors.sorcererSummon(
            RecommendedMatureTowerDefinitions.get("sorcerer")
                .stage(TowerPathOption.BOTTOM,4),
            ids[0],ctx
        )
        val necro = SpecialTowerProcessors.necromancerSummon(
            RecommendedMatureTowerDefinitions.get("necromancer")
                .stage(TowerPathOption.TOP,3),
            ids[0],ctx
        )

        fun <T> fallback(key: String, value: T): ResolvedTruth<T> =
            ResolvedTruth(value, ResolutionSource.ENGINEERING_FALLBACK)

        val leachCharge = LeachChargeService(
            LeachChargeConfig(
                fallback("leach.maxCharge",100.0),
                fallback("leach.chargePerTargetTick",10.0)
            )
        )
        val state = LeachChargeState()
        leachCharge.addEligibleTargets(state,3)
        leachCharge.addEligibleTargets(state,7)
        val ready = leachCharge.ready(state)
        val consumed = leachCharge.consume(state)

        val leachBeam = SpecialTowerProcessors.leachBeam(
            RecommendedMatureTowerDefinitions.get("leach")
                .stage(TowerPathOption.NONE,2),
            listOf(ids[0],ids[1]),ctx
        )

        return listOf(
            FixtureResult(
                "zeus-bounce-up-to-five-total",
                zeus.impacts.size == 5 &&
                    zeus.impacts.all { it.damages.single().amount == 40.0 }
            ),
            FixtureResult(
                "turret-bounce-up-to-five-total",
                turret.impacts.size == 5 &&
                    turret.impacts.all { it.damages.single().amount == 30.0 }
            ),
            FixtureResult(
                "sorcerer-up-to-four-summons-metadata",
                sorcerer.kind == SummonKind.SORCERER_ANIMAL &&
                    sorcerer.maxActive == 4
            ),
            FixtureResult(
                "necromancer-shulker-path",
                necro.kind == SummonKind.SHULKER
            ),
            FixtureResult(
                "leach-charge-explicit-truth-gated-config",
                ready && consumed && state.charge == 0.0
            ),
            FixtureResult(
                "leach-beam-2021-metadata",
                leachBeam.impacts.size == 2 &&
                    leachBeam.impacts.first().damages.single().amount == 105.0 &&
                    leachBeam.impacts.last().damages.single().amount == 45.0
            )
        )
    }
}
