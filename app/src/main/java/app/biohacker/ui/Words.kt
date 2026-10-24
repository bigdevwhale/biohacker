package app.biohacker.ui

import app.biohacker.domain.Catalog
import app.biohacker.domain.Combos
import app.biohacker.domain.Metric
import app.biohacker.domain.Note
import app.biohacker.domain.Status
import app.biohacker.domain.Sys
import app.biohacker.domain.Tag
import app.biohacker.domain.Verdict
import app.biohacker.domain.When
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** The words of the lab, in both languages. */
class Words(val ru: Boolean) {
    fun t(r: String, e: String) = if (ru) r else e

    fun plural(n: Int, one: String, few: String, many: String): String {
        val a = abs(n) % 100; val b = a % 10
        return when { a in 11..19 -> many; b == 1 -> one; b in 2..4 -> few; else -> many }
    }

    fun days(n: Int) = if (ru) "$n ${plural(n, "день", "дня", "дней")}" else "$n day${if (n == 1) "" else "s"}"

    fun sys(s: Sys) = when (s) {
        Sys.SLEEP -> t("Часы", "Clock"); Sys.MOVE -> t("Мотор", "Engine"); Sys.FUEL -> t("Топка", "Furnace"); Sys.SHIELD -> t("Броня", "Armor")
        Sys.MIND -> t("Процессор", "Processor"); Sys.CALM -> t("Охлаждение", "Cooling"); Sys.JOY -> t("Аккумулятор", "Battery"); Sys.LINK -> t("Сеть", "Network")
    }

    fun sysTag(s: Sys) = when (s) {
        Sys.SLEEP -> t("сон, свет и ритм", "sleep, light and rhythm"); Sys.MOVE -> t("движение и сила", "movement and strength")
        Sys.FUEL -> t("еда и вода", "food and water"); Sys.SHIELD -> t("закалка и профилактика", "hardening and prevention")
        Sys.MIND -> t("фокус и обучение", "focus and learning"); Sys.CALM -> t("стресс и дыхание", "stress and breath")
        Sys.JOY -> t("настроение и смысл", "mood and meaning"); Sys.LINK -> t("люди и природа", "people and nature")
    }

    fun tag(g: Tag) = when (g) {
        Tag.LIGHT -> t("свет", "light"); Tag.DARK -> t("темнота", "darkness"); Tag.RHYTHM -> t("ритм", "rhythm"); Tag.ROOM -> t("спальня", "bedroom")
        Tag.WINDDOWN -> t("отбой", "wind-down"); Tag.NAP -> t("дневной сон", "naps")
        Tag.WALK -> t("ходьба", "walking"); Tag.CARDIO -> t("выносливость", "cardio"); Tag.STRENGTH -> t("сила", "strength"); Tag.MOBILITY -> t("подвижность", "mobility")
        Tag.NEAT -> t("меньше сидеть", "less sitting"); Tag.BALANCE -> t("равновесие", "balance")
        Tag.PLANTS -> t("растения", "plants"); Tag.PROTEIN -> t("белок", "protein"); Tag.TIMING -> t("время еды", "meal timing"); Tag.WATER -> t("вода", "water")
        Tag.LESS -> t("меньше лишнего", "less of the bad"); Tag.GUT -> t("кишечник", "gut")
        Tag.COLD -> t("холод", "cold"); Tag.HEAT -> t("жар", "heat"); Tag.SUN -> t("солнце и кожа", "sun and skin"); Tag.TEETH -> t("уход", "body care")
        Tag.CHECK -> t("проверки", "check-ups"); Tag.AIR -> t("воздух и дом", "air and home")
        Tag.FOCUS -> t("фокус", "focus"); Tag.LEARN -> t("учёба", "learning"); Tag.DIGITAL -> t("экран", "screens"); Tag.CREATE -> t("творчество", "making")
        Tag.READ -> t("чтение", "reading"); Tag.NOVELTY -> t("новизна", "novelty")
        Tag.BREATH -> t("дыхание", "breath"); Tag.MEDITATE -> t("медитация", "meditation"); Tag.BODY -> t("тело", "body"); Tag.WRITE -> t("письмо", "writing")
        Tag.SLOW -> t("паузы", "pauses"); Tag.SENSE -> t("чувства", "senses")
        Tag.GRATEFUL -> t("благодарность", "gratitude"); Tag.PLAY -> t("игра", "play"); Tag.MEANING -> t("смысл", "meaning"); Tag.SAVOR -> t("смакование", "savouring")
        Tag.MUSIC -> t("музыка", "music"); Tag.AWE -> t("восхищение", "awe")
        Tag.FRIENDS -> t("друзья", "friends"); Tag.FAMILY -> t("семья", "family"); Tag.KIND -> t("доброта", "kindness"); Tag.NATURE -> t("природа", "nature")
        Tag.COMMUNITY -> t("сообщество", "community"); Tag.TOUCH -> t("близость", "closeness")
    }

    fun metric(m: Metric) = when (m) {
        Metric.SLEEP -> t("Сон", "Sleep"); Metric.ENERGY -> t("Энергия", "Energy"); Metric.MOOD -> t("Настроение", "Mood")
        Metric.FOCUS -> t("Фокус", "Focus"); Metric.CALM -> t("Спокойствие", "Calm")
    }

    fun metricQ(m: Metric) = when (m) {
        Metric.SLEEP -> t("Как спал этой ночью?", "How did you sleep last night?"); Metric.ENERGY -> t("Сколько сил сегодня?", "How much energy today?")
        Metric.MOOD -> t("Какое настроение?", "How's your mood?"); Metric.FOCUS -> t("Как держалось внимание?", "How well could you focus?")
        Metric.CALM -> t("Насколько было спокойно?", "How calm did you feel?")
    }

    fun scale(m: Metric): List<String> = when (m) {
        Metric.SLEEP -> listOf(t("ужасно", "awful"), t("плохо", "poor"), t("так себе", "so-so"), t("хорошо", "good"), t("отлично", "great"))
        Metric.ENERGY -> listOf(t("пусто", "empty"), t("мало", "low"), t("средне", "okay"), t("бодро", "lively"), t("заряжен", "charged"))
        Metric.MOOD -> listOf(t("тяжело", "heavy"), t("хмуро", "gloomy"), t("ровно", "even"), t("хорошо", "good"), t("светло", "bright"))
        Metric.FOCUS -> listOf(t("каша", "scattered"), t("рвётся", "patchy"), t("средне", "okay"), t("держится", "steady"), t("лазер", "laser"))
        Metric.CALM -> listOf(t("на взводе", "wound up"), t("тревожно", "uneasy"), t("ровно", "even"), t("спокойно", "calm"), t("штиль", "still"))
    }

    fun at(w: When) = when (w) {
        When.WAKE -> t("после подъёма", "on waking"); When.MORNING -> t("утро", "morning"); When.DAY -> t("день", "day")
        When.EVENING -> t("вечер", "evening"); When.BED -> t("перед сном", "before bed"); When.ANY -> t("в любое время", "any time")
    }

    fun ev(e: Char) = when (e) {
        'A' -> t("сильные данные", "strong evidence"); 'B' -> t("хорошие данные", "good evidence")
        'C' -> t("правдоподобно", "plausible"); else -> t("слабые данные", "weak evidence")
    }

    fun evLong(e: Char) = when (e) {
        'A' -> t("Много исследований и метаанализов, научный консенсус", "Many trials and meta-analyses, scientific consensus")
        'B' -> t("Есть рандомизированные исследования или крупные наблюдения", "Some randomised trials or large cohort studies")
        'C' -> t("Небольшие исследования, механизм, наблюдения", "Small studies, mechanism, observational links")
        else -> t("Традиция и опыт — но безвредно и дёшево попробовать", "Tradition and experience — but harmless and cheap to try")
    }

    fun rare(r: Int) = when (r) { 1 -> t("обычная", "common"); 2 -> t("редкая", "rare"); else -> t("легендарная", "legendary") }

    fun load(l: Int) = when (l) { 1 -> t("лёгкий", "light"); 2 -> t("средний", "medium"); else -> t("тяжёлый", "heavy") }

    fun freq(f: Int) = when {
        f >= 7 -> t("каждый день", "daily")
        f == 1 -> t("раз в неделю", "once a week")
        else -> if (ru) "$f ${plural(f, "раз", "раза", "раз")} в неделю" else "$f× a week"
    }

    fun min(m: Int) = if (m >= 60) t("час+", "1 h+") else if (ru) "$m мин" else "$m min"

    fun power(n: Int) = if (ru) "$n ${plural(n, "единица", "единицы", "единиц")}" else "$n unit${if (n == 1) "" else "s"}"

    fun status(s: Status) = when (s) {
        Status.RUN -> t("на испытании", "on trial"); Status.ON -> t("в прошивке", "in firmware"); Status.OFF -> t("отклонён", "rejected")
        Status.UNSURE -> t("неясно", "unclear"); Status.DROP -> t("прерван", "stopped"); Status.RETIRED -> t("снят", "removed")
    }

    fun verdict(v: Verdict) = when (v) {
        Verdict.WORKS -> t("РАБОТАЕТ", "WORKS"); Verdict.FLAT -> t("НЕТ ЭФФЕКТА", "NO EFFECT")
        Verdict.WORSE -> t("ХУЖЕ", "WORSE"); Verdict.THIN -> t("МАЛО ДАННЫХ", "THIN DATA")
    }

    fun verdictText(v: Verdict) = when (v) {
        Verdict.WORKS -> t("В дни с этим модом показатели заметно выше. Похоже, тебе подходит.", "On days with this mod your gauges ran clearly higher. Looks like it suits you.")
        Verdict.FLAT -> t("Разницы почти нет. Бывает: не каждый мод работает для каждого. Можно отклонить без сожалений — или продлить, если нравится.", "Hardly any difference. That happens: not every mod works for everyone. Reject it without regret — or extend it if you like it.")
        Verdict.WORSE -> t("В дни с модом показатели ниже. Возможно, не твоё — или не то время дня.", "Your gauges ran lower on days with the mod. Maybe not for you — or not at this time of day.")
        Verdict.THIN -> t("Данных мало: не хватает показаний или мод выполнялся реже половины плана. Продли на неделю — или реши сам.", "Not enough data: readings are missing or the mod was done less than half the plan. Extend it a week — or decide yourself.")
    }

    fun signed(v: Float): String {
        val r = (v * 10).roundToInt() / 10f
        val s = if (ru) String.format(Locale("ru"), "%.1f", abs(r)) else String.format(Locale.ENGLISH, "%.1f", abs(r))
        return (if (r > 0) "+" else if (r < 0) "−" else "±") + s
    }

    fun num(v: Float) = if (ru) String.format(Locale("ru"), "%.1f", v) else String.format(Locale.ENGLISH, "%.1f", v)

    fun version(major: Int, minor: Int) = "v$major.$minor"

    fun date(d: LocalDate): String = if (ru) "${d.dayOfMonth} ${MONTHS[d.monthValue - 1]}" else "${d.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${d.dayOfMonth}"

    fun weekday(d: LocalDate) = d.dayOfWeek.getDisplayName(TextStyle.SHORT, if (ru) Locale("ru") else Locale.ENGLISH)

    fun greet(hour: Int) = when { hour < 5 || hour >= 18 -> t("Добрый вечер", "Good evening"); hour < 12 -> t("Доброе утро", "Good morning"); else -> t("Добрый день", "Good afternoon") }

    /** One line of patch notes, with its sign. */
    fun note(n: Note, cat: Catalog): Pair<String, String> {
        val mod = n.mod?.let { cat[it]?.name(ru) }.orEmpty()
        val sys = n.mod?.let { cat[it]?.sys }?.let { " · " + sys(it) }.orEmpty()
        return when (n.kind) {
            Note.Kind.INSTALL -> "+" to t("Установлен мод «$mod»$sys", "Installed “$mod”$sys")
            Note.Kind.REJECT -> "−" to t("Отклонён «$mod»: не дал эффекта", "Rejected “$mod”: no effect")
            Note.Kind.UNSURE -> "?" to t("«$mod» — результат неясен, отложен", "“$mod” — unclear, set aside")
            Note.Kind.DROP -> "×" to t("Испытание «$mod» прервано", "Trial of “$mod” stopped")
            Note.Kind.START -> "~" to t("На испытание поставлен «$mod»", "Put “$mod” on trial")
            Note.Kind.COMBO -> "★" to t("Найдено комбо «${Combos[n.combo.orEmpty()]?.name(true).orEmpty()}»", "Combo found: “${Combos[n.combo.orEmpty()]?.name(false).orEmpty()}”")
            Note.Kind.UP -> "↑" to t("${metric(n.metric!!)}: ${signed(n.value)} к прошлой неделе", "${metric(n.metric!!)}: ${signed(n.value)} on last week")
            Note.Kind.DOWN -> "!" to t("Известная проблема: «${metric(n.metric!!)}» ${signed(n.value)} к прошлой неделе", "Known issue: ${metric(n.metric!!).lowercase()} ${signed(n.value)} on last week")
            Note.Kind.GLITCH -> "!" to t("Известная проблема: «${metric(n.metric!!)}» в красной зоне ${days(n.n)}", "Known issue: ${metric(n.metric!!).lowercase()} in the red on ${days(n.n)}")
            Note.Kind.STABLE -> "✓" to t("Прошивка стабильна: ${(n.value * 100).roundToInt()}% выполнено", "Firmware stable: ${(n.value * 100).roundToInt()}% kept")
            Note.Kind.QUIET -> "·" to t("Тихая сборка. Изменений нет", "Quiet build. No changes")
        }
    }

    /** First-visit hints, one card per screen. */
    fun coach(screen: String): List<String> = when (screen) {
        "blueprint" -> listOf(t("Это твой чертёж", "This is your blueprint"),
            t("Тело разбито на восемь систем. Каждый мод, который тебе подошёл, загорается узлом в своей системе.", "The body is split into eight systems. Every mod that works for you lights up as a node in its system."),
            t("Ниже — что сделать сегодня. Отмечай галочкой: так копятся данные для испытаний.", "Below is today's work. Tick things off: that's the data your trials run on."),
            t("Раз в день приходит посылка из лаборатории с новой карточкой.", "Once a day a parcel arrives from the lab with a new card."))
        "readings" -> listOf(t("Показания", "Readings"),
            t("Пять приборов, по одному касанию. Раз в день — лучше вечером.", "Five gauges, one tap each. Once a day — evening is best."),
            t("По ним лаборатория поймёт, какие моды работают именно для тебя.", "That's how the lab learns which mods work for you."),
            t("Плохой день — не провал, а глитч. Лаборатория подскажет аварийный патч.", "A bad day isn't a failure, it's a glitch. The lab will suggest an emergency patch."))
        "lab" -> listOf(t("Лаборатория", "The lab"),
            t("Каждый мод проходит испытание — 14 дней. Потом смотрим данные и решаем: вшить в прошивку или отклонить.", "Every mod goes through a 14-day trial. Then we look at the data and decide: install it, or reject it."),
            t("Мощность ограничена: тяжёлые моды занимают больше. С каждым установленным модом мощность растёт.", "Power is limited: heavy mods take more of it. Each installed mod adds power."),
            t("Отклонённый мод — не провал. Это тоже результат.", "A rejected mod isn't a failure. It's a result too."))
        "catalog" -> listOf(t("Картотека", "The card index"),
            t("Здесь все протоколы: восемь систем, у каждого — класс доказательности от A до D.", "Every protocol is here: eight systems, each with an evidence class from A to D."),
            t("Открытая карточка считается изученной. За каждые десять — бонусная посылка.", "Every card you open counts as studied. Each ten earn a bonus parcel."),
            t("Сломанные моды — популярные хаки, которые не подтвердились. Их находишь в посылках.", "Broken mods are popular hacks that didn't hold up. You find them in parcels."))
        "log" -> listOf(t("Журнал", "The log"),
            t("Каждый понедельник выходит патч: что установлено, что отклонено, что выросло.", "Every Monday a patch comes out: what was installed, rejected, what went up."),
            t("Комбо — скрытые сочетания модов. Найди все.", "Combos are hidden pairings of mods. Find them all."),
            t("Плановое ТО — проверки, которые ловят поломки заранее.", "Scheduled service: check-ups that catch faults early."))
        else -> emptyList()
    }

    companion object {
        private val MONTHS = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")
    }
}
