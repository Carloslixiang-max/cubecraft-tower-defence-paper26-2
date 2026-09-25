package dev.cubecrafttd.ui

enum class BazaarSection {
    GOLDMINE,
    SWORD,
    BOW,
    POTIONS
}

data class BazaarEntryDefinition(
    val actionId: String,
    val section: BazaarSection,
    val coinCost: Long? = null,
    val expCost: Long? = null,
    val evidenceStatus: UiEvidenceStatus
)

/**
 * Only entries with recovered prices belong here.
 * Weapon/potion exact tables remain separate until their Mature values are
 * fully normalized.
 */
object BazaarKnownEntries {
    val goldmineUpgrades = listOf(
        BazaarEntryDefinition(
            "goldmine:2",BazaarSection.GOLDMINE,
            expCost=125,
            evidenceStatus=UiEvidenceStatus.MATURE_CONTEXT
        ),
        BazaarEntryDefinition(
            "goldmine:3",BazaarSection.GOLDMINE,
            expCost=350,
            evidenceStatus=UiEvidenceStatus.MATURE_CONTEXT
        ),
        BazaarEntryDefinition(
            "goldmine:4",BazaarSection.GOLDMINE,
            expCost=1000,
            evidenceStatus=UiEvidenceStatus.MATURE_CONTEXT
        ),
        BazaarEntryDefinition(
            "goldmine:5",BazaarSection.GOLDMINE,
            expCost=2500,
            evidenceStatus=UiEvidenceStatus.MATURE_CONTEXT
        ),
        BazaarEntryDefinition(
            "goldmine:6",BazaarSection.GOLDMINE,
            expCost=6000,
            evidenceStatus=UiEvidenceStatus.MATURE_CONTEXT
        )
    )
}
