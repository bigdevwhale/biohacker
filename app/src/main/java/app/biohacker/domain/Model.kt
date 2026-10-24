package app.biohacker.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The five gauges read every day, 1..5 each. */
enum class Metric { SLEEP, ENERGY, MOOD, FOCUS, CALM;
    val key get() = name.lowercase()
}

/** The eight systems of the body on the blueprint; each answers mostly to one gauge. */
enum class Sys(val metric: Metric, val code: String) {
    SLEEP(Metric.SLEEP, "CLK"), MOVE(Metric.ENERGY, "MTR"), FUEL(Metric.ENERGY, "FRN"), SHIELD(Metric.ENERGY, "ARM"),
    MIND(Metric.FOCUS, "CPU"), CALM(Metric.CALM, "CLR"), JOY(Metric.MOOD, "BAT"), LINK(Metric.MOOD, "NET");

    val key get() = name.lowercase()
    val tags get() = Tag.entries.filter { it.sys == this }

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key }
        /** The systems that move a gauge, for the emergency patch. */
        fun feeding(m: Metric) = entries.filter { it.metric == m }
    }
}

/** What a mod works on inside its system; combos are made of tags. */
enum class Tag(val sys: Sys) {
    LIGHT(Sys.SLEEP), DARK(Sys.SLEEP), RHYTHM(Sys.SLEEP), ROOM(Sys.SLEEP), WINDDOWN(Sys.SLEEP), NAP(Sys.SLEEP),
    WALK(Sys.MOVE), CARDIO(Sys.MOVE), STRENGTH(Sys.MOVE), MOBILITY(Sys.MOVE), NEAT(Sys.MOVE), BALANCE(Sys.MOVE),
    PLANTS(Sys.FUEL), PROTEIN(Sys.FUEL), TIMING(Sys.FUEL), WATER(Sys.FUEL), LESS(Sys.FUEL), GUT(Sys.FUEL),
    COLD(Sys.SHIELD), HEAT(Sys.SHIELD), SUN(Sys.SHIELD), TEETH(Sys.SHIELD), CHECK(Sys.SHIELD), AIR(Sys.SHIELD),
    FOCUS(Sys.MIND), LEARN(Sys.MIND), DIGITAL(Sys.MIND), CREATE(Sys.MIND), READ(Sys.MIND), NOVELTY(Sys.MIND),
    BREATH(Sys.CALM), MEDITATE(Sys.CALM), BODY(Sys.CALM), WRITE(Sys.CALM), SLOW(Sys.CALM), SENSE(Sys.CALM),
    GRATEFUL(Sys.JOY), PLAY(Sys.JOY), MEANING(Sys.JOY), SAVOR(Sys.JOY), MUSIC(Sys.JOY), AWE(Sys.JOY),
    FRIENDS(Sys.LINK), FAMILY(Sys.LINK), KIND(Sys.LINK), NATURE(Sys.LINK), COMMUNITY(Sys.LINK), TOUCH(Sys.LINK);

    val key get() = name.lowercase()

    companion object {
        fun of(sys: Sys, key: String) = entries.firstOrNull { it.sys == sys && it.key == key }
    }
}

enum class When { WAKE, MORNING, DAY, EVENING, BED, ANY;
    val key get() = name.lowercase()
    companion object { fun of(key: String) = entries.firstOrNull { it.key == key } }
}

/**
 * One protocol of the catalogue. [ev] is the evidence class A..D, [rare] 1..3 how surprising the card is,
 * [load] 1..3 how much willpower it takes while new, [freq] days a week.
 */
data class Mod(
    val id: String, val sys: Sys, val tag: Tag, val ev: Char, val rare: Int, val load: Int, val at: When, val min: Int, val freq: Int,
    val ruName: String, val enName: String, val ruHow: String, val enHow: String, val ruWhy: String, val enWhy: String,
    val ruWarn: String?, val enWarn: String?,
) {
    fun name(ru: Boolean) = if (ru) ruName else enName
    fun how(ru: Boolean) = if (ru) ruHow else enHow
    fun why(ru: Boolean) = if (ru) ruWhy else enWhy
    fun warn(ru: Boolean) = if (ru) ruWarn else enWarn
    /** A code like on a drawing: CLK-001. */
    val code get() = sys.code + "-" + id.substringAfterLast('-')
    val daily get() = freq >= 7
}

/** A popular hack that doesn't hold up: a broken mod for the collection. */
data class Myth(val id: String, val sys: Sys, val ruName: String, val enName: String, val ruTruth: String, val enTruth: String) {
    fun name(ru: Boolean) = if (ru) ruName else enName
    fun truth(ru: Boolean) = if (ru) ruTruth else enTruth
    val code get() = sys.code + "-X" + id.substringAfterLast("-m")
}

/** One day's gauges, each 1..5. */
data class Reading(val day: Long, val v: List<Int>) {
    operator fun get(m: Metric) = v[m.ordinal]
    val glitch get() = v.any { it <= 2 }
    val low get() = Metric.entries.filter { this[it] <= 2 }
}

/** A mod done on a day. */
data class Mark(val day: Long, val mod: String)

enum class Status { RUN, ON, OFF, UNSURE, DROP, RETIRED;
    val key get() = name.lowercase()
    companion object { fun of(key: String) = entries.firstOrNull { it.key == key } ?: DROP }
}

/** A mod on trial from [start] for [days] days, and how it ended (on day [end]). */
data class Trial(val id: Long, val mod: String, val start: Long, val days: Int, val status: Status, val end: Long = -1, val effect: Float? = null) {
    val last get() = start + days - 1
    fun day(today: Long) = (today - start + 1).toInt()
    fun ready(today: Long) = status == Status.RUN && today > last
}

/** Today's (or a bonus) parcel: a card that comes sealed. [myth] when it is a broken mod. */
data class Parcel(val day: Long, val card: String, val myth: Boolean, val opened: Boolean = false)

data class Bag(
    val subject: String = "",
    /** The systems the subject cares about most: parcels lean towards them. */
    val focus: Set<Sys> = emptySet(),
    /** Cards opened in the catalogue. */
    val studied: Set<String> = emptySet(),
    /** Cards that came in parcels. */
    val found: Set<String> = emptySet(),
    val myths: Set<String> = emptySet(),
    val parcel: Parcel? = null,
    /** Bonus parcels waiting: one for each finished trial and every ten cards studied. */
    val bonus: Int = 0,
    val combos: Set<String> = emptySet(),
    /** The last week whose patch notes were read (epoch day of its Monday). */
    val notesSeen: Long = -1,
    /** Maintenance item → the day it was last done. */
    val upkeep: Map<String, Long> = emptyMap(),
    /** Screens whose first-visit hints were read. */
    val coach: Set<String> = emptySet(),
    val start: Long = 0,
)

data class State(val readings: List<Reading>, val marks: List<Mark>, val trials: List<Trial>, val bag: Bag) {
    private val byDay = readings.associateBy { it.day }
    private val markSet = marks.map { it.day to it.mod }.toHashSet()
    fun reading(day: Long) = byDay[day]
    fun done(day: Long, mod: String) = (day to mod) in markSet
    val running get() = trials.filter { it.status == Status.RUN }
    /** The installed mods: whose last trial ended in the firmware. */
    val firmware: List<Trial> get() = trials.groupBy { it.mod }.values.map { it.last() }.filter { it.status == Status.ON }.sortedBy { it.end }
    fun lastTrial(mod: String) = trials.lastOrNull { it.mod == mod }
}

fun Long.date(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
fun Long.day(zone: ZoneId): Long = date(zone).toEpochDay()
