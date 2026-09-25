package dev.cubecrafttd.mob

data class MobLevelDefinition(
    val level: Int,
    val unlockOrUpgradeExp: Long,
    val sendCoins: Long,
    val expRewardOnDeath: Long,
    val health: Double,
    val castleDamage: Double,
    val speedStat: Double
)

data class MobSpecialDefinition(
    val regeneration: Boolean? = null,
    val runThresholdHealthFraction: Double? = null,
    val shrinkOnKillMaxHealthLossFraction: Double? = null,
    val notes: List<String> = emptyList()
)

data class MobDefinition(
    val mobId: String,
    val displayName: String,
    val levels: List<MobLevelDefinition>,
    val special: MobSpecialDefinition = MobSpecialDefinition(),
    val provenance: String = "2020_BASELINE_PLUS_2021_OVERRIDES"
) {
    fun level(level: Int): MobLevelDefinition =
        levels.firstOrNull { it.level == level }
            ?: error("Unknown level $level for mob $mobId")
}

interface MobDefinitionRepository {
    fun all(): Map<String, MobDefinition>
    fun get(mobId: String): MobDefinition =
        all()[mobId] ?: error("Unknown mobId $mobId")
}

object RecommendedMatureMobDefinitions : MobDefinitionRepository {
    private fun l(
        level: Int,
        upgradeExp: Long,
        sendCoins: Long,
        expReward: Long,
        health: Double,
        castleDamage: Double,
        speed: Double
    ) = MobLevelDefinition(
        level, upgradeExp, sendCoins, expReward, health, castleDamage, speed
    )

    private val definitions = listOf(
        MobDefinition(
            "zombie", "Zombie",
            listOf(
                l(1,0,15,7,40.0,1.0,1.6),
                l(2,88,18,9,56.0,1.0,1.6),
                l(3,153,22,10,78.0,1.0,1.6),
                l(4,268,26,13,110.0,1.5,1.6),
                l(5,469,31,15,154.0,2.0,1.6)
            )
        ),
        MobDefinition(
            "spider", "Spider / Cave Spider",
            listOf(
                l(1,100,25,15,80.0,1.0,1.6),
                l(2,175,30,16,112.0,1.0,2.0),
                l(3,306,36,18,157.0,1.5,2.5),
                l(4,536,43,19,222.0,1.0,2.5),
                l(5,938,52,22,307.0,1.5,2.0)
            )
        ),
        MobDefinition(
            "pigman", "Zombie Pigman",
            listOf(
                l(1,150,100,22,150.0,1.0,2.0),
                l(2,263,120,24,210.0,1.0,2.0),
                l(3,459,144,25,297.0,1.0,2.0),
                l(4,804,173,25,412.0,1.0,2.0),
                l(5,1407,207,27,576.0,1.5,2.0)
            )
        ),
        MobDefinition(
            "skeleton", "Skeleton / Wither Skeleton",
            listOf(
                l(1,200,120,30,350.0,1.0,1.6),
                l(2,350,144,33,490.0,1.0,1.6),
                l(3,613,173,36,686.0,1.0,1.6),
                l(4,1072,225,40,960.0,1.5,1.6),
                l(5,1876,255,43,1345.0,2.0,1.6)
            )
        ),
        MobDefinition(
            "creeper", "Creeper",
            listOf(
                l(1,325,250,46,350.0,1.0,2.0),
                l(2,550,290,50,460.0,1.0,2.0),
                l(3,800,350,55,630.0,2.0,2.0),
                l(4,1300,420,61,900.0,3.0,2.0),
                l(5,1850,510,67,1250.0,4.0,2.0)
            ),
            MobSpecialDefinition(
                regeneration = true,
                notes = listOf("Kamikaze rebalance", "2021 replacement levels")
            )
        ),
        MobDefinition(
            "silverfish", "Silverfish / Endermite",
            listOf(
                l(1,125,38,18,120.0,1.0,1.6),
                l(2,230,50,20,170.0,1.0,1.6),
                l(3,355,75,26,225.0,1.0,1.6),
                l(4,690,170,35,310.0,1.5,2.0),
                l(5,1100,200,37,390.0,1.5,2.0)
            ),
            MobSpecialDefinition(notes = listOf("2021 replacement levels; immunity registry is separate"))
        ),
        MobDefinition(
            "blaze", "Blaze",
            listOf(
                l(1,500,500,105,410.0,1.0,1.6),
                l(2,875,600,111,574.0,1.0,1.6),
                l(3,1531,720,115,804.0,1.0,1.6),
                l(4,2680,864,121,1125.0,1.5,1.6),
                l(5,4689,1037,127,1575.0,2.0,1.6)
            ),
            MobSpecialDefinition(notes = listOf("Aerial"))
        ),
        MobDefinition(
            "witch", "Witch",
            listOf(
                l(1,300,150,37,300.0,1.0,1.6),
                l(2,525,165,39,405.0,1.0,1.6),
                l(3,918,182,42,547.0,1.0,1.6),
                l(4,1606,200,43,738.0,1.5,1.6),
                l(5,2810,220,45,996.0,2.0,1.6)
            ),
            MobSpecialDefinition(notes = listOf("Healing exact amount/rate/radius remain TruthGate unresolved"))
        ),
        MobDefinition(
            "slime", "Slime / Magma Cube",
            listOf(
                l(1,1000,1500,150,800.0,1.0,0.8),
                l(2,1750,1800,157,880.0,1.5,0.8),
                l(3,3063,2160,165,968.0,2.0,0.8),
                l(4,5359,2592,174,1065.0,2.5,0.8),
                l(5,9379,3110,183,1171.0,3.0,0.8)
            ),
            MobSpecialDefinition(shrinkOnKillMaxHealthLossFraction = 0.25)
        ),
        MobDefinition(
            "giant", "Giant",
            listOf(
                l(1,2500,4000,300,3000.0,10.0,0.8),
                l(2,4375,4500,306,4500.0,20.0,0.8),
                l(3,7656,5062,312,6750.0,30.0,0.8),
                l(4,13398,5695,318,10125.0,40.0,0.8),
                l(5,23447,6407,324,15187.0,50.0,0.8)
            ),
            MobSpecialDefinition(
                runThresholdHealthFraction = 0.20,
                notes = listOf("Regeneration exists; exact regen rate remains VIDEO_REQUIRED", "Quake immunity")
            )
        )
    ).associateBy { it.mobId }

    override fun all(): Map<String, MobDefinition> = definitions
}
