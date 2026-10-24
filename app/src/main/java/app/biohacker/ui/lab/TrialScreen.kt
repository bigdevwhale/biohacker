package app.biohacker.ui.lab

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.biohacker.container
import app.biohacker.domain.Combo
import app.biohacker.domain.Engine
import app.biohacker.domain.Metric
import app.biohacker.domain.Result
import app.biohacker.domain.Status
import app.biohacker.domain.Trial
import app.biohacker.domain.Verdict
import app.biohacker.ui.Home
import app.biohacker.ui.Nav
import app.biohacker.ui.components.BottomSheet
import app.biohacker.ui.components.Btn
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

/** One trial: its day strip while it runs, its protocol once it's over, and the decision. */
@Composable
fun TrialScreen(h: Home, id: Long, nav: Nav, onClose: () -> Unit) {
    val w = h.w
    val c = LocalContext.current.container
    val t = h.s.trials.firstOrNull { it.id == id }
    val m = t?.let { h.cat[it.mod] }
    var installed by remember { mutableStateOf<Pair<Int, List<Combo>>?>(null) }
    var confirmDrop by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().grid()) {
        TopBar(m?.code ?: "", t?.let { w.status(it.status) }, onBack = onClose)
        if (t == null || m == null) return@Column
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Panel(onClick = { nav.mod(m.id) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EvBadge(m.ev)
                    Text("${w.sys(m.sys).uppercase()} · ${w.tag(m.tag)}", style = T.Label, modifier = Modifier.padding(start = 10.dp).weight(1f))
                    Text(w.t("КАРТОЧКА ›", "CARD ›"), style = T.Label.copy(color = C.amber))
                }
                Text(m.name(h.ru), style = T.Title, modifier = Modifier.padding(top = 6.dp))
                Text(m.how(h.ru), style = T.Body, modifier = Modifier.padding(top = 4.dp))
            }
            val combos = installed
            if (combos != null) { Installed(h, combos.first, combos.second); return@Column }
            val r = Engine.result(h.s, t, h.cat)
            val ready = t.ready(h.today)
            if (t.status == Status.RUN) {
                Section(if (ready) w.t("Испытание завершено", "Trial complete") else w.t("День ${t.day(h.today)} из ${t.days}", "Day ${t.day(h.today)} of ${t.days}"), num = "01")
                Strip(h, t)
                Text(
                    w.t("План: ${w.freq(m.freq)}. Выполнено ${r.done} из ${r.target}. Нажми на клетку, чтобы отметить забытый день.", "Plan: ${w.freq(m.freq)}. Done ${r.done} of ${r.target}. Tap a cell to mark a day you forgot."),
                    style = T.Small, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
            Section(if (ready || t.status != Status.RUN) w.t("Протокол испытания", "Trial protocol") else w.t("Предварительные данные", "Data so far"), num = "02")
            Protocol(h, r, final = ready || t.status != Status.RUN)
            Gap(8)
            when {
                ready -> Decide(h, t, r) { st ->
                    if (st == Status.ON) {
                        val before = h.combos.map { it.id }.toSet()
                        val after = Engine.combos(h.firmware.mapNotNull { h.cat[it.mod]?.tag }.toSet() + m.tag)
                        val fresh = after.filter { it.id !in before }
                        c.sfx.chime()
                        c.write { repo.finish(catalog, t, st); repo.combosSeen(after.map { it.id }.toSet()) }
                        installed = h.firmware.size to fresh
                    } else {
                        c.sfx.stamp()
                        c.write { repo.finish(catalog, t, st) }
                        onClose()
                    }
                }
                t.status == Status.RUN -> {
                    Btn(w.t("Прервать испытание", "Stop the trial"), Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), Kind.RED, small = true) { confirmDrop = true }
                }
                t.status == Status.ON -> {
                    Text(w.t("В прошивке с ${w.date(LocalDate.ofEpochDay(t.end))}.", "In the firmware since ${w.date(LocalDate.ofEpochDay(t.end))}."), style = T.Small, modifier = Modifier.padding(horizontal = 20.dp))
                    Btn(w.t("Снять с прошивки", "Remove from firmware"), Modifier.fillMaxWidth().padding(16.dp), Kind.RED, small = true) { c.write { repo.retire(t) }; onClose() }
                }
                else -> {
                    if (Engine.block(h.s, h.cat, m) == null) Btn(w.t("Испытать заново", "Try it again"), Modifier.fillMaxWidth().padding(16.dp), Kind.LINE) {
                        c.sfx.stamp(); c.write { repo.start(catalog, m) }; onClose()
                    }
                }
            }
            Gap(24)
        }
    }
    BottomSheet(visible = confirmDrop, onClose = { confirmDrop = false }) {
        Text(w.t("Прервать испытание?", "Stop the trial?"), style = T.Title)
        Text(w.t("Мод уйдёт в архив гипотез, мощность освободится. Попробовать снова можно в любой момент.", "The mod goes to the hypotheses archive and frees its power. You can try again any time."), style = T.Body, modifier = Modifier.padding(vertical = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(w.t("Отмена", "Cancel"), Modifier.weight(1f), Kind.LINE) { confirmDrop = false }
            Btn(w.t("Прервать", "Stop"), Modifier.weight(1f), Kind.RED) { confirmDrop = false; c.write { repo.finish(catalog, t!!, Status.DROP) }; onClose() }
        }
    }
}

/** The trial's days as tappable cells: tap a past (or today's) cell to mark it done or not. */
@Composable
private fun Strip(h: Home, t: Trial) {
    val c = LocalContext.current.container
    val cs = cells(h, t)
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        cs.forEachIndexed { i, s ->
            val d = t.start + i
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.fillMaxWidth().height(30.dp).background(if (s == 1) C.cyan else C.card)
                        .border(1.dp, when (s) { 1 -> C.cyan; 2 -> C.amber; 0 -> C.faint; else -> C.grid })
                        .clickable(enabled = d <= h.today) { val on = s != 1; if (on) c.sfx.blip() else c.sfx.tap(); c.write { repo.mark(d, t.mod, on) } },
                )
                Text("${i + 1}", style = T.Label.copy(color = if (d == h.today) C.amber else C.faint))
            }
        }
    }
}

/** Gauges with the mod against without it, and the verdict. */
@Composable
private fun Protocol(h: Home, r: Result, final: Boolean) {
    val w = h.w
    val v = r.verdict
    Panel(tick = verdictColor(v)) {
        if (final) Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) { Stamp(w.verdict(v), verdictColor(v)) }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp)) {
            Text(w.t("ПРИБОР", "GAUGE"), style = T.Label, modifier = Modifier.weight(1f))
            Text(w.t("БЕЗ", "OFF"), style = T.Label, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
            Text(w.t("С МОДОМ", "ON"), style = T.Label, modifier = Modifier.width(66.dp), textAlign = TextAlign.End)
            Text("Δ", style = T.Label, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
        }
        for (m in Metric.entries) {
            val d = r.delta(m)
            val main = m == r.main
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text((if (main) "▸ " else "  ") + w.metric(m), style = (if (main) T.BodyM else T.Body), modifier = Modifier.weight(1f))
                Text(r.base[m.ordinal]?.let { w.num(it) } ?: "—", style = T.Code.copy(color = C.muted), modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
                Text(r.on[m.ordinal]?.let { w.num(it) } ?: "—", style = T.Code, modifier = Modifier.width(66.dp), textAlign = TextAlign.End)
                Text(d?.let { w.signed(it) } ?: "—", style = T.Code.copy(color = when { d == null -> C.muted; d >= .3f -> C.green; d <= -.3f -> C.red; else -> C.line }), modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
            }
        }
        Text(
            w.t("Дней с модом: ${r.nOn} · без: ${r.nBase} · план выполнен на ${(r.adherence * 100).toInt()}%", "Days on: ${r.nOn} · off: ${r.nBase} · plan kept ${(r.adherence * 100).toInt()}%"),
            style = T.Small, modifier = Modifier.padding(top = 8.dp),
        )
        if (final) Text(w.verdictText(v), style = T.Body, modifier = Modifier.padding(top = 8.dp))
        else Text(w.t("Окончательный вывод — в конце испытания. Сравниваются дни, когда мод выполнен (и следующее утро), с днями без него, включая две недели до старта.", "The verdict comes at the end. Days with the mod (and the morning after) are compared with days without it, including the two weeks before the start."), style = T.Small, modifier = Modifier.padding(top = 6.dp))
    }
}

private fun verdictColor(v: Verdict) = when (v) { Verdict.WORKS -> C.green; Verdict.FLAT -> C.amber; Verdict.WORSE -> C.red; Verdict.THIN -> C.muted }

@Composable
private fun Decide(h: Home, t: Trial, r: Result, onPick: (Status) -> Unit) {
    val w = h.w
    val c = LocalContext.current.container
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(w.t("Решение за тобой: данные подсказывают, но лучше знаешь ты.", "Your call: the data hints, but you know best."), style = T.Small)
        Btn(w.t("Вшить в прошивку", "Install into firmware"), Modifier.fillMaxWidth(), if (r.verdict == Verdict.WORKS) Kind.GREEN else Kind.GHOST) { onPick(Status.ON) }
        Btn(w.t("Отклонить", "Reject"), Modifier.fillMaxWidth(), Kind.RED) { onPick(Status.OFF) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(w.t("+7 дней", "+7 days"), Modifier.weight(1f), Kind.LINE, small = true) { c.sfx.tap(); c.write { repo.extend(t) } }
            Btn(w.t("Неясно", "Unclear"), Modifier.weight(1f), Kind.LINE, small = true) { onPick(Status.UNSURE) }
        }
    }
}

@Composable
private fun Installed(h: Home, n: Int, combos: List<Combo>) {
    val w = h.w
    Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Stamp(w.t("УСТАНОВЛЕНО", "INSTALLED"), C.green)
        Gap(14)
        Text("${w.version(Engine.major(n), Engine.minor(n))} → ${w.version(Engine.major(n + 1), Engine.minor(n + 1))}", style = T.Big.copy(color = C.green))
        Text(w.t("Мод вшит в прошивку и загорелся на чертеже.", "The mod is in your firmware and lit on the blueprint."), style = T.Body, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
        if ((n + 1) % 2 == 0) Text(w.t("+1 к мощности", "+1 power"), style = T.Code.copy(color = C.cyan))
        Text(w.t("+1 бонусная посылка", "+1 bonus parcel"), style = T.Code.copy(color = C.violet), modifier = Modifier.padding(top = 4.dp))
    }
    for (cb in combos) Panel(tick = C.amber, line = C.amber) {
        Text(w.t("★ НАЙДЕНО КОМБО", "★ COMBO FOUND"), style = T.Label.copy(color = C.amber))
        Text(cb.name(h.ru), style = T.Title, modifier = Modifier.padding(vertical = 4.dp))
        Text(cb.tags.joinToString(" + ") { w.tag(it) }, style = T.Code.copy(color = C.muted))
        Text(cb.why(h.ru), style = T.Body, modifier = Modifier.padding(top = 6.dp))
    }
}
