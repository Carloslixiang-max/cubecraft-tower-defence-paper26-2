package dev.cubecrafttd.tower

import dev.cubecrafttd.mob.DamageKind
import dev.cubecrafttd.mob.EffectKind
import dev.cubecrafttd.mob.TargetLayer

enum class TowerFootprint { THREE_BY_THREE, FIVE_BY_FIVE }
enum class TowerPathOption { TOP, BOTTOM, NONE }

data class TowerStageStats(
    val cost: Long?,
    val cumulativeCost: Long?,
    val damage: Double?,
    val attackIntervalSeconds: Double?,
    val rangeBlocks: Double?,
    val extras: Map<String, Double> = emptyMap(),
    val baseline2020AttackIntervalSeconds: Double? = null
)

data class TowerPathDefinition(
    val option: TowerPathOption,
    val level: Int,
    val stats: TowerStageStats,
    val abilityTags: Set<String> = emptySet(),
    val evidenceStatus: String,
    val targetLayersOverride:
        Set<TargetLayer>? = null
)

data class TowerDefinition(
    val towerId: String,
    val displayName: String,
    val footprint: TowerFootprint,
    val targetLayers: Set<TargetLayer>,
    val damageKinds: Set<DamageKind>,
    val effectKinds: Set<EffectKind> = emptySet(),
    val levels: List<TowerPathDefinition>,
    val teamLimit: Int? = null,
    val specialTags: Set<String> = emptySet(),
    val unresolvedFields: Set<String> = emptySet()
) {
    fun stage(path: TowerPathOption, level: Int): TowerPathDefinition =
        levels.firstOrNull { it.option == path && it.level == level }
            ?: error("No stage $towerId $path L$level")
}

interface TowerDefinitionRepository {
    fun all(): Map<String, TowerDefinition>
    fun get(towerId: String): TowerDefinition =
        all()[towerId] ?: error("Unknown towerId $towerId")
}

private fun s(
    cost: Long?,
    cumulative: Long?,
    damage: Double?,
    interval: Double?,
    range: Double?,
    extras: Map<String, Double> = emptyMap(),
    baseline2020Interval: Double? = interval
) = TowerStageStats(
    cost, cumulative, damage, interval, range, extras, baseline2020Interval
)

private fun d(
    option: TowerPathOption,
    level: Int,
    stats: TowerStageStats,
    vararg abilities: String,
    evidence: String = "2020_BASELINE",
    targetLayers:
        Set<TargetLayer>? = null
) = TowerPathDefinition(
    option,level,stats,abilities.toSet(),
    evidence,targetLayers
)

/**
 * RECOMMENDED_MATURE numeric repository:
 * 2020 baseline + explicit 2021 overrides.
 *
 * Important: Archer Path 1's 2021 fire rate is known to have increased, but the
 * official post did not provide the full replacement table. Those mature exact
 * intervals are therefore null; the 2020 values remain only as baseline metadata.
 */
object RecommendedMatureTowerDefinitions : TowerDefinitionRepository {
    private val defs = listOf(
        TowerDefinition(
            "archer","Archer Tower",TowerFootprint.THREE_BY_THREE,
            setOf(TargetLayer.GROUND,TargetLayer.AIR),setOf(DamageKind.PHYSICAL),
            levels=listOf(
                d(TowerPathOption.NONE,1,s(160,160,5.0,null,10.0,baseline2020Interval=2.0),
                    "single_arrow",evidence="2021_FIRE_RATE_OVERRIDE_EXACT_UNKNOWN"),
                d(TowerPathOption.TOP,2,s(260,420,5.0,null,12.0,baseline2020Interval=1.5),
                    "single_arrow",evidence="2021_FIRE_RATE_OVERRIDE_EXACT_UNKNOWN"),
                d(TowerPathOption.TOP,3,s(450,870,12.0,null,13.0,baseline2020Interval=1.5),
                    "multi_arrow",evidence="2021_FIRE_RATE_OVERRIDE_EXACT_UNKNOWN"),
                d(TowerPathOption.TOP,4,s(920,1790,20.0,null,15.0,baseline2020Interval=1.2),
                    "up_to_4_arrows",evidence="2021_FIRE_RATE_OVERRIDE_EXACT_UNKNOWN"),
                d(TowerPathOption.BOTTOM,2,s(220,380,5.0,2.0,12.0),"fire_arrow"),
                d(TowerPathOption.BOTTOM,3,s(660,1040,10.0,2.0,13.0),"fire_arrow"),
                d(TowerPathOption.BOTTOM,4,s(1690,2730,7.5,2.0,15.0,mapOf("aoeRange" to 2.0)),
                    "explosive_arrow")
            ),
            unresolvedFields=setOf("Path1 Mature exact fire delay","branch-specific exact firing origins","bottom IV mature body")
        ),

        TowerDefinition(
            "leach","Leach Tower",TowerFootprint.FIVE_BY_FIVE,
            setOf(TargetLayer.GROUND,TargetLayer.AIR),setOf(DamageKind.LIGHTNING),
            levels=listOf(
                d(TowerPathOption.NONE,1,s(3000,3000,50.0,5.0,12.0,
                    mapOf("baseline2020Damage" to 35.0,"bonusDeathRay2020" to 30.0)),
                    "charge",evidence="2021_EXPLICIT_LEVEL1_DAMAGE_OVERRIDE"),
                d(TowerPathOption.NONE,2,s(2100,5100,60.0,5.0,15.0,
                    mapOf("beamDamage2021" to 45.0,"bonusDeathRay2020" to 45.0)),
                    "charge",evidence="2021_EXPLICIT_BEAM_OVERRIDE"),
                d(TowerPathOption.NONE,3,s(3600,8700,90.0,5.0,15.0,
                    mapOf("beamDamage2021" to 45.0,"bonusDeathRay2020" to 60.0)),
                    "death_ray",evidence="2021_EXPLICIT_BEAM_OVERRIDE")
            ),
            teamLimit=1,
            unresolvedFields=setOf("charge threshold","charge gain","mature beam visual")
        ),

        TowerDefinition(
            "artillery","Artillery Tower",TowerFootprint.THREE_BY_THREE,
            setOf(TargetLayer.GROUND),setOf(DamageKind.PHYSICAL),setOf(EffectKind.STUN),
            levels=listOf(
                d(TowerPathOption.NONE,1,s(240,240,16.5,7.0,10.0),"aoe"),
                d(TowerPathOption.TOP,2,s(110,350,20.0,7.0,12.0,
                    mapOf("fragDamage" to 12.5,"aoeRange" to 2.0)),"aoe"),
                d(TowerPathOption.TOP,3,s(270,620,20.0,7.0,13.0,
                    mapOf("fragDamage" to 12.5,"aoeRange" to 2.0,"stunSeconds" to 0.5)),"stun"),
                d(TowerPathOption.TOP,4,s(300,920,20.0,7.0,15.0,
                    mapOf("fragDamage" to 12.5,"aoeRange" to 2.0,"stunSeconds" to 1.0)),"stun"),
                d(TowerPathOption.BOTTOM,3,s(390,740,17.5,5.0,13.0,mapOf("aoeRange" to 3.0)),"landmine"),
                d(TowerPathOption.BOTTOM,4,s(460,1200,17.5,4.0,15.0,mapOf("aoeRange" to 3.0)),"landmine")
            ),
            unresolvedFields=setOf("landmine global/shared cap")
        ),

        TowerDefinition(
            "mage","Mage Tower",TowerFootprint.THREE_BY_THREE,
            setOf(TargetLayer.GROUND),setOf(DamageKind.FIRE),
            levels=listOf(
                d(TowerPathOption.NONE,1,s(350,350,10.0,5.0,10.0,
                    mapOf("fireDamage" to 5.0,"burnDurationSeconds" to 2.0)),"burn"),
                d(TowerPathOption.NONE,2,s(70,420,10.0,4.0,12.0,
                    mapOf("fireDamage" to 5.0,"burnDurationSeconds" to 5.0)),"burn"),
                d(TowerPathOption.NONE,3,s(90,510,10.0,3.0,13.0,
                    mapOf("fireDamage" to 5.0,"burnDurationSeconds" to 8.0)),"fireball_rain"),
                d(TowerPathOption.NONE,4,s(580,1090,10.0,1.0,15.0,
                    mapOf("fireDamage" to 15.0,"burnDurationSeconds" to 12.0)),"fireball_rain")
            )
        ),

        TowerDefinition(
            "ice","Ice Tower",TowerFootprint.THREE_BY_THREE,
            setOf(TargetLayer.GROUND),setOf(DamageKind.PHYSICAL),setOf(EffectKind.ICE_SLOW),
            levels=listOf(
                d(TowerPathOption.NONE,1,s(280,280,0.0,1.0,10.0),"slow"),
                d(TowerPathOption.NONE,2,s(60,340,0.0,1.0,12.0),"slow"),
                d(TowerPathOption.NONE,3,s(80,360,0.0,1.0,13.0),"slow"),
                d(TowerPathOption.NONE,4,s(210,630,4.0,1.0,15.0,
                    mapOf("damageChance2021" to 0.5,"baseline2020Damage" to 0.0)),
                    "50_percent_damage_when_slowed",evidence="2021_OFFICIAL_EXPLICIT_OVERRIDE")
            )
        ),

        TowerDefinition(
            "poison","Poison Tower",TowerFootprint.THREE_BY_THREE,
            setOf(TargetLayer.GROUND,TargetLayer.AIR),setOf(DamageKind.POISON),
            levels=listOf(
                d(TowerPathOption.NONE,1,s(380,380,1.0,null,10.0,
                    mapOf("poisonDamage" to 2.0,"poisonTickSeconds" to 1.0,"durationSeconds" to 5.0)),
                    "poison_dot"),
                d(TowerPathOption.NONE,2,s(320,700,1.0,null,12.0,
                    mapOf("poisonDamage" to 2.0,"poisonTickSeconds" to 1.0,"durationSeconds" to 10.0)),
                    "poison_dot"),
                d(TowerPathOption.TOP,3,s(450,1150,1.0,null,13.0,
                    mapOf("poisonDamage" to 2.0,"poisonTickSeconds" to 1.0,"durationSeconds" to 30.0)),
                    "long_duration_poison"),
                d(TowerPathOption.TOP,4,s(720,1870,1.0,null,15.0,
                    mapOf("poisonDamage" to 2.0,"poisonTickSeconds" to 1.0,"durationSecondsTable" to 120.0)),
                    "very_long_duration_poison"),
                d(TowerPathOption.BOTTOM,3,s(520,1220,12.0,6.0,13.0,baseline2020Interval=10.0),
                    "close_range_damage",evidence="2021_EXPLICIT_FIRE_DELAY_OVERRIDE"),
                d(TowerPathOption.BOTTOM,4,s(270,1490,24.0,6.0,15.0,baseline2020Interval=10.0),
                    "close_range_damage",evidence="2021_EXPLICIT_FIRE_DELAY_OVERRIDE")
            ),
            unresolvedFields=setOf("top path true infinite-duration runtime semantics","bottom IV mature body")
        ),

        TowerDefinition(
            "quake","Quake Tower",TowerFootprint.THREE_BY_THREE,
            setOf(TargetLayer.GROUND),setOf(DamageKind.PHYSICAL),setOf(EffectKind.STUN),
            levels=listOf(
                d(TowerPathOption.NONE,1,s(200,200,5.0,5.0,10.0,mapOf("stunSeconds" to 3.0)),"aoe_stun"),
                d(TowerPathOption.NONE,2,s(320,520,7.0,4.0,12.0,mapOf("stunSeconds" to 4.0)),"aoe_stun"),
                d(TowerPathOption.NONE,3,s(560,1080,9.0,3.0,13.0,mapOf("stunSeconds" to 5.0)),"throw_blocks"),
                d(TowerPathOption.NONE,4,s(1450,2530,11.0,2.0,15.0,mapOf("stunSeconds" to 6.0)),"throw_blocks")
            ),
            specialTags=setOf("ground_only")
        ),

        TowerDefinition(
            "sorcerer","Sorcerer Tower",TowerFootprint.THREE_BY_THREE,
            setOf(TargetLayer.GROUND,TargetLayer.AIR),setOf(DamageKind.PHYSICAL),
            levels=listOf(
                d(TowerPathOption.NONE,1,s(800,800,7.0,5.0,10.0),"summon_animal",targetLayers=setOf(TargetLayer.GROUND)),
                d(TowerPathOption.NONE,2,s(200,1000,14.0,4.0,12.0),"summon_animal",targetLayers=setOf(TargetLayer.GROUND)),
                d(TowerPathOption.BOTTOM,3,s(300,1300,21.0,4.0,13.0),"multi_summon",targetLayers=setOf(TargetLayer.GROUND)),
                d(TowerPathOption.BOTTOM,4,s(530,1830,28.0,4.0,15.0),"up_to_4_animals",targetLayers=setOf(TargetLayer.GROUND)),
                d(TowerPathOption.TOP,3,s(130,1130,14.0,4.0,13.0,mapOf("aoeRange" to 2.0)),"kamikaze",targetLayers=setOf(TargetLayer.GROUND)),
                d(TowerPathOption.TOP,4,s(460,1590,28.0,4.0,15.0,mapOf("aoeRange" to 2.0)),"kamikaze",targetLayers=setOf(TargetLayer.GROUND))
            ),
            unresolvedFields=setOf("summon path exact entity counts/origins")
        ),

        TowerDefinition(
            "zeus","Zeus Tower",TowerFootprint.THREE_BY_THREE,
            setOf(TargetLayer.GROUND,TargetLayer.AIR),setOf(DamageKind.LIGHTNING),
            levels=listOf(
                d(TowerPathOption.NONE,1,s(420,420,15.0,5.0,10.0),
                    "lightning",evidence="2021_EXPLICIT_COST_OVERRIDE"),
                d(TowerPathOption.NONE,2,s(360,780,24.0,4.0,12.0),
                    "lightning",evidence="2021_EXPLICIT_COST_DAMAGE_OVERRIDE"),
                d(TowerPathOption.TOP,3,s(700,1480,40.0,4.0,13.0),
                    "chain_lightning",evidence="2021_EXPLICIT_COST_DAMAGE_OVERRIDE"),
                d(TowerPathOption.TOP,4,s(900,2380,40.0,3.0,15.0),
                    "bounce_up_to_5",evidence="2021_EXPLICIT_OVERRIDE"),
                d(TowerPathOption.BOTTOM,3,s(520,1300,45.0,3.0,13.0),
                    "baby_zeus",evidence="2021_EXPLICIT_OVERRIDE"),
                d(TowerPathOption.BOTTOM,4,s(720,2020,60.0,2.0,15.0),
                    "baby_zeus",evidence="2021_EXPLICIT_OVERRIDE")
            ),
            unresolvedFields=setOf("Zeus/Baby Zeus mature body exact")
        ),

        TowerDefinition(
            "necromancer","Necromancer Tower",TowerFootprint.FIVE_BY_FIVE,
            setOf(TargetLayer.GROUND,TargetLayer.AIR),setOf(DamageKind.PHYSICAL),
            levels=listOf(
                d(TowerPathOption.NONE,1,s(790,790,15.0,2.0,12.0),"iron_golem",targetLayers=setOf(TargetLayer.GROUND)),
                d(TowerPathOption.NONE,2,s(320,1110,25.0,1.0,13.0),"iron_golem",targetLayers=setOf(TargetLayer.GROUND)),
                d(TowerPathOption.TOP,3,s(560,1670,30.0,1.0,15.0),"shulker",targetLayers=setOf(TargetLayer.GROUND,TargetLayer.AIR)),
                d(TowerPathOption.BOTTOM,3,s(410,1520,25.0,1.0,14.0),"snowman",targetLayers=setOf(TargetLayer.GROUND))
            ),
            unresolvedFields=setOf("Shulker exact speed/origin","Snowman max pierce/origin","mature summoned-body visuals")
        ),

        TowerDefinition(
            "turret","Turret Tower",TowerFootprint.FIVE_BY_FIVE,
            setOf(TargetLayer.GROUND,TargetLayer.AIR),setOf(DamageKind.PHYSICAL),
            levels=listOf(
                d(TowerPathOption.NONE,1,s(1000,1000,5.0,0.5,12.0),"rapid_fire"),
                d(TowerPathOption.TOP,2,s(1200,2200,7.0,0.4,15.0),"super_long_range"),
                d(TowerPathOption.TOP,3,s(2800,5000,14.0,0.2,20.0),"super_long_range"),
                d(TowerPathOption.BOTTOM,2,s(1100,2100,15.0,0.5,12.0),"bounce"),
                d(TowerPathOption.BOTTOM,3,s(1500,3600,30.0,0.5,14.0),"bounce_up_to_5")
            ),
            unresolvedFields=setOf("bounce next-target geometry","head/rotation/bounce mature visual")
        )
    ).associateBy { it.towerId }

    override fun all(): Map<String, TowerDefinition> = defs
}
