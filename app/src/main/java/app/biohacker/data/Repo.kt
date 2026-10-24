package app.biohacker.data

import app.biohacker.domain.Bag
import app.biohacker.domain.Catalog
import app.biohacker.domain.Engine
import app.biohacker.domain.Mark
import app.biohacker.domain.Mod
import app.biohacker.domain.Parcel
import app.biohacker.domain.Reading
import app.biohacker.domain.State
import app.biohacker.domain.Status
import app.biohacker.domain.Sys
import app.biohacker.domain.Trial
import app.biohacker.domain.day
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId
import kotlin.random.Random

/** The lab on disk, and the live picture of it in [state]. */
class Repo(db: LabDb, private val clock: () -> Long = System::currentTimeMillis, private val zone: () -> ZoneId = ZoneId::systemDefault) {
    private val dao = db.dao()
    private val lock = Mutex()
    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state

    fun today() = clock().day(zone())

    suspend fun refresh(): State = lock.withLock { load().also { _state.value = it } }

    private suspend fun load(): State = State(
        dao.readings().map { Reading(it.day, listOf(it.sleep, it.energy, it.mood, it.focus, it.calm)) },
        dao.marks().map { Mark(it.day, it.mod) },
        dao.trials().map { Trial(it.id, it.mod, it.start, it.days, Status.of(it.status), it.end, it.effect) },
        Bags.read(dao.meta().associate { it.k to it.v }),
    )

    private suspend fun current(): State = _state.value ?: load()

    private suspend fun write(block: suspend (State) -> Bag?) = lock.withLock {
        val s = current()
        val b = block(s)
        if (b != null && b != s.bag) dao.meta(Bags.write(b))
        _state.value = load()
    }

    private suspend fun change(f: (State) -> Bag) = write { f(it) }

    suspend fun reading(day: Long, v: List<Int>) = write {
        dao.reading(ReadingRow(day, v[0], v[1], v[2], v[3], v[4], clock())); null
    }

    suspend fun mark(day: Long, mod: String, on: Boolean) = write {
        if (on) dao.mark(MarkRow(day, mod)) else dao.unmark(day, mod); null
    }

    /** Puts a mod on trial from today, if there is the power for it. */
    suspend fun start(cat: Catalog, m: Mod): Boolean {
        var ok = false
        write { s ->
            if (Engine.block(s, cat, m) == null) { dao.insert(TrialRow(mod = m.id, start = today(), days = Engine.TRIAL_DAYS, status = Status.RUN.key, end = -1, effect = null)); ok = true }
            Engine.study(s.bag, m.id).let { if (it.start == 0L) it.copy(start = clock()) else it }
        }
        return ok
    }

    /** Ends a trial: into the firmware, rejected, unclear or dropped early. A finished trial earns a bonus parcel. */
    suspend fun finish(cat: Catalog, t: Trial, status: Status) = write { s ->
        val r = Engine.result(s, t, cat)
        dao.trial(TrialRow(t.id, t.mod, t.start, t.days, status.key, today(), Engine.effect(r)))
        if (status == Status.DROP) null else s.bag.copy(bonus = s.bag.bonus + 1)
    }

    suspend fun extend(t: Trial) = write {
        dao.trial(TrialRow(t.id, t.mod, t.start, t.days + Engine.EXTEND_DAYS, Status.RUN.key, -1, null)); null
    }

    /** Takes an installed mod out of the firmware. */
    suspend fun retire(t: Trial) = write {
        dao.trial(TrialRow(t.id, t.mod, t.start, t.days, Status.RETIRED.key, t.end, t.effect)); null
    }

    /** Makes sure today's parcel is drawn and kept, so it can't be rerolled by reopening the app. */
    suspend fun parcel(cat: Catalog): Parcel? {
        var out: Parcel? = null
        change { s -> Engine.parcel(s, cat, today()).also { out = it }?.let { s.bag.copy(parcel = it) } ?: s.bag }
        return out
    }

    suspend fun openParcel(cat: Catalog): Parcel? {
        var out: Parcel? = null
        change { s ->
            val p = Engine.parcel(s, cat, today()) ?: return@change s.bag
            if (p.opened) return@change s.bag
            out = p
            Engine.open(s.bag, p).copy(parcel = p.copy(opened = true))
        }
        return out
    }

    suspend fun openBonus(cat: Catalog): Parcel? {
        var out: Parcel? = null
        change { s ->
            if (s.bag.bonus <= 0) return@change s.bag
            val p = Engine.draw(s, cat, Random(clock()), today()) ?: return@change s.bag
            out = p
            Engine.open(s.bag, p).copy(bonus = s.bag.bonus - 1)
        }
        return out
    }

    suspend fun study(id: String) = change { Engine.study(it.bag, id) }

    suspend fun subject(name: String) = change { it.bag.copy(subject = name.trim().take(24)) }
    suspend fun focus(f: Set<Sys>) = change { it.bag.copy(focus = f) }
    suspend fun begin() = change { if (it.bag.start == 0L) it.bag.copy(start = clock()) else it.bag }
    suspend fun combosSeen(ids: Set<String>) = change { it.bag.copy(combos = it.bag.combos + ids) }
    suspend fun notesSeen(monday: Long) = change { it.bag.copy(notesSeen = maxOf(it.bag.notesSeen, monday)) }
    suspend fun upkeep(id: String, day: Long?) = change { if (day == null) it.bag.copy(upkeep = it.bag.upkeep - id) else it.bag.copy(upkeep = it.bag.upkeep + (id to day)) }
    suspend fun coach(screen: String) = change { it.bag.copy(coach = it.bag.coach + screen) }
    suspend fun coachReset() = change { it.bag.copy(coach = emptySet()) }

    /** For the demo: the whole lab straight in. */
    suspend fun seed(readings: List<Reading>, marks: List<Mark>, trials: List<Trial>, bag: Bag) = lock.withLock {
        for (r in readings) dao.reading(ReadingRow(r.day, r.v[0], r.v[1], r.v[2], r.v[3], r.v[4], clock()))
        for (m in marks) dao.mark(MarkRow(m.day, m.mod))
        for (t in trials) dao.insert(TrialRow(mod = t.mod, start = t.start, days = t.days, status = t.status.key, end = t.end, effect = t.effect))
        dao.meta(Bags.write(bag)); _state.value = load()
    }
}

/** The bag kept as plain key–value rows. */
object Bags {
    private fun set(v: String?) = v.orEmpty().split(',').filter { it.isNotEmpty() }.toSet()

    fun read(m: Map<String, String>): Bag = Bag(
        subject = m["subject"].orEmpty(),
        focus = set(m["focus"]).mapNotNull { Sys.of(it) }.toSet(),
        studied = set(m["studied"]),
        found = set(m["found"]),
        myths = set(m["myths"]),
        parcel = m["parcel"]?.split(':')?.takeIf { it.size == 4 }?.let { c -> c[0].toLongOrNull()?.let { Parcel(it, c[1], c[2] == "1", c[3] == "1") } },
        bonus = m["bonus"]?.toIntOrNull() ?: 0,
        combos = set(m["combos"]),
        notesSeen = m["notesSeen"]?.toLongOrNull() ?: -1,
        upkeep = set(m["upkeep"]).mapNotNull { p -> p.substringAfter('=').toLongOrNull()?.let { p.substringBefore('=') to it } }.toMap(),
        coach = set(m["coach"]),
        start = m["start"]?.toLongOrNull() ?: 0,
    )

    fun write(b: Bag): List<MetaRow> = listOf(
        "subject" to b.subject,
        "focus" to b.focus.joinToString(",") { it.key },
        "studied" to b.studied.joinToString(","),
        "found" to b.found.joinToString(","),
        "myths" to b.myths.joinToString(","),
        "parcel" to (b.parcel?.let { "${it.day}:${it.card}:${if (it.myth) 1 else 0}:${if (it.opened) 1 else 0}" } ?: ""),
        "bonus" to b.bonus.toString(),
        "combos" to b.combos.joinToString(","),
        "notesSeen" to b.notesSeen.toString(),
        "upkeep" to b.upkeep.entries.joinToString(",") { "${it.key}=${it.value}" },
        "coach" to b.coach.joinToString(","),
        "start" to b.start.toString(),
    ).map { MetaRow(it.first, it.second) }
}
