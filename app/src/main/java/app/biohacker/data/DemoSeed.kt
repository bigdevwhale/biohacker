package app.biohacker.data

import app.biohacker.AppContainer
import app.biohacker.domain.Bag
import app.biohacker.domain.Catalog
import app.biohacker.domain.Engine
import app.biohacker.domain.Mark
import app.biohacker.domain.Metric
import app.biohacker.domain.Mod
import app.biohacker.domain.Reading
import app.biohacker.domain.State
import app.biohacker.domain.Status
import app.biohacker.domain.Sys
import app.biohacker.domain.Tag
import app.biohacker.domain.Trial
import app.biohacker.domain.day
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * The demo build's lab: five weeks of readings and trials — four mods in the firmware, one rejected,
 * three on trial (one with its protocol ready), a parcel waiting and a fresh week of patch notes.
 */
object DemoSeed {
    suspend fun seedIfEmpty(c: AppContainer) {
        val s = c.repo.refresh()
        if (s.readings.isNotEmpty() || s.trials.isNotEmpty()) return
        c.settings.update { it.copy(onboarded = true) }
        val st = build(c.catalog, System.currentTimeMillis(), ZoneId.systemDefault(), ru = java.util.Locale.getDefault().language == "ru")
        c.repo.seed(st.readings, st.marks, st.trials, st.bag)
    }

    /** A mod of [tag] from the catalogue, lightest and best-backed first. */
    private fun pick(cat: Catalog, tag: Tag, daily: Boolean = true): Mod? =
        cat.of(tag.sys).filter { it.tag == tag && (!daily || it.freq >= 5) }.minWithOrNull(compareBy({ it.rare }, { it.ev }, { it.load }))
            ?: cat.of(tag.sys).firstOrNull { it.tag == tag }

    fun build(cat: Catalog, now: Long, zone: ZoneId, ru: Boolean = true): State {
        val rnd = Random(11)
        val today = now.day(zone)
        // mod, start (days ago), days run, final status, how much it lifts which gauge
        data class Plan(val tag: Tag, val ago: Int, val status: Status, val lift: Map<Metric, Float>)
        val plans = listOf(
            Plan(Tag.LIGHT, 35, Status.ON, mapOf(Metric.SLEEP to .8f, Metric.ENERGY to .3f)),
            Plan(Tag.BREATH, 34, Status.ON, mapOf(Metric.CALM to .8f)),
            Plan(Tag.WATER, 30, Status.OFF, emptyMap()),
            Plan(Tag.WALK, 20, Status.ON, mapOf(Metric.ENERGY to .6f, Metric.MOOD to .3f)),
            Plan(Tag.DIGITAL, 19, Status.ON, mapOf(Metric.FOCUS to .7f)),
            Plan(Tag.DARK, 15, Status.RUN, mapOf(Metric.SLEEP to .7f)),
            Plan(Tag.STRENGTH, 8, Status.RUN, mapOf(Metric.ENERGY to .4f)),
            Plan(Tag.GRATEFUL, 3, Status.RUN, mapOf(Metric.MOOD to .5f)),
        )
        val mods = plans.mapNotNull { p -> pick(cat, p.tag)?.let { p to it } }
        val marks = ArrayList<Mark>()
        for ((p, m) in mods) {
            val start = today - p.ago
            val stop = if (p.status == Status.OFF) start + Engine.TRIAL_DAYS - 1 else today - 1
            val chance = (minOf(m.freq, 7) / 7.0) * (if (p.status == Status.OFF) .75 else .88)
            for (d in start..stop) if (rnd.nextDouble() < chance || d == start) marks += Mark(d, m.id)
            if (p.status == Status.ON || p.status == Status.RUN) if (rnd.nextDouble() < .6) marks += Mark(today, m.id)
        }
        val done = marks.map { it.day to it.mod }.toHashSet()
        val readings = ArrayList<Reading>()
        for (d in (today - 49)..(today - 1)) {
            if (d == today - 23) continue // one missed day, bridged by the reserve
            val v = Metric.entries.map { mt ->
                var x = 2.9 + (if (mt == Metric.SLEEP && (d == today - 12 || d == today - 9)) -1.4 else 0.0)
                for ((p, m) in mods) if ((d to m.id) in done || (d - 1 to m.id) in done) x += (p.lift[mt] ?: 0f)
                (x + rnd.nextDouble(-.8, .8)).roundToInt().coerceIn(1, 5)
            }
            readings += Reading(d, v)
        }
        val trials = mods.map { (p, m) ->
            val start = today - p.ago
            val end = if (p.status == Status.RUN) -1L else start + Engine.TRIAL_DAYS
            Trial(0, m.id, start, Engine.TRIAL_DAYS, p.status, end, null)
        }
        val tmp = State(readings, marks, trials, Bag())
        val scored = trials.map { t -> if (t.status == Status.RUN) t else t.copy(effect = Engine.effect(Engine.result(tmp, t, cat))) }
        val studied = cat.all.shuffled(Random(3)).take(64).map { it.id }.toSet() + mods.map { it.second.id }
        val found = cat.all.filter { it.rare >= 2 }.shuffled(Random(5)).take(14).map { it.id }.toSet()
        val myths = cat.myths.shuffled(Random(9)).take(7).map { it.id }.toSet()
        val fwTags = scored.filter { it.status == Status.ON }.mapNotNull { cat[it.mod]?.tag }.toSet()
        val bag = Bag(
            subject = if (ru) "Марат" else "Marat",
            focus = setOf(Sys.SLEEP, Sys.CALM, Sys.MOVE),
            studied = studied + found, found = found, myths = myths, bonus = 1,
            combos = Engine.combos(fwTags).map { it.id }.toSet(),
            notesSeen = Engine.monday(today) - 14,
            upkeep = mapOf("dentist" to today - 100, "bp" to today - 120, "blood" to today - 200, "eyes" to today - 400),
            coach = setOf("blueprint", "lab", "readings", "catalog", "log", "card", "result", "parcel"),
            start = now - 50 * 86_400_000L,
        )
        return State(readings, marks, scored, bag)
    }
}
