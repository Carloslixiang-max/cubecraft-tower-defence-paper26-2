package dev.cubecrafttd.testing

import dev.cubecrafttd.arena.*
import dev.cubecrafttd.mob.*
import dev.cubecrafttd.truth.*
import java.util.UUID

object MobLethalAndSupportRuntimeFixture {
    private fun <T> fallback(v:T)=
        ResolvedTruth(
            v,
            ResolutionSource
                .ENGINEERING_FALLBACK
        )

    fun run():List<FixtureResult> {
        val owner=UUID.fromString(
            "00000000-0000-0000-0000-000000017001"
        )

        val slime=MobRuntimeState(
            MobIdentity(
                MobInstanceId(1),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000017011"
                ),
                "slime",owner,
                TeamId.RED,1
            ),
            MobRouteState("red-route",0,0.0,1.0),
            MobCombatState(
                0.0,800.0,
                MobLifecycleState.MOVING
            )
        )
        val lethal=MobLethalHitResolver(
            SlimeLethalConfig(
                fallback(2)
            )
        )
        val shrink1=lethal.resolve(
            slime,
            DamageSourceIdentity
                .PlayerSword(owner)
        )
        slime.combat.health=0.0
        val shrink2=lethal.resolve(
            slime,
            DamageSourceIdentity
                .PlayerSword(owner)
        )
        slime.combat.health=0.0
        val final=lethal.resolve(
            slime,
            DamageSourceIdentity
                .PlayerSword(owner)
        )

        val context=ArenaContext(
            ArenaId("support"),
            UUID.fromString(
                "00000000-0000-0000-0000-000000017100"
            ),
            TestingMapFactory.minimal()
        ).also {
            it.state=ArenaState.RUNNING
        }
        val witch=MobRuntimeState(
            MobIdentity(
                MobInstanceId(2),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000017012"
                ),
                "witch",owner,
                TeamId.RED,1
            ),
            MobRouteState("red-route",0,0.0,2.0),
            MobCombatState(
                300.0,300.0,
                MobLifecycleState.MOVING
            )
        )
        val zombie=MobRuntimeState(
            MobIdentity(
                MobInstanceId(3),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000017013"
                ),
                "zombie",owner,
                TeamId.RED,1
            ),
            MobRouteState("red-route",0,0.0,3.0),
            MobCombatState(
                50.0,100.0,
                MobLifecycleState.MOVING
            )
        )
        val giant=MobRuntimeState(
            MobIdentity(
                MobInstanceId(4),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000017014"
                ),
                "giant",owner,
                TeamId.RED,1
            ),
            MobRouteState("red-route",0,0.0,4.0),
            MobCombatState(
                100.0,200.0,
                MobLifecycleState.MOVING
            )
        )
        val creeper=MobRuntimeState(
            MobIdentity(
                MobInstanceId(5),
                UUID.fromString(
                    "00000000-0000-0000-0000-000000017015"
                ),
                "creeper",owner,
                TeamId.RED,1
            ),
            MobRouteState("red-route",0,0.0,5.0),
            MobCombatState(
                100.0,200.0,
                MobLifecycleState.MOVING
            )
        )
        listOf(witch,zombie,giant,creeper)
            .forEach(
                context.entityIndex::registerMob
            )

        val phase=MobSupportTickPhase(
            witch=WitchHealConfig(
                fallback(0.10),
                fallback(1L),
                fallback(10.0),
                fallback(8),
                fallback(
                    WitchHealMode
                        .MAX_HEALTH_FRACTION
                )
            ),
            creeperRegen=
                PassiveRegenConfig(
                    fallback(20.0)
                ),
            giantRegen=
                GiantRegenConfig(
                    fallback(20.0)
                ),
            geometry=
                MobSupportGeometryProvider {
                    from,to ->
                    if(
                        from==witch.identity.entityUuid &&
                        to==zombie.identity.entityUuid
                    ) 2.0 else 20.0
                }
        )

        // first tick arms Witch; second tick performs burst
        context.advanceSyntheticTick(1)
        phase.tick(context)
        context.advanceSyntheticTick(1)
        phase.tick(context)

        return listOf(
            FixtureResult(
                "slime-lethal-shrink-refills-reduced-max",
                shrink1.kind==
                    MobLethalOutcomeKind.SHRUNK &&
                    shrink2.kind==
                        MobLethalOutcomeKind.SHRUNK &&
                    final.kind==
                        MobLethalOutcomeKind
                            .FINAL_DEATH &&
                    slime.combat.maxHealth==
                        450.0 &&
                    slime.combat.lifecycle==
                        MobLifecycleState.DEAD
            ),
            FixtureResult(
                "witch-mature-percent-heal",
                zombie.combat.health==60.0
            ),
            FixtureResult(
                "passive-regen-ticks",
                giant.combat.health==102.0 &&
                    creeper.combat.health==102.0
            ),
            FixtureResult(
                "witch-does-not-heal-witch",
                witch.combat.health==300.0
            )
        )
    }
}
