package app.biohacker.ui.log

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.biohacker.container
import app.biohacker.domain.Combos
import app.biohacker.domain.Engine
import app.biohacker.domain.Metric
import app.biohacker.domain.Upkeep
import app.biohacker.domain.Week
import app.biohacker.ui.Home
import app.biohacker.ui.Nav
import app.biohacker.ui.components.BottomSheet
import app.biohacker.ui.components.Btn
import app.biohacker.ui.components.Chip
import app.biohacker.ui.components.CommunityLinks
import app.biohacker.ui.components.Dot
import app.biohacker.ui.components.Gap
import app.biohacker.ui.components.Kind
import app.biohacker.ui.components.Panel
import app.biohacker.ui.components.Section
import app.biohacker.ui.components.TopBar
import app.biohacker.ui.components.grid
import app.biohacker.ui.theme.C
import app.biohacker.ui.theme.T
import java.time.LocalDate

@Composable
fun LogScreen(h: Home, nav: Nav) {
    val w = h.w
    val c = LocalContext.current.container
    val latest = h.weeks.firstOrNull()?.monday
    LaunchedEffect(latest) { if (latest != null && latest > h.s.bag.notesSeen) c.repo.notesSeen(latest) }
    var upkeep by remember { mutableStateOf<Upkeep?>(null) }
    Box(Modifier.fillMaxSize().grid()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(w.t("Журнал", "Log"), "${w.version(h.major, h.minor)} · ${w.t("сборка", "build")} ${h.build}")
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Section(w.t("Патчноуты", "Patch notes"), num = "01")
                if (h.weeks.isEmpty()) Text(w.t("Первый патч выйдет в понедельник.", "The first patch comes out on Monday."), style = T.Small, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                var more by remember { mutableStateOf(false) }
                h.weeks.take(if (more) h.weeks.size else 2).forEachIndexed { i, wk -> Patch(h, wk, fresh = i == 0) }
                if (h.weeks.size > 2 && !more) Text(w.t("ВСЕ ПАТЧИ (${h.weeks.size}) ›", "ALL PATCHES (${h.weeks.size}) ›"), style = T.Label.copy(color = C.amber), modifier = Modifier.clickable { more = true }.padding(horizontal = 20.dp, vertical = 8.dp))
                Section(w.t("Показания · 30 дней", "Readings · 30 days"), num = "02")
                Chart(h)
                Section(w.t("Комбо ${h.combos.size}/${Combos.all.size}", "Combos ${h.combos.size}/${Combos.all.size}"), num = "03")
                ComboList(h)
                Section(w.t("Плановое ТО", "Scheduled service"), num = "04")
                for (u in Upkeep.all) UpkeepRow(h, u) { upkeep = u }
                Section(w.t("Баги, идеи, обновления", "Bugs, ideas, updates"), num = "05")
                CommunityLinks(w)
                Text(
                    w.t("Приложение не заменяет врача. Если что-то беспокоит — к специалисту.", "This app is not a doctor. If something worries you, see a professional."),
                    style = T.Small.copy(color = C.faint), modifier = Modifier.padding(20.dp),
                )
                Gap(16)
            }
        }
        UpkeepSheet(h, upkeep) { upkeep = null }
    }
}

@Composable
private fun Patch(h: Home, wk: Week, fresh: Boolean) {
    val w = h.w
    val mon = LocalDate.ofEpochDay(wk.monday)
    Panel(tick = if (fresh) C.cyan else C.faint) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(w.version(wk.major, wk.minor), style = T.Code.copy(color = if (fresh) C.cyan else C.line), modifier = Modifier.padding(end = 10.dp))
            Text("${w.t("сборка", "build")} ${wk.build} · ${w.date(mon)} – ${w.date(mon.plusDays(6))}", style = T.Label, modifier = Modifier.weight(1f))
        }
        Gap(6)
        for (n in wk.notes) {
            val (sign, text) = w.note(n, h.cat)
            Row(Modifier.padding(vertical = 2.dp)) {
                Text(sign, style = T.Code.copy(color = when (sign) { "+", "✓", "↑" -> C.green; "−", "×" -> C.red; "!" -> C.amber; "★" -> C.amber; "~" -> C.cyan; else -> C.muted }), modifier = Modifier.width(22.dp))
                Text(text, style = T.Body)
            }
        }
    }
}

@Composable
private fun Chart(h: Home) {
    val w = h.w
    var on by remember { mutableStateOf(Metric.entries.toSet()) }
    val days = (h.today - 29..h.today).toList()
    val colors = mapOf(Metric.SLEEP to C.violet, Metric.ENERGY to C.amber, Metric.MOOD to C.sys(app.biohacker.domain.Sys.JOY), Metric.FOCUS to C.cyan, Metric.CALM to C.green)
    Panel {
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val dx = size.width / (days.size - 1)
            fun y(v: Float) = size.height - (v - 1) / 4f * size.height
            for (k in 1..5) drawLine(C.grid, Offset(0f, y(k.toFloat())), Offset(size.width, y(k.toFloat())), 1f, pathEffect = if (k == 3) null else PathEffect.dashPathEffect(floatArrayOf(4f, 5f)))
            for (m in Metric.entries) {
                if (m !in on) continue
                var prev: Offset? = null
                days.forEachIndexed { i, d ->
                    val r = h.s.reading(d)
                    if (r == null) { prev = null; return@forEachIndexed }
                    // a week's running mean keeps five lines readable
                    val vs = (d - 3..d + 3).mapNotNull { h.s.reading(it)?.get(m) }
                    val p = Offset(i * dx, y(vs.average().toFloat()))
                    prev?.let { drawLine(colors.getValue(m), it, p, 2.2f) }
                    prev = p
                }
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (m in Metric.entries) Chip(w.metric(m), on = m in on, color = colors.getValue(m)) { on = if (m in on) on - m else on + m }
        }
        val wk = h.s.readings.filter { it.day > h.today - 7 }
        val prev = h.s.readings.filter { it.day in (h.today - 13)..(h.today - 7) }
        if (wk.isNotEmpty()) {
            Gap(10)
            for (m in Metric.entries) Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Dot(colors.getValue(m))
                Text(w.metric(m), style = T.Body, modifier = Modifier.padding(start = 8.dp).weight(1f))
                val a = wk.map { it[m] }.average().toFloat()
                Text(w.num(a), style = T.Code)
                if (prev.isNotEmpty()) {
                    val d = a - prev.map { it[m] }.average().toFloat()
                    Text(w.signed(d), style = T.Code.copy(color = when { d >= .3f -> C.green; d <= -.3f -> C.red; else -> C.muted }), modifier = Modifier.width(56.dp).padding(start = 10.dp))
                }
            }
            Text(w.t("среднее за 7 дней и разница с прошлой неделей", "7-day average and change on the week before"), style = T.Small.copy(color = C.faint), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun ComboList(h: Home) {
    val w = h.w
    val found = h.combos.map { it.id }.toSet()
    for (cb in Combos.all.sortedBy { if (it.id in found) 0 else 1 }) {
        val got = cb.id in found
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.Top) {
            Text(if (got) "★" else "☆", style = T.Code.copy(color = if (got) C.amber else C.faint), modifier = Modifier.width(24.dp))
            Column(Modifier.weight(1f)) {
                Text(if (got) cb.name(h.ru) else "? ? ?", style = T.BodyM.copy(color = if (got) C.ink else C.faint))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // the hint: which systems it needs; the tags themselves show once found
                    for (t in cb.tags) {
                        Dot(C.sys(t.sys), 7.dp)
                        Text(if (got) " ${w.tag(t)}  " else " ${w.sys(t.sys)}  ", style = T.Small)
                    }
                }
                if (got) Text(cb.why(h.ru), style = T.Small.copy(color = C.line), modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}

@Composable
private fun UpkeepRow(h: Home, u: Upkeep, onClick: () -> Unit) {
    val w = h.w
    val due = Engine.upkeepDue(h.s.bag, u, h.today)
    val (txt, col) = when {
        due == null -> w.t("не отмечено", "not logged") to C.muted
        due < 0 -> w.t("просрочено", "overdue") to C.red
        due < 30 -> w.t("через ${w.days(due.toInt())}", "in ${w.days(due.toInt())}") to C.amber
        else -> w.t("до ${w.date(LocalDate.ofEpochDay(h.today + due))}", "until ${w.date(LocalDate.ofEpochDay(h.today + due))}") to C.green
    }
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(col))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(u.name(h.ru), style = T.BodyM)
            Text(w.t("раз в ${u.months} мес.", "every ${u.months} mo"), style = T.Small)
        }
        Text(txt.uppercase(), style = T.Label.copy(color = col))
    }
}

@Composable
private fun UpkeepSheet(h: Home, u: Upkeep?, onClose: () -> Unit) {
    val w = h.w
    val c = LocalContext.current.container
    BottomSheet(visible = u != null, onClose = onClose) {
        val x = u ?: return@BottomSheet
        Text(w.t("ПЛАНОВОЕ ТО", "SCHEDULED SERVICE"), style = T.Label.copy(color = C.amber))
        Text(x.name(h.ru), style = T.Title, modifier = Modifier.padding(vertical = 6.dp))
        Text(x.how(h.ru), style = T.Body)
        h.s.bag.upkeep[x.id]?.let { Text(w.t("Последний раз: ${w.date(LocalDate.ofEpochDay(it))}", "Last time: ${w.date(LocalDate.ofEpochDay(it))}"), style = T.Small, modifier = Modifier.padding(top = 8.dp)) }
        Btn(w.t("Сделано сегодня", "Done today"), Modifier.fillMaxWidth().padding(top = 14.dp), Kind.GREEN) { c.sfx.blip(); c.write { repo.upkeep(x.id, h.today) }; onClose() }
        if (h.s.bag.upkeep[x.id] != null) Btn(w.t("Сбросить отметку", "Clear the mark"), Modifier.fillMaxWidth().padding(top = 8.dp), Kind.LINE, small = true) { c.write { repo.upkeep(x.id, null) }; onClose() }
        Gap(6)
    }
}
