package app.biohacker.ui.readings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.biohacker.container
import app.biohacker.domain.Engine
import app.biohacker.domain.Metric
import app.biohacker.domain.Reading
import app.biohacker.ui.Home
import app.biohacker.ui.Nav
import app.biohacker.ui.components.Btn
import app.biohacker.ui.components.Chip
import app.biohacker.ui.components.EvBadge
import app.biohacker.ui.components.Gap
import app.biohacker.ui.components.Kind
import app.biohacker.ui.components.Panel
import app.biohacker.ui.components.Section
import app.biohacker.ui.components.Stamp
import app.biohacker.ui.components.TopBar
import app.biohacker.ui.components.grid
import app.biohacker.ui.theme.C
import app.biohacker.ui.theme.T
import java.time.LocalDate

/** The five gauges, one tap each; today, or yesterday if it was missed. */
@Composable
fun ReadingsScreen(h: Home, nav: Nav, onClose: () -> Unit) {
    val w = h.w
    val c = LocalContext.current.container
    val canYesterday = h.s.reading(h.today - 1) == null && h.s.readings.isNotEmpty()
    var day by remember { mutableStateOf(h.today) }
    val v = remember(day) { mutableStateListOf<Int?>().apply { val r = h.s.reading(day); Metric.entries.forEach { add(r?.get(it)) } } }
    var saved by remember { mutableStateOf<Reading?>(null) }
    Column(Modifier.fillMaxSize().grid()) {
        TopBar(w.t("Показания", "Readings"), w.date(LocalDate.ofEpochDay(day)) + " · " + w.t("сборка", "build") + " ${h.build + if (h.s.reading(day) == null) 1 else 0}", onBack = onClose)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            val done = saved
            if (done != null) {
                Saved(h, done, nav, onClose)
                return@Column
            }
            if (canYesterday) Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(w.t("Сегодня", "Today"), on = day == h.today) { day = h.today }
                Chip(w.t("Вчера", "Yesterday"), on = day == h.today - 1) { day = h.today - 1 }
            }
            Metric.entries.forEachIndexed { i, m -> Gauge(h, m, v[i], i) { c.sfx.tap(); v[i] = it } }
            Gap(8)
        }
        Btn(
            w.t("Записать показания", "Save readings"), Modifier.fillMaxWidth().padding(16.dp), enabled = saved == null && v.all { it != null },
        ) {
            val r = Reading(day, v.map { it ?: 3 })
            if (r.glitch) c.sfx.alarm() else c.sfx.sweep()
            c.write { repo.reading(day, r.v) }
            saved = r
        }
    }
}

@Composable
private fun Gauge(h: Home, m: Metric, value: Int?, index: Int, onPick: (Int) -> Unit) {
    val w = h.w
    val labels = w.scale(m)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("0${index + 1}", style = T.Label.copy(color = C.amber))
            Text(w.metric(m).uppercase(), style = T.Code.copy(color = C.ink, letterSpacing = 1.5.sp), modifier = Modifier.padding(start = 8.dp))
            Text(value?.let { labels[it - 1] } ?: "—", style = T.Label.copy(color = gaugeColor(value)), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
        Text(w.metricQ(m), style = T.Small, modifier = Modifier.padding(top = 2.dp, bottom = 6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for (k in 1..5) {
                val lit = value != null && k <= value
                Box(
                    Modifier.weight(1f).height(44.dp).background(if (lit) gaugeColor(value).copy(alpha = .25f + .15f * k) else C.card)
                        .border(1.dp, if (lit) gaugeColor(value) else C.faint).clickable { onPick(k) },
                    contentAlignment = Alignment.Center,
                ) { Text("$k", style = T.Code.copy(color = if (lit) C.ink else C.muted)) }
            }
        }
    }
}

private fun gaugeColor(v: Int?) = when (v) { null -> C.muted; 1, 2 -> C.red; 3 -> C.amber; else -> C.green }

@Composable
private fun Saved(h: Home, r: Reading, nav: Nav, onClose: () -> Unit) {
    val w = h.w
    Column(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Stamp(if (r.glitch) w.t("ГЛИТЧ", "GLITCH") else w.t("ЗАПИСАНО", "LOGGED"), if (r.glitch) C.red else C.green)
        Gap(16)
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (m in Metric.entries) Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${r[m]}", style = T.Big.copy(color = gaugeColor(r[m])))
                Text(w.metric(m).uppercase(), style = T.Label.copy(fontSize = 9.sp), maxLines = 1)
            }
        }
        Gap(10)
        val running = h.s.running.size
        Text(
            if (running > 0) w.t("Данные ушли в $running ${w.plural(running, "испытание", "испытания", "испытаний")}.", "Data sent to $running trial${if (running == 1) "" else "s"}.")
            else w.t("Данные сохранены — они станут базой для будущих испытаний.", "Saved — this becomes the baseline for future trials."),
            style = T.Body, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
    if (r.glitch) {
        val mods = Engine.emergency(h.cat, r)
        Section(w.t("Аварийный патч", "Emergency patch"), num = "!")
        Panel(line = C.red, tick = C.red) {
            Text(w.t("Тяжёлый день — это не провал, а данные. Попробуй что-то из этого:", "A hard day isn't a failure, it's data. Try one of these:"), style = T.Body)
            for (m in mods) Row(Modifier.fillMaxWidth().clickable { nav.mod(m.id) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                EvBadge(m.ev)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(m.name(h.ru), style = T.BodyM)
                    Text(m.how(h.ru), style = T.Small, maxLines = 2)
                }
            }
        }
    }
    Btn(w.t("Готово", "Done"), Modifier.fillMaxWidth().padding(16.dp), Kind.LINE, onClick = onClose)
}
