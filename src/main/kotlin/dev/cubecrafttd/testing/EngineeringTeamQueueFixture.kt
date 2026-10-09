package dev.cubecrafttd.testing

import dev.cubecrafttd.match.*
import dev.cubecrafttd.economy.PricingMode
import java.util.UUID

object EngineeringTeamQueueFixture {
    fun run(): List<FixtureResult> {
        val players = (1L..26L).map { UUID(109, it) }
        fun queue(count: Int) = EngineeringTeamQueueState(6).also { q -> players.take(count).forEach { q.join(it) } }
        val partial = queue(11)
        val busy = queue(12)
        val duplicate = busy.join(players.first())
        val fifo = queue(26)
        val first = fifo.matchIfArenaAvailable(true)!!
        val second = fifo.matchIfArenaAvailable(true)!!
        val failed = queue(15)
        val reserved = failed.matchIfArenaAvailable(true)!!
        failed.restoreMatchToFront(reserved)
        failed.restoreMatchToFront(reserved) // Retry must not duplicate players.
        val cancelled = queue(13)
        val cancelledMatch = cancelled.matchIfArenaAvailable(true)!!
        cancelled.restoreMatchToFront(cancelledMatch)
        cancelled.leave(players[4])
        val replacement = cancelled.matchIfArenaAvailable(true)!!
        val pruned = queue(12)
        val removed = pruned.retainEligible(players.take(12).toSet() - players[2])
        val source = players.take(12).toMutableList()
        val copied = EngineeringTeamQueueMatch(source, 6)
        source.clear()
        val armageddon = players.associateWith { HistoricalPregameArmageddonVoteOption.HORDE }.toMutableMap()
        val pricing = players.associateWith { HistoricalPregamePricingVoteOption.QUICK_START }.toMutableMap()
        first.players.forEachIndexed { index, player ->
            armageddon[player] = if (index < 7) HistoricalPregameArmageddonVoteOption.LIGHTNING else HistoricalPregameArmageddonVoteOption.WITHER
            pricing[player] = if (index < 7) HistoricalPregamePricingVoteOption.DOUBLE_INCOME else HistoricalPregamePricingVoteOption.NORMAL
        }
        val voted = PregameTeamStartResolver.resolve(first, armageddon, pricing, ArmageddonType.entries.toSet(), ArmageddonType.WITHER)
        val defaults = PregameTeamStartResolver.resolve(first, emptyMap(), emptyMap(), setOf(ArmageddonType.WITHER), ArmageddonType.WITHER)
        val preferences = players.associateWith { player -> dev.cubecrafttd.ui.PregamePreferenceState(
            settings=dev.cubecrafttd.ui.PlayerMatchSettings(20, digitalMobHealth=player.leastSignificantBits % 2 == 0L)) }
        val settings = PregameTeamStartResolver.settings(first, preferences)
        settings.getValue(players.first()).damageIndicators = true
        val tied = PregameTeamStartResolver.resolve(first,
            first.players.mapIndexed { i, p -> p to if (i < 6) HistoricalPregameArmageddonVoteOption.LIGHTNING else HistoricalPregameArmageddonVoteOption.HORDE }.toMap(),
            first.players.mapIndexed { i, p -> p to if (i < 6) HistoricalPregamePricingVoteOption.DOUBLE_INCOME else HistoricalPregamePricingVoteOption.QUICK_START }.toMap(),
            ArmageddonType.entries.toSet(), ArmageddonType.WITHER)
        return listOf(
            FixtureResult("team-queue-settings-copy-all-twelve-without-outsider-or-shared-mutation", settings.keys == first.players.toSet() &&
                !preferences.getValue(players.first()).settings.damageIndicators &&
                !settings.getValue(players[1]).damageIndicators && settings.all { (p, s) -> s.digitalMobHealth == (p.leastSignificantBits % 2 == 0L) }),
            FixtureResult("team-queue-rejects-zero-and-seven-player-team-profiles", listOf(0, 7).all { runCatching { EngineeringTeamQueueState(it) }.isFailure }),
            FixtureResult("team-queue-eleven-players-cannot-start-six-v-six", partial.matchIfArenaAvailable(true) == null && partial.snapshot() == players.take(11)),
            FixtureResult("team-queue-busy-arena-and-duplicate-join-preserve-full-roster", !duplicate.added && busy.matchIfArenaAvailable(false) == null && busy.snapshot() == players.take(12)),
            FixtureResult("team-queue-reserves-two-disjoint-fifo-balanced-rosters", first.players == players.take(12) && second.players == players.drop(12).take(12) && first.redPlayers == players.take(12).filterIndexed { i, _ -> i % 2 == 0 }.toSet() && first.bluePlayers.size == 6 && fifo.snapshot() == players.takeLast(2)),
            FixtureResult("team-queue-failed-start-restores-entire-roster-without-duplication", failed.snapshot() == players.take(15)),
            FixtureResult("team-queue-cancelled-countdown-replaces-leaver-before-new-start", replacement.players == players.take(13).filter { it != players[4] } && replacement.redPlayers.size == 6 && replacement.bluePlayers.size == 6 && cancelled.size() == 0),
            FixtureResult("team-queue-pruning-one-ineligible-player-prevents-incomplete-start", removed == setOf(players[2]) && pruned.matchIfArenaAvailable(true) == null && pruned.size() == 11),
            FixtureResult("team-queue-reserved-roster-detaches-from-caller-list", copied.players == players.take(12) && copied.redPlayers.size == 6),
            FixtureResult("team-queue-votes-use-all-twelve-and-ignore-fourteen-outsiders", voted.armageddon.selection.type == ArmageddonType.LIGHTNING && voted.pricing.pricingMode == PricingMode.DOUBLE_INCOME),
            FixtureResult("team-queue-no-vote-defaults-remain-random-and-normal", defaults.armageddon.resolution == HistoricalPregameArmageddonResolution.NO_VOTES_RANDOM && defaults.pricing.pricingMode == PricingMode.NORMAL),
            FixtureResult("team-queue-tied-twelve-player-votes-remain-explicit-engineering-fallbacks", tied.armageddon.resolution == HistoricalPregameArmageddonResolution.ENGINEERING_TIE_RANDOM_FALLBACK && tied.pricing.resolution == HistoricalPregamePricingResolution.ENGINEERING_TIE_NORMAL_FALLBACK),
            FixtureResult("team-queue-one-through-six-player-profiles-reserve-exact-balanced-rosters", (1..6).all { size ->
                val q = EngineeringTeamQueueState(size)
                players.take(size * 2 + 1).forEach { q.join(it) }
                val match = q.matchIfArenaAvailable(true)!!
                match.redPlayers.size == size && match.bluePlayers.size == size && q.snapshot() == listOf(players[size * 2])
            })
        )
    }
}
