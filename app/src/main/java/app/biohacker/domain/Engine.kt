package app.biohacker.domain

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.random.Random

/** How a trial came out on its main gauge. */
enum class Verdict { WORKS, FLAT, WORSE, THIN }

/**
 * A trial's protocol: the gauges on days the mod was done (or the day after) against the days it wasn't,
 * including the two weeks before the trial began.
 */
data class Result(val done: Int, val target: Int, val base: List<Float?>, val on: List<Float?>, val nBase: Int, val nOn: Int, val main: Metric) {
    val adherence get() = if (target == 0) 0f else (done.toFloat() / target).coerceAtMost(1f)
    fun delta(m: Metric): Float? { val a = on[m.ordinal]; val b = base[m.ordinal]; return if (a == null || b == null) null else a - b }
    val verdict: Verdict get() {
        val d = delta(main)
        if (d == null || nOn < 4 || nBase < 3 || adherence < .5f) return Verdict.THIN
        val best = Metric.entries.mapNotNull { delta(it) }.maxOrNull() ?: 0f
        return when {
            d >= .35f || (best >= .5f && d >= 0f) -> Verdict.WORKS
            d <= -.35f -> Verdict.WORSE
            else -> Verdict.FLAT
        }
    }
}

/** One line of a week's patch notes; the words come later, in the UI. */
data class Note(val kind: Kind, val mod: String? = null, val combo: String? = null, val metric: Metric? = null, val value: Float = 0f, val n: Int = 0) {
    enum class Kind { INSTALL, REJECT, UNSURE, DROP, START, COMBO, UP, DOWN, GLITCH, STABLE, QUIET }
}

data class Week(val monday: Long, val major: Int, val minor: Int, val build: Int, val notes: List<Note>)

/**
 * The rules of the lab, as pure functions of the state:
 * - a mod is tried for 14 days and only the subject decides whether it goes into the firmware;
 * - the trials running at once may not weigh more than the power, which grows with the firmware;
 * - the version is the firmware count, the build the days of readings;
 * - every day brings one sealed parcel, leaning to the systems the subject cares about.
 */
object Engine {
    const val TRIAL_DAYS = 14
    const val EXTEND_DAYS = 7
    /** How many studied cards earn a bonus parcel. */
    const val STUDY_BONUS = 10

    fun capacity(s: State) = 3 + minOf(5, s.firmware.size / 2)

    fun used(s: State, cat: Catalog) = s.running.sumOf { cat[it.mod]?.load ?: 1 }

    fun free(s: State, cat: Catalog) = capacity(s) - used(s, cat)

    fun installed(s: State, mod: String) = s.lastTrial(mod)?.status == Status.ON

    fun running(s: State, mod: String) = s.lastTrial(mod)?.status == Status.RUN

    /** Why a mod can't go on trial now, or null when it can. */
    fun block(s: State, cat: Catalog, m: Mod): Block? = when {
        running(s, m.id) -> Block.RUNNING
        installed(s, m.id) -> Block.INSTALLED
        used(s, cat) + m.load > capacity(s) -> Block.POWER
        else -> null
    }

    enum class Block { RUNNING, INSTALLED, POWER }

    fun major(n: Int) = 1 + n / 10
    fun minor(n: Int) = n % 10

    /**
     * Days in a row with readings, ending today (or yesterday while today's are not in yet). One missed day
     * a week is bridged by the reserve power, so a single slip doesn't reset the count.
     */
    fun uptime(s: State, today: Long): Int {
        if (s.readings.isEmpty()) return 0
        val first = s.readings.minOf { it.day }
        var d = if (s.reading(today) != null) today else today - 1
        var n = 0
        var bridged = Long.MAX_VALUE
        while (d >= first) {
            if (s.reading(d) != null) { n++; d--; continue }
            if (n > 0 && bridged - d >= 7 && s.reading(d - 1) != null) { bridged = d; d--; continue }
            break
        }
        return n
    }

    fun coverage(s: State, cat: Catalog): Set<Sys> = s.firmware.mapNotNull { cat[it.mod]?.sys }.toSet()

    /** How well an installed (or running) mod was kept over the last week, 0..1. */
    fun integrity(s: State, m: Mod, today: Long): Float {
        val from = maxOf(today - 6, s.lastTrial(m.id)?.start ?: today - 6)
        val days = (today - from + 1).toInt().coerceAtLeast(1)
        val want = ceil(minOf(m.freq, 7) * days / 7.0).coerceAtLeast(1.0)
        val got = (from..today).count { s.done(it, m.id) }
        return (got / want).toFloat().coerceAtMost(1f)
    }

    /** An installed mod left alone for a week fades on the blueprint. */
    fun faded(s: State, m: Mod, today: Long): Boolean {
        val t = s.lastTrial(m.id) ?: return false
        return t.status == Status.ON && today - t.end >= 7 && integrity(s, m, today) < .4f
    }

    fun firmwareIntegrity(s: State, cat: Catalog, today: Long): Float? {
        val ms = s.firmware.mapNotNull { cat[it.mod] }
        if (ms.isEmpty()) return null
        return ms.map { integrity(s, it, today) }.average().toFloat()
    }

    fun result(s: State, t: Trial, cat: Catalog): Result {
        val m = cat[t.mod]
        val main = m?.sys?.metric ?: Metric.ENERGY
        val freq = m?.freq ?: 7
        val last = minOf(t.last, if (t.end >= 0) t.end else t.last)
        val trialDays = (t.start..last)
        val done = trialDays.count { s.done(it, t.mod) }
        val target = ceil(freq * (last - t.start + 1) / 7.0).toInt().coerceAtLeast(1)
        val on = ArrayList<Reading>()
        val base = ArrayList<Reading>()
        for (r in s.readings) {
            when {
                r.day in (t.start - 14) until t.start -> base += r
                r.day in t.start..(last + 1) -> if (s.done(r.day, t.mod) || s.done(r.day - 1, t.mod)) on += r else if (r.day <= last) base += r
            }
        }
        fun avg(rs: List<Reading>) = Metric.entries.map { mt -> if (rs.isEmpty()) null else rs.map { it[mt] }.average().toFloat() }
        return Result(done, target, avg(base), avg(on), base.size, on.size, main)
    }

    private fun seed(day: Long, salt: String) = (day * 1_000_003 + salt.hashCode()).hashCode()

    private fun weight(m: Mod, focus: Set<Sys>) = (when (m.rare) { 1 -> 3.0; 2 -> 2.0; else -> 1.5 }) * (if (m.sys in focus) 2.0 else 1.0)

    /** Draws a sealed card: now and then a broken mod, otherwise a protocol not yet seen. */
    fun draw(s: State, cat: Catalog, rnd: Random, day: Long): Parcel? {
        val myths = cat.myths.filter { it.id !in s.bag.myths }
        if (myths.isNotEmpty() && rnd.nextDouble() < .15) return Parcel(day, myths[rnd.nextInt(myths.size)].id, true)
        val tried = s.trials.map { it.mod }.toSet()
        var pool = cat.all.filter { it.id !in s.bag.found && it.id !in s.bag.studied && it.id !in tried }
        if (pool.isEmpty()) pool = cat.all.filter { it.id !in s.bag.found }
        if (pool.isEmpty()) return myths.takeIf { it.isNotEmpty() }?.let { Parcel(day, it[rnd.nextInt(it.size)].id, true) }
        val total = pool.sumOf { weight(it, s.bag.focus) }
        var x = rnd.nextDouble() * total
        for (m in pool) { x -= weight(m, s.bag.focus); if (x <= 0) return Parcel(day, m.id, false) }
        return Parcel(day, pool.last().id, false)
    }

    /** Today's parcel: kept if already drawn for [day]. */
    fun parcel(s: State, cat: Catalog, day: Long): Parcel? {
        s.bag.parcel?.let { if (it.day == day) return it }
        return draw(s, cat, Random(seed(day, s.bag.subject)), day)
    }

    fun open(b: Bag, p: Parcel): Bag = if (p.myth) b.copy(myths = b.myths + p.card) else b.copy(found = b.found + p.card, studied = b.studied + p.card)

    /** A card read in the catalogue; every tenth new one earns a bonus parcel. */
    fun study(b: Bag, id: String): Bag {
        if (id in b.studied) return b
        val n = b.studied.size + 1
        return b.copy(studied = b.studied + id, bonus = b.bonus + if (n % STUDY_BONUS == 0) 1 else 0)
    }

    fun combos(tags: Set<Tag>) = Combos.all.filter { c -> c.tags.all { it in tags } }

    fun combos(s: State, cat: Catalog) = combos(s.firmware.mapNotNull { cat[it.mod]?.tag }.toSet())

    /** For a day of low gauges: three short, well-backed mods for whatever sagged. */
    fun emergency(cat: Catalog, r: Reading): List<Mod> {
        val low = r.low.sortedBy { r[it] }
        if (low.isEmpty()) return emptyList()
        val rnd = Random(seed(r.day, "patch"))
        val out = ArrayList<Mod>()
        var i = 0
        while (out.size < 3 && i < 12) {
            val m = low[i % low.size]
            val pool = Sys.feeding(m).flatMap { cat.of(it) }.filter { it.min <= 5 && it.ev <= 'B' && it !in out }
            if (pool.isNotEmpty()) out += pool[rnd.nextInt(pool.size)]
            i++
        }
        return out
    }

    /** First mods to try: the lightest, best-backed basics of the chosen systems. */
    fun starters(cat: Catalog, focus: Set<Sys>): List<Mod> {
        val systems = (focus.ifEmpty { setOf(Sys.SLEEP, Sys.MOVE, Sys.CALM) }).sortedBy { it.ordinal }
        val picks = ArrayList<Mod>()
        var k = 0
        while (picks.size < 3 && k < 9) {
            for (sys in systems) {
                if (picks.size >= 3) break
                val m = cat.of(sys).filter { it !in picks }.sortedWith(compareBy({ it.ev }, { it.load }, { it.rare }, { it.min })).firstOrNull() ?: continue
                picks += m
            }
            k++
        }
        return picks
    }

    /** Today's work: running trials first, then the firmware, in order through the day. */
    fun today(s: State, cat: Catalog): List<Pair<Mod, Trial>> =
        (s.running + s.firmware).mapNotNull { t -> cat[t.mod]?.let { it to t } }
            .sortedWith(compareBy({ it.first.at.ordinal }, { if (it.second.status == Status.RUN) 0 else 1 }))

    /** Times a mod was done this week (Monday first), for mods that aren't daily. */
    fun thisWeek(s: State, mod: String, today: Long): Int {
        val mon = LocalDate.ofEpochDay(today).with(DayOfWeek.MONDAY).toEpochDay()
        return (mon..today).count { s.done(it, mod) }
    }

    fun monday(day: Long) = LocalDate.ofEpochDay(day).with(DayOfWeek.MONDAY).toEpochDay()

    /** Patch notes for every finished week since the start, newest first. */
    fun weeks(s: State, cat: Catalog, today: Long): List<Week> {
        if (s.bag.start == 0L && s.readings.isEmpty() && s.trials.isEmpty()) return emptyList()
        val first = listOfNotNull(s.readings.minOfOrNull { it.day }, s.trials.minOfOrNull { it.start }).minOrNull() ?: return emptyList()
        val out = ArrayList<Week>()
        var mon = monday(first)
        val thisMon = monday(today)
        while (mon < thisMon) { out += week(s, cat, mon); mon += 7 }
        return out.reversed()
    }

    fun week(s: State, cat: Catalog, mon: Long): Week {
        val sun = mon + 6
        val range = mon..sun
        val notes = ArrayList<Note>()
        val ended = s.trials.filter { it.end in range }
        for (t in ended) notes += Note(when (t.status) { Status.ON -> Note.Kind.INSTALL; Status.OFF -> Note.Kind.REJECT; Status.UNSURE -> Note.Kind.UNSURE; else -> Note.Kind.DROP }, mod = t.mod)
        for (t in s.trials.filter { it.start in range && it.end !in range }) notes += Note(Note.Kind.START, mod = t.mod)
        fun fw(upTo: Long) = s.trials.groupBy { it.mod }.values.mapNotNull { ts -> ts.lastOrNull { it.start <= upTo && (it.end in 0..upTo || it.status == Status.RUN) } }
            .filter { it.status == Status.ON && it.end in 0..upTo }
        val before = combos(fw(mon - 1).mapNotNull { cat[it.mod]?.tag }.toSet()).map { it.id }.toSet()
        val nowFw = fw(sun)
        for (c in combos(nowFw.mapNotNull { cat[it.mod]?.tag }.toSet())) if (c.id !in before) notes += Note(Note.Kind.COMBO, combo = c.id)
        val wk = s.readings.filter { it.day in range }
        val prev = s.readings.filter { it.day in (mon - 7)..(mon - 1) }
        if (wk.size >= 3 && prev.size >= 3) {
            val d = Metric.entries.associateWith { m -> (wk.map { it[m] }.average() - prev.map { it[m] }.average()).toFloat() }
            d.maxByOrNull { it.value }?.takeIf { it.value >= .3f }?.let { notes += Note(Note.Kind.UP, metric = it.key, value = it.value) }
            d.minByOrNull { it.value }?.takeIf { it.value <= -.4f }?.let { notes += Note(Note.Kind.DOWN, metric = it.key, value = it.value) }
        }
        val glitches = wk.filter { it.glitch }
        if (glitches.size >= 2) {
            val m = Metric.entries.maxBy { mt -> glitches.count { it[mt] <= 2 } }
            notes += Note(Note.Kind.GLITCH, metric = m, n = glitches.count { it[m] <= 2 })
        }
        val fwMods = nowFw.mapNotNull { cat[it.mod] }
        if (fwMods.isNotEmpty()) {
            val integ = fwMods.map { m ->
                val want = ceil(minOf(m.freq, 7).toDouble()).coerceAtLeast(1.0)
                ((range).count { s.done(it, m.id) } / want).coerceAtMost(1.0)
            }.average().toFloat()
            if (integ >= .75f) notes += Note(Note.Kind.STABLE, value = integ)
        }
        if (notes.isEmpty()) notes += Note(Note.Kind.QUIET)
        val n = nowFw.size
        val build = s.readings.count { it.day <= sun }
        return Week(mon, major(n), minor(n), build, notes)
    }

    /** Days until a maintenance item is due (negative when overdue), or null if it was never done. */
    fun upkeepDue(b: Bag, u: Upkeep, today: Long): Long? {
        val last = b.upkeep[u.id] ?: return null
        val due = LocalDate.ofEpochDay(last).plusMonths(u.months.toLong()).toEpochDay()
        return due - today
    }

    fun upkeepOverdue(b: Bag, today: Long) = Upkeep.all.count { (upkeepDue(b, it, today) ?: -1) < 0 }

    /** The verdict a trial would get, as the effect stored with it. */
    fun effect(r: Result) = r.delta(r.main)
}
