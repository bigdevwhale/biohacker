package app.biohacker.ui.blueprint

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.biohacker.container
import app.biohacker.domain.Engine
import app.biohacker.domain.Mod
import app.biohacker.domain.Status
import app.biohacker.domain.Sys
import app.biohacker.domain.Trial
import app.biohacker.ui.Home
import app.biohacker.ui.Nav
import app.biohacker.ui.Tab
import app.biohacker.ui.art.Blueprint
import app.biohacker.ui.components.BottomSheet
import app.biohacker.ui.components.Btn
import app.biohacker.ui.components.Dot
import app.biohacker.ui.components.EvBadge
import app.biohacker.ui.components.Gap
import app.biohacker.ui.components.Kind
import app.biohacker.ui.components.Panel
import app.biohacker.ui.components.Section
import app.biohacker.ui.components.Tick
import app.biohacker.ui.components.TopBar
import app.biohacker.ui.components.frame
import app.biohacker.ui.components.grid
import app.biohacker.ui.theme.C
import app.biohacker.ui.theme.T

@Composable
fun BlueprintScreen(h: Home, nav: Nav) {
    val w = h.w
    var sys by remember { mutableStateOf<Sys?>(null) }
    Box(Modifier.fillMaxSize().grid()) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                w.t("Биохакер", "Biohacker"),
                "${h.s.bag.subject.ifBlank { w.t("испытуемый", "subject") }} · ${w.version(h.major, h.minor)} · ${w.t("сборка", "build")} ${h.build}",
                end = { Icon(Icons.Outlined.Settings, null, tint = C.muted, modifier = Modifier.size(40.dp).clickable(onClick = nav.settings).padding(8.dp)) },
            )
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Blueprint(h.plan, Modifier.fillMaxWidth().height(430.dp).padding(top = 8.dp)) { sys = it }
                Stats(h)
                Banners(h, nav)
                Today(h, nav)
                Gap(24)
            }
        }
        SystemSheet(h, sys, nav) { sys = null }
    }
}

@Composable
private fun Stats(h: Home) {
    val w = h.w
    val up = Engine.uptime(h.s, h.today)
    val cov = Engine.coverage(h.s, h.cat).size
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile(w.t("Аптайм", "Uptime"), w.days(up), C.green, Modifier.weight(1f))
        Tile(w.t("Мощность", "Power"), "${h.used}/${h.capacity}", if (h.used >= h.capacity) C.amber else C.cyan, Modifier.weight(1f))
        Tile(w.t("Покрытие", "Coverage"), "$cov/8", C.violet, Modifier.weight(1f))
    }
}

@Composable
private fun Tile(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier.frame(C.card, C.faint, color).padding(horizontal = 10.dp, vertical = 9.dp)) {
        Text(label.uppercase(), style = T.Label)
        Text(value, style = T.Code.copy(color = color, fontSize = T.Big.fontSize * .72f), maxLines = 1)
    }
}

@Composable
private fun Banners(h: Home, nav: Nav) {
    val w = h.w
    val c = LocalContext.current.container
    val reading = h.s.reading(h.today)
    if (reading == null) Banner(C.amber, w.t("ПОКАЗАНИЯ", "READINGS"), w.t("Сними сегодняшние показания — пять касаний", "Take today's readings — five taps"), nav.readings)
    h.s.running.firstOrNull { it.ready(h.today) }?.let { t ->
        val name = h.cat[t.mod]?.name(h.ru).orEmpty()
        Banner(C.green, w.t("ПРОТОКОЛ ГОТОВ", "PROTOCOL READY"), w.t("Испытание «$name» завершено. Смотрим данные", "The trial of “$name” is over. Let's see the data")) { nav.trial(t.id) }
    }
    val p = h.parcel
    if (p != null && !p.opened) Banner(C.violet, w.t("ПОСЫЛКА", "PARCEL"), w.t("Из лаборатории пришла посылка. Что внутри?", "A parcel from the lab. What's inside?")) { c.sfx.tap(); nav.parcel(false) }
    else if (h.s.bag.bonus > 0) Banner(C.violet, w.t("БОНУС", "BONUS"), w.t("Бонусных посылок: ${h.s.bag.bonus}", "Bonus parcels: ${h.s.bag.bonus}")) { c.sfx.tap(); nav.parcel(true) }
    h.weeks.firstOrNull()?.takeIf { it.monday > h.s.bag.notesSeen }?.let { wk ->
        Banner(C.cyan, w.t("ПАТЧ ${w.version(wk.major, wk.minor)}", "PATCH ${w.version(wk.major, wk.minor)}"), w.t("Вышли заметки к прошлой неделе", "Last week's patch notes are out")) { nav.tab(Tab.LOG) }
    }
    if (reading != null && reading.glitch) {
        val mods = Engine.emergency(h.cat, reading)
        if (mods.isNotEmpty()) {
            Section(w.t("Аварийный патч", "Emergency patch"), num = "!")
            Panel(line = C.red, tick = C.red) {
                Text(w.t("Глитч: ", "Glitch: ") + reading.low.joinToString(", ") { w.metric(it).lowercase() }, style = T.BodyM.copy(color = C.red))
                Text(w.t("Ничего не сломалось — просто тяжёлый день. Вот три коротких мода, которые могут помочь прямо сегодня:", "Nothing's broken — just a hard day. Three short mods that may help today:"), style = T.Small, modifier = Modifier.padding(vertical = 6.dp))
                for (m in mods) Row(Modifier.fillMaxWidth().clickable { nav.mod(m.id) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    EvBadge(m.ev)
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(m.name(h.ru), style = T.BodyM)
                        Text("${w.sys(m.sys)} · ${w.min(m.min)}", style = T.Small)
                    }
                    Text("›", style = T.Code.copy(color = C.amber))
                }
            }
        }
    }
}

@Composable
private fun Banner(color: Color, label: String, text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).frame(C.card, color.copy(alpha = .6f), color).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(color))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(label, style = T.Label.copy(color = color))
            Text(text, style = T.Body)
        }
        Text("›", style = T.Big.copy(color = color))
    }
}

@Composable
private fun Today(h: Home, nav: Nav) {
    val w = h.w
    val c = LocalContext.current.container
    val list = Engine.today(h.s, h.cat)
    val integ = Engine.firmwareIntegrity(h.s, h.cat, h.today)
    Section(w.t("Сегодня", "Today"), num = "01", link = integ?.let { w.t("прошивка ${(it * 100).toInt()}%", "firmware ${(it * 100).toInt()}%") }, onLink = { nav.tab(Tab.LAB) })
    if (list.isEmpty()) {
        Panel(onClick = { nav.catalog(null) }) {
            Text(w.t("Чертёж пока пуст", "The blueprint is empty for now"), style = T.Heading)
            Text(w.t("Выбери в картотеке первый мод и поставь его на испытание на 14 дней.", "Pick your first mod in the card index and put it on a 14-day trial."), style = T.Body, modifier = Modifier.padding(vertical = 6.dp))
            Btn(w.t("Открыть картотеку", "Open the card index"), Modifier.fillMaxWidth().padding(top = 6.dp)) { nav.catalog(null) }
        }
        return
    }
    var group: app.biohacker.domain.When? = null
    for ((m, t) in list) {
        if (m.at != group) {
            group = m.at
            Text(w.at(m.at).uppercase(), style = T.Label.copy(color = C.faint), modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 2.dp))
        }
        TodayRow(h, m, t, onOpen = { if (t.status == Status.RUN) nav.trial(t.id) else nav.mod(m.id) }) { on ->
            if (on) c.sfx.blip() else c.sfx.tap()
            c.write { repo.mark(h.today, m.id, on) }
        }
    }
}

@Composable
private fun TodayRow(h: Home, m: Mod, t: Trial, onOpen: () -> Unit, onTick: (Boolean) -> Unit) {
    val w = h.w
    val done = h.s.done(h.today, m.id)
    val run = t.status == Status.RUN
    val col = if (run) C.cyan else C.green
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp).frame(if (done) C.card.copy(alpha = .55f) else C.card, C.faint, col).padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Dot(C.sys(m.sys))
                Text(m.code, style = T.Label.copy(color = C.muted), modifier = Modifier.padding(start = 6.dp))
                Text(
                    if (run) w.t("  ИСПЫТАНИЕ ${t.day(h.today).coerceAtMost(t.days)}/${t.days}", "  TRIAL ${t.day(h.today).coerceAtMost(t.days)}/${t.days}") else w.t("  ПРОШИВКА", "  FIRMWARE"),
                    style = T.Label.copy(color = col),
                )
            }
            Text(m.name(h.ru), style = T.BodyM.copy(color = if (done) C.muted else C.ink), modifier = Modifier.padding(top = 2.dp))
            if (!m.daily) Text(w.t("на этой неделе ${Engine.thisWeek(h.s, m.id, h.today)} из ${m.freq}", "this week ${Engine.thisWeek(h.s, m.id, h.today)} of ${m.freq}"), style = T.Small)
        }
        Tick(done, col, modifier = Modifier.padding(start = 8.dp)) { onTick(!done) }
    }
}

@Composable
private fun SystemSheet(h: Home, sys: Sys?, nav: Nav, onClose: () -> Unit) {
    val w = h.w
    BottomSheet(visible = sys != null, onClose = onClose) {
        val s = sys ?: return@BottomSheet
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).border(2.dp, C.sys(s)))
            Text("${s.code} · ${w.sys(s).uppercase()}", style = T.Code.copy(color = C.sys(s)), modifier = Modifier.padding(start = 10.dp))
        }
        Text(w.sysTag(s).replaceFirstChar { it.uppercase() }, style = T.Title, modifier = Modifier.padding(top = 6.dp))
        val mine = (h.firmware + h.s.running).filter { h.cat[it.mod]?.sys == s }
        Text(
            w.t("Модов в прошивке: ${mine.count { it.status == Status.ON }} · на испытании: ${mine.count { it.status == Status.RUN }} · в картотеке: ${h.cat.of(s).size}",
                "In firmware: ${mine.count { it.status == Status.ON }} · on trial: ${mine.count { it.status == Status.RUN }} · in the index: ${h.cat.of(s).size}"),
            style = T.Small, modifier = Modifier.padding(vertical = 6.dp),
        )
        for (t in mine) {
            val m = h.cat[t.mod] ?: continue
            Row(Modifier.fillMaxWidth().clickable { onClose(); if (t.status == Status.RUN) nav.trial(t.id) else nav.mod(m.id) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(if (t.status == Status.RUN) C.cyan else C.green))
                Text(m.name(h.ru), style = T.Body, modifier = Modifier.weight(1f).padding(start = 10.dp))
                Text(w.status(t.status), style = T.Label)
            }
        }
        Btn(w.t("Картотека: ${w.sys(s)}", "Cards: ${w.sys(s)}"), Modifier.fillMaxWidth().padding(top = 12.dp), Kind.LINE) { onClose(); nav.catalog(s) }
        Gap(6)
    }
}
