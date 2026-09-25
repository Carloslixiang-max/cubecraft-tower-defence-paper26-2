package dev.cubecrafttd.mob

data class MobCombatRuntimeBindings(
    val lethalResolver: MobLethalHitResolver
) {
    companion object {
        fun fromResolvedNormal(
            slime: SlimeLethalConfig
        ): MobCombatRuntimeBindings =
            MobCombatRuntimeBindings(
                MobLethalHitResolver(slime)
            )
    }
}
