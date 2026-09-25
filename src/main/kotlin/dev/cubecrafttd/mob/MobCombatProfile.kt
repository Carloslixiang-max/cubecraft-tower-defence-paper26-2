package dev.cubecrafttd.mob

enum class TargetLayer { GROUND, AIR, BOSS }

enum class DamageKind {
    PHYSICAL,
    FIRE,
    LIGHTNING,
    POISON
}

enum class EffectKind {
    ICE_SLOW,
    STUN,
    KNOCKBACK
}

data class MobCombatProfile(
    val formId: String,
    val targetLayer: TargetLayer,
    val directVisibleToTowerTypes: Set<String>? = null,
    val damageImmunities: Set<DamageKind> = emptySet(),
    val effectImmunities: Set<EffectKind> = emptySet(),
    val immuneTowerTypes: Set<String> = emptySet(),
    val indirectSplashEligibleWhenDirectHidden: Boolean = false,
    val notes: List<String> = emptyList()
)

object RecommendedMatureMobCombatProfiles {
    private val profiles = listOf(
        MobCombatProfile("zombie", TargetLayer.GROUND),
        MobCombatProfile("spider", TargetLayer.GROUND),
        MobCombatProfile(
            "cave_spider",
            TargetLayer.GROUND,
            directVisibleToTowerTypes = setOf("ice", "mage"),
            indirectSplashEligibleWhenDirectHidden = true,
            notes = listOf("Most towers cannot directly see/target; Ice and Mage confirmed direct exceptions")
        ),
        MobCombatProfile(
            "pigman",
            TargetLayer.GROUND,
            damageImmunities = setOf(DamageKind.FIRE, DamageKind.LIGHTNING)
        ),
        MobCombatProfile("skeleton", TargetLayer.GROUND),
        MobCombatProfile(
            "wither_skeleton",
            TargetLayer.GROUND,
            damageImmunities = setOf(DamageKind.FIRE, DamageKind.LIGHTNING)
        ),
        MobCombatProfile(
            "creeper",
            TargetLayer.GROUND,
            effectImmunities = setOf(EffectKind.ICE_SLOW),
            notes = listOf("Regeneration retained in runtime definition; exact 2021 regen behavior remains evidence-sensitive")
        ),
        MobCombatProfile("silverfish", TargetLayer.AIR),
        MobCombatProfile(
            "endermite",
            TargetLayer.AIR,
            damageImmunities = setOf(DamageKind.FIRE),
            immuneTowerTypes = setOf("mage"),
            notes = listOf("RECOMMENDED_MATURE_POST_2021: Mage immunity")
        ),
        MobCombatProfile(
            "witch",
            TargetLayer.GROUND,
            damageImmunities = setOf(
                DamageKind.POISON,
                DamageKind.FIRE,
                DamageKind.LIGHTNING
            ),
            immuneTowerTypes = setOf("poison", "mage", "zeus")
        ),
        MobCombatProfile("slime", TargetLayer.GROUND),
        MobCombatProfile(
            "magma_cube",
            TargetLayer.GROUND,
            damageImmunities = setOf(DamageKind.FIRE),
            immuneTowerTypes = setOf("mage")
        ),
        MobCombatProfile(
            "blaze",
            TargetLayer.AIR,
            damageImmunities = setOf(DamageKind.FIRE)
        ),
        MobCombatProfile(
            "giant",
            TargetLayer.GROUND,
            effectImmunities = setOf(
                EffectKind.ICE_SLOW,
                EffectKind.STUN,
                EffectKind.KNOCKBACK
            )
        ),
        MobCombatProfile(
            "wither",
            TargetLayer.BOSS,
            immuneTowerTypes = setOf(
                "ice", "mage", "necromancer", "quake", "leach"
            ),
            notes = listOf(
                "Armageddon boss handler; castle contact is instant defeat",
                "AoE potion immunity belongs to potion/boss rule layer"
            )
        )
    ).associateBy { it.formId }

    fun get(formId: String): MobCombatProfile =
        profiles[formId] ?: error("Unknown combat form $formId")

    fun all(): Map<String, MobCombatProfile> = profiles
}

data class TowerAttackCapability(
    val towerType: String,
    val targetLayers: Set<TargetLayer>,
    val damageKinds: Set<DamageKind>,
    val effectKinds: Set<EffectKind> = emptySet()
)

object MobCombatEligibility {
    fun directTargetable(
        profile: MobCombatProfile,
        capability: TowerAttackCapability
    ): Boolean {
        if (profile.targetLayer !in capability.targetLayers) return false
        if (capability.towerType in profile.immuneTowerTypes) return false
        val visibility = profile.directVisibleToTowerTypes
        if (visibility != null && capability.towerType !in visibility) return false
        return true
    }

    fun canApplyDirectDamage(
        profile: MobCombatProfile,
        capability: TowerAttackCapability
    ): Boolean {
        if (!directTargetable(profile, capability)) return false
        if (capability.damageKinds.any { it in profile.damageImmunities }) {
            // A multi-kind attack is allowed if it has at least one non-immune component.
            if (capability.damageKinds.all { it in profile.damageImmunities }) return false
        }
        return capability.damageKinds.any { it !in profile.damageImmunities }
    }

    fun canApplyIndirectSplashDamage(
        profile: MobCombatProfile,
        capability: TowerAttackCapability
    ): Boolean {
        if (profile.targetLayer !in capability.targetLayers) return false
        if (capability.towerType in profile.immuneTowerTypes) return false
        if (profile.directVisibleToTowerTypes != null &&
            !profile.indirectSplashEligibleWhenDirectHidden
        ) return false
        return capability.damageKinds.any { it !in profile.damageImmunities }
    }

    fun canApplyEffect(
        profile: MobCombatProfile,
        effect: EffectKind
    ): Boolean = effect !in profile.effectImmunities
}
