package app.biohacker

import app.biohacker.data.Bags
import app.biohacker.data.DemoSeed
import app.biohacker.domain.Bag
import app.biohacker.domain.Catalog
import app.biohacker.domain.Engine
import app.biohacker.domain.Mark
import app.biohacker.domain.Note
import app.biohacker.domain.Parcel
import app.biohacker.domain.Reading
import app.biohacker.domain.State
import app.biohacker.domain.Status
import app.biohacker.domain.Sys
import app.biohacker.domain.Trial
import app.biohacker.domain.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

class EngineTest {
    private fun row(id: String, sys: String, tag: String, load: Int = 1, freq: Int = 7, rare: Int = 1, ev: String = "A") =
        listOf(id, sys, tag, ev, "$rare", "$load", "morning", "5", "$freq", "Имя $id", "Name $id", "как", "how", "почему", "why", "-", "-").joinToString("\t")

    private val cat = Catalog.of(
        listOf(listOf(
            row("sleep-001", "sleep", "light"), row("sleep-002", "sleep", "dark", load = 2), row("move-001", "move", "walk", load = 3),
            row("shield-001", "shield", "cold"), row("calm-001", "calm", "breath", rare = 3), row("joy-001", "joy", "grateful", freq = 3),
        ).joinToString("\n")),
        listOf("sleep-m01\tsleep\tМиф\tMyth\tПравда\tTruth"),
    )

    private fun st(readings: List<Reading> = emptyList(), marks: List<Mark> = emptyList(), trials: List<Trial> = emptyList(), bag: Bag = Bag()) = State(readings, marks, trials, bag)

    @Test fun parsesAndSkipsBadLines() {
        assertEquals(6, cat.all.size)
        assertEquals(1, cat.myths.size)
        assertEquals("CLK-001", cat["sleep-001"]!!.code)
        assertEquals(0, Catalog.parse("broken\tline").size)
    }

    @Test fun powerLimitsTrials() {
        var s = st()
        assertEquals(3, Engine.capacity(s))
        assertNull(Engine.block(s, cat, cat["move-001"]!!))
        s = st(trials = listOf(Trial(1, "move-001", 0, 14, Status.RUN)))
        assertEquals(Engine.Block.POWER, Engine.block(s, cat, cat["sleep-001"]!!))
        assertEquals(Engine.Block.RUNNING, Engine.block(s, cat, cat["move-001"]!!))
        // four installed mods add two units of power
        val fw = listOf("sleep-001", "sleep-002", "shield-001", "calm-001").mapIndexed { i, m -> Trial(10L + i, m, 0, 14, Status.ON, 14) }
        assertEquals(5, Engine.capacity(st(trials = fw)))
        assertEquals(Engine.Block.INSTALLED, Engine.block(st(trials = fw), cat, cat["calm-001"]!!))
    }

    @Test fun trialFindsAnEffect() {
        val start = 100L
        val marks = (start until start + 14).filter { it % 3 != 0L }.map { Mark(it, "sleep-001") }
        val done = marks.map { it.day }.toSet()
        val readings = (start - 14 until start + 15).map { d -> Reading(d, listOf(if (d in done || d - 1 in done) 4 else 3, 3, 3, 3, 3)) }
        val t = Trial(1, "sleep-001", start, 14, Status.RUN)
        val r = Engine.result(st(readings, marks, listOf(t)), t, cat)
        assertEquals(Verdict.WORKS, r.verdict)
        assertTrue(r.delta(app.biohacker.domain.Metric.SLEEP)!! > .5f)
        assertTrue(t.ready(start + 14))
    }

    @Test fun thinDataWithoutReadings() {
        val t = Trial(1, "sleep-001", 100, 14, Status.RUN)
        assertEquals(Verdict.THIN, Engine.result(st(trials = listOf(t)), t, cat).verdict)
    }

    @Test fun uptimeBridgesOneMissedDayAWeek() {
        val days = (1L..20L).filter { it != 10L }
        val s = st(days.map { Reading(it, listOf(3, 3, 3, 3, 3)) })
        assertEquals(19, Engine.uptime(s, 20))
        assertEquals(19, Engine.uptime(s, 21)) // today's readings not in yet
        val two = st((1L..20L).filter { it != 10L && it != 11L }.map { Reading(it, listOf(3, 3, 3, 3, 3)) })
        assertEquals(9, Engine.uptime(two, 20))
    }

    @Test fun parcelIsStableForTheDayAndOpens() {
        val s = st()
        val a = Engine.parcel(s, cat, 200)
        assertEquals(a, Engine.parcel(s, cat, 200))
        assertNotNull(a)
        val b = Engine.open(Bag(), a!!)
        assertTrue(a.card in b.found || a.card in b.myths)
        // a kept parcel wins over a fresh draw
        val kept = Parcel(200, "joy-001", false, opened = true)
        assertEquals(kept, Engine.parcel(st(bag = Bag(parcel = kept)), cat, 200))
    }

    @Test fun drawSkipsWhatIsKnown() {
        val known = cat.all.map { it.id }.toSet() - "calm-001"
        repeat(20) {
            val p = Engine.draw(st(bag = Bag(studied = known, myths = setOf("sleep-m01"))), cat, Random(it), 1)!!
            assertEquals("calm-001", p.card)
        }
    }

    @Test fun everyTenthStudyEarnsABonus() {
        var b = Bag()
        for (i in 1..10) b = Engine.study(b, "x$i")
        assertEquals(1, b.bonus)
        assertEquals(b, Engine.study(b, "x3"))
    }

    @Test fun combosNeedEveryTag() {
        val fw = listOf(Trial(1, "sleep-001", 0, 14, Status.ON, 14))
        assertTrue(Engine.combos(st(trials = fw), cat).isEmpty())
        val both = fw + Trial(2, "shield-001", 0, 14, Status.ON, 14)
        assertEquals(listOf("reset"), Engine.combos(st(trials = both), cat).map { it.id })
    }

    @Test fun emergencyPatchPicksShortMods() {
        val r = Reading(5, listOf(1, 3, 3, 3, 3))
        val ms = Engine.emergency(cat, r)
        assertTrue(ms.isNotEmpty())
        assertTrue(ms.all { it.sys == Sys.SLEEP })
    }

    @Test fun weekNotesCatchInstallsAndCombos() {
        val mon = LocalDate.of(2026, 10, 5).toEpochDay()
        val trials = listOf(Trial(1, "sleep-001", mon - 14, 14, Status.ON, mon + 1), Trial(2, "shield-001", mon - 14, 14, Status.ON, mon + 2))
        val wk = Engine.week(st(trials = trials), cat, mon)
        assertEquals(2, wk.notes.count { it.kind == Note.Kind.INSTALL })
        assertTrue(wk.notes.any { it.kind == Note.Kind.COMBO && it.combo == "reset" })
        assertEquals(1, wk.major); assertEquals(2, wk.minor)
    }

    @Test fun bagRoundTrips() {
        val b = Bag(subject = "Марат", focus = setOf(Sys.SLEEP, Sys.JOY), studied = setOf("a", "b"), found = setOf("a"), myths = setOf("m"),
            parcel = Parcel(9, "a", false, true), bonus = 2, combos = setOf("reset"), notesSeen = 7, upkeep = mapOf("bp" to 3L), coach = setOf("lab"), start = 5)
        assertEquals(b, Bags.read(Bags.write(b).associate { it.k to it.v }))
    }

    @Test fun realCatalogueParsesWhole() {
        val dir = File("src/main/assets/content")
        if (!dir.exists()) return
        val mods = Catalog.files().map { File("src/main/assets/" + it) }.filter { it.exists() }
        val myths = Catalog.mythFiles().map { File("src/main/assets/" + it) }.filter { it.exists() }
        val c = Catalog.of(mods.map { it.readText() }, myths.map { it.readText() })
        val lines = mods.sumOf { f -> f.readLines().count { it.isNotBlank() } }
        assertEquals(lines, c.all.size)
        assertEquals(c.all.size, c.all.map { it.id }.toSet().size)
    }

    @Test fun demoSeedRunsThroughTheEngine() {
        val s = DemoSeed.build(cat, System.currentTimeMillis(), ZoneId.of("UTC"))
        assertTrue(s.readings.size > 40)
        assertTrue(s.trials.isNotEmpty())
    }
}
