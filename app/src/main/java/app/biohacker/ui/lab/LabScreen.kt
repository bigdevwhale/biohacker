package app.biohacker.ui.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.biohacker.domain.Engine
import app.biohacker.domain.Status
import app.biohacker.domain.Trial
import app.biohacker.ui.Home
import app.biohacker.ui.Nav
import app.biohacker.ui.components.HelpButton
import app.biohacker.ui.components.Bar
import app.biohacker.ui.components.Btn
import app.biohacker.ui.components.Cells
import app.biohacker.ui.components.Dot
import app.biohacker.ui.components.Gap
import app.biohacker.ui.components.Kind
import app.biohacker.ui.components.Panel
import app.biohacker.ui.components.Section
import app.biohacker.ui.components.Spark
import app.biohacker.ui.components.TopBar
import app.biohacker.ui.components.grid
import app.biohacker.ui.theme.C
import app.biohacker.ui.theme.T

@Composable
fun LabScreen(h: Home, nav: Nav) {
    val w = h.w
    Column(Modifier.fillMaxSize().grid()) {
        TopBar(w.t("Лаборатория", "The lab"), w.t("мощность ${h.used}/${h.capacity} · прошивка ${h.firmware.size}", "power ${h.used}/${h.capacity} · firmware ${h.firmware.size}"), end = { HelpButton { nav.help("lab") } })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Power(h, nav)
            val running = h.s.running
            Section(w.t("На испытании", "On trial"), num = "01")
            if (running.isEmpty()) Text(w.t("Сейчас ничего не испытывается.", "Nothing on trial right now."), style = T.Small, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            for (t in running) TrialCard(h, t) { nav.trial(t.id) }
            Section(w.t("Прошивка", "Firmware"), num = "02")
            if (h.firmware.isEmpty()) Text(w.t("Мод попадает сюда, когда ты решаешь, что он работает.", "A mod lands here once you decide it works."), style = T.Small, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
            for (t in h.firmware) FirmwareRow(h, t) { nav.trial(t.id) }
            val archive = h.s.trials.filter { it.status in setOf(Status.OFF, Status.UNSURE, Status.DROP, Status.RETIRED) && h.s.lastTrial(it.mod) == it }.reversed()
            if (archive.isNotEmpty()) {
                Section(w.t("Архив гипотез", "Hypotheses archive"), num = "03")
                for (t in archive) ArchiveRow(h, t) { nav.trial(t.id) }
            }
            Gap(24)
        }
    }
}

@Composable
private fun Power(h: Home, nav: Nav) {
    val w = h.w
    val free = h.capacity - h.used
    Panel(tick = C.cyan) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(w.t("МОЩНОСТЬ", "POWER"), style = T.Label.copy(color = C.cyan), modifier = Modifier.weight(1f))
            Text("${h.used} / ${h.capacity}", style = T.Code.copy(color = C.cyan))
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // eight cells: the most power there can ever be; the ones not yet earned stay dark
            for (i in 0 until 8) Box(Modifier.weight(1f).height(14.dp).background(when { i < h.used -> C.cyan; i < h.capacity -> C.grid; else -> C.paper }))
        }
        Text(
            if (free > 0) w.t("Свободно: ${w.power(free)}. Лёгкий мод занимает 1, тяжёлый — 3.", "Free: ${w.power(free)}. A light mod takes 1, a heavy one 3.")
            else w.t("Мощность занята. Заверши испытание — или установи пару модов, чтобы её стало больше.", "Power is full. Finish a trial — or install a couple of mods to get more."),
            style = T.Small,
        )
        Text(w.t("Каждые 2 мода в прошивке дают +1 к мощности.", "Every 2 mods in firmware add +1 power."), style = T.Small.copy(color = C.faint), modifier = Modifier.padding(top = 2.dp))
        if (free > 0) Btn(w.t("Выбрать мод", "Pick a mod"), Modifier.fillMaxWidth().padding(top = 10.dp), Kind.LINE) { nav.catalog(null) }
    }
}

/** A day strip for a trial: 1 done, 0 missed, 2 today (not yet), 3 ahead. */
fun cells(h: Home, t: Trial): List<Int> = (0 until t.days).map { i ->
    val d = t.start + i
    when {
        h.s.done(d, t.mod) -> 1
        d < h.today -> 0
        d == h.today -> 2
        else -> 3
    }
}

@Composable
private fun TrialCard(h: Home, t: Trial, onClick: () -> Unit) {
    val w = h.w
    val m = h.cat[t.mod] ?: return
    val ready = t.ready(h.today)
    Panel(tick = if (ready) C.green else C.cyan, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(C.sys(m.sys))
            Text("${m.code} · ${w.sys(m.sys).uppercase()}", style = T.Label, modifier = Modifier.padding(start = 6.dp).weight(1f))
            Text(if (ready) w.t("ГОТОВ", "READY") else w.t("ДЕНЬ ${t.day(h.today)}/${t.days}", "DAY ${t.day(h.today)}/${t.days}"), style = T.Label.copy(color = if (ready) C.green else C.cyan))
        }
        Text(m.name(h.ru), style = T.Heading, modifier = Modifier.padding(vertical = 4.dp))
        Cells(cells(h, t), C.cyan)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(w.metric(m.sys.metric).uppercase(), style = T.Label, modifier = Modifier.width(96.dp))
            Spark((t.start - 7..minOf(h.today, t.last)).map { h.s.reading(it)?.get(m.sys.metric) }, C.sys(m.sys), Modifier.weight(1f).height(26.dp))
        }
        if (ready) Btn(w.t("Открыть протокол", "Open the protocol"), Modifier.fillMaxWidth().padding(top = 10.dp), Kind.GREEN, onClick = onClick)
    }
}

@Composable
private fun FirmwareRow(h: Home, t: Trial, onClick: () -> Unit) {
    val w = h.w
    val m = h.cat[t.mod] ?: return
    val integ = Engine.integrity(h.s, m, h.today)
    val faded = Engine.faded(h.s, m, h.today)
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(if (faded) C.faint else C.sys(m.sys)))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(m.name(h.ru), style = T.BodyM.copy(color = if (faded) C.muted else C.ink))
            Bar(integ, Modifier.padding(top = 4.dp), if (faded) C.faint else C.green, 4.dp)
        }
        Text(t.effect?.let { w.signed(it) } ?: "", style = T.Code.copy(color = if ((t.effect ?: 0f) > 0) C.green else C.muted), modifier = Modifier.width(56.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun ArchiveRow(h: Home, t: Trial, onClick: () -> Unit) {
    val w = h.w
    val m = h.cat[t.mod] ?: return
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(m.code, style = T.Label, modifier = Modifier.width(70.dp))
        Text(m.name(h.ru), style = T.Body.copy(color = C.muted), modifier = Modifier.weight(1f))
        Text(w.status(t.status).uppercase(), style = T.Label.copy(color = if (t.status == Status.OFF) C.red else C.faint))
    }
}
