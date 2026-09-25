package dev.cubecrafttd.ui

enum class WeaponKind { SWORD, BOW }

data class WeaponTierDefinition(
    val weapon: WeaponKind,
    val tierId: String,
    val unlockCoins: Long,
    val damage: Double?,
    val averageCriticalDamage: Double? = null,
    val averageDamage: Double? = null,
    val evidenceStatus: String
)

data class WeaponAbilityDefinition(
    val weapon: WeaponKind,
    val abilityId: String,
    val historical2017RequiredHits: Int,
    val recommendedMatureRequiredHits: Int?,
    val matureStatus: String
)

data class AoEPotionDefinition(
    val potionId: String,
    val unlockExp: Long,
    val useCostCoins: Long,
    val effectLengthBlocks: Int? = null,
    val durationSeconds: Double? = null,
    val damagePerTickOrMeteor: Double? = null,
    val count: Int? = null,
    val totalDamage: Double? = null,
    val maxAffectedTroops: Int? = null,
    val baseline2020: Map<String,Double> =
        emptyMap(),
    val evidenceStatus: String
)

object RecommendedMatureBazaarDefinitions {
    val swordTiers = listOf(
        WeaponTierDefinition(
            WeaponKind.SWORD,
            "wood",0,8.0,11.0,null,
            "MATURE_TABLE"
        ),
        WeaponTierDefinition(
            WeaponKind.SWORD,
            "stone",1000,10.0,14.0,null,
            "MATURE_TABLE"
        ),
        WeaponTierDefinition(
            WeaponKind.SWORD,
            "iron",2000,12.0,16.5,null,
            "MATURE_TABLE"
        )
    )

    val bowTiers = listOf(
        WeaponTierDefinition(
            WeaponKind.BOW,
            "1",0,null,null,6.75,
            "MATURE_TABLE"
        ),
        WeaponTierDefinition(
            WeaponKind.BOW,
            "2",1000,null,null,11.0,
            "MATURE_TABLE"
        ),
        WeaponTierDefinition(
            WeaponKind.BOW,
            "3",2000,null,null,16.5,
            "MATURE_TABLE"
        )
    )

    /**
     * 2017 hit counts are historical anchors only.
     * 2021 says these were reduced, but the full final table is not recovered.
     */
    val abilities = listOf(
        WeaponAbilityDefinition(
            WeaponKind.SWORD,
            "freeze",15,null,
            "2021_REDUCED_EXACT_UNKNOWN"
        ),
        WeaponAbilityDefinition(
            WeaponKind.SWORD,
            "flame",20,null,
            "2021_REDUCED_EXACT_UNKNOWN"
        ),
        WeaponAbilityDefinition(
            WeaponKind.SWORD,
            "quake",20,null,
            "2021_REDUCED_EXACT_UNKNOWN"
        ),
        WeaponAbilityDefinition(
            WeaponKind.SWORD,
            "spikes",20,null,
            "2021_REDUCED_EXACT_UNKNOWN"
        ),
        WeaponAbilityDefinition(
            WeaponKind.BOW,
            "machine_gun",12,null,
            "2021_REDUCED_EXACT_UNKNOWN"
        ),
        WeaponAbilityDefinition(
            WeaponKind.BOW,
            "bazooka",15,null,
            "2021_REDUCED_EXACT_UNKNOWN"
        ),
        WeaponAbilityDefinition(
            WeaponKind.BOW,
            "shulker",10,null,
            "2021_REDUCED_EXACT_UNKNOWN"
        ),
        WeaponAbilityDefinition(
            WeaponKind.BOW,
            "sniper",12,null,
            "2021_REDUCED_EXACT_UNKNOWN"
        )
    )

    val aoePotions = listOf(
        AoEPotionDefinition(
            "freeze",
            unlockExp=200,
            useCostCoins=300,
            evidenceStatus="MATURE_COST_TABLE_EFFECT_EXACT_PENDING"
        ),
        AoEPotionDefinition(
            "inferno",
            unlockExp=300,
            useCostCoins=400,
            effectLengthBlocks=11,
            durationSeconds=8.0,
            baseline2020=mapOf(
                "durationSeconds" to 10.0,
                "damagePerSecond" to 42.18
            ),
            evidenceStatus="2021_DURATION_OVERRIDE"
        ),
        AoEPotionDefinition(
            "meteor",
            unlockExp=580,
            useCostCoins=1350,
            effectLengthBlocks=11,
            damagePerTickOrMeteor=8.88,
            count=20,
            totalDamage=177.6,
            evidenceStatus="MATURE_TABLE"
        ),
        AoEPotionDefinition(
            "zeus",
            unlockExp=450,
            useCostCoins=1200,
            effectLengthBlocks=17,
            maxAffectedTroops=4,
            baseline2020=mapOf(
                "damagePerBolt" to 57.0,
                "count" to 8.0,
                "totalDamage" to 456.0
            ),
            evidenceStatus="2021_BASE_DAMAGE_INCREASED_EXACT_UNKNOWN"
        ),
        AoEPotionDefinition(
            "speed",
            unlockExp=350,
            useCostCoins=800,
            effectLengthBlocks=17,
            evidenceStatus="MATURE_COST_TABLE"
        ),
        AoEPotionDefinition(
            "heal",
            unlockExp=300,
            useCostCoins=750,
            effectLengthBlocks=17,
            evidenceStatus="MATURE_COST_TABLE"
        )
    )

    fun potion(
        id: String
    ): AoEPotionDefinition =
        aoePotions.firstOrNull {
            it.potionId == id
        } ?: error("Unknown AoE potion $id")
}
