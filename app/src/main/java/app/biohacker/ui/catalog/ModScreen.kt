package app.biohacker.ui.catalog

import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.biohacker.container
import app.biohacker.domain.Engine
import app.biohacker.domain.Mod
import app.biohacker.ui.Home
import app.biohacker.ui.Nav
import app.biohacker.ui.Tab
import app.biohacker.ui.components.Btn
import app.biohacker.ui.components.EvBadge
import app.biohacker.ui.components.Gap
import app.biohacker.ui.components.Kind
import app.biohacker.ui.components.Panel
import app.biohacker.ui.components.Rarity
import app.biohacker.ui.components.Section
import app.biohacker.ui.components.Spec
import app.biohacker.ui.components.Stamp
import app.biohacker.ui.components.TopBar
import app.biohacker.ui.components.grid
import app.biohacker.ui.theme.C
import app.biohacker.ui.theme.T
import java.time.LocalDate

/** A protocol's full card; opening it counts as studied. */
@Composable
fun ModScreen(h: Home, id: String, nav: Nav, onClose: () -> Unit) {
    val w = h.w
    val c = LocalContext.current.container
    val m = h.cat[id]
    val wasNew = remember(id) { id !in h.s.bag.studied }
    val bonusEarned = remember(id) { wasNew && (h.s.bag.studied.size + 1) % Engine.STUDY_BONUS == 0 }
    var started by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id) { if (wasNew) c.repo.study(id) }
    Column(Modifier.fillMaxSize().grid()) {
        TopBar(m?.code ?: id, m?.let { w.sys(it.sys) + " · " + w.tag(it.tag) }, onBack = onClose)
        if (m == null) return@Column
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Card(h, m)
            if (bonusEarned) Panel(tick = C.violet, line = C.violet) {
                Text(w.t("+1 БОНУСНАЯ ПОСЫЛКА", "+1 BONUS PARCEL"), style = T.Label.copy(color = C.violet))
                Text(w.t("Ещё десять карточек изучено. Посылка ждёт на чертеже.", "Ten more cards studied. A parcel is waiting on the blueprint."), style = T.Small)
            }
            Section(w.t("Как", "How"), num = "01")
            Text(m.how(h.ru), style = T.Body.copy(fontSize = T.Body.fontSize * 1.07f), modifier = Modifier.padding(horizontal = 20.dp))
            Section(w.t("Почему", "Why"), num = "02")
            Text(m.why(h.ru), style = T.Body, modifier = Modifier.padding(horizontal = 20.dp))
            m.warn(h.ru)?.let { warn ->
                Panel(line = C.red, tick = C.red) {
                    Text(w.t("ОСТОРОЖНО", "CAUTION"), style = T.Label.copy(color = C.red))
                    Text(warn, style = T.Body, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Section(w.t("Доказательность", "Evidence"), num = "03")
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                EvBadge(m.ev, big = true)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(w.ev(m.ev).uppercase(), style = T.Label.copy(color = C.ev(m.ev)))
                    Text(w.evLong(m.ev), style = T.Small)
                }
            }
            val past = h.s.trials.filter { it.mod == m.id }
            if (past.isNotEmpty()) {
                Section(w.t("Испытания", "Trials"), num = "04")
                for (t in past) Text(
                    "${w.date(LocalDate.ofEpochDay(t.start))} · ${w.status(t.status)}" + (t.effect?.let { " · " + w.signed(it) } ?: ""),
                    style = T.Code, modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
                )
            }
            Gap(16)
        }
        Action(h, m, started, nav, onClose) { started = true }
    }
}

@Composable
private fun Card(h: Home, m: Mod) {
    val w = h.w
    Panel(tick = C.sys(m.sys)) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(C.sys(m.sys)))
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(w.rare(m.rare).uppercase(), style = T.Label.copy(color = when (m.rare) { 3 -> C.amber; 2 -> C.cyan; else -> C.muted }), modifier = Modifier.padding(end = 8.dp))
            Rarity(m.rare)
        }
        Text(m.name(h.ru), style = T.Display, modifier = Modifier.padding(vertical = 6.dp))
        Spec(w.t("Когда", "When"), w.at(m.at))
        Spec(w.t("Время", "Time"), w.min(m.min))
        Spec(w.t("Частота", "Frequency"), w.freq(m.freq))
        Spec(w.t("Нагрузка", "Load"), "${w.load(m.load)} · ${w.power(m.load)}")
        Spec(w.t("Прибор", "Gauge"), w.metric(m.sys.metric), color = C.sys(m.sys))
    }
}

@Composable
private fun Action(h: Home, m: Mod, started: Boolean, nav: Nav, onClose: () -> Unit, onStart: () -> Unit) {
    val w = h.w
    val c = LocalContext.current.container
    val mod = Modifier.fillMaxWidth().padding(16.dp)
    if (started) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Stamp(w.t("НА ИСПЫТАНИИ", "ON TRIAL"), C.cyan)
            Btn(w.t("В лабораторию", "To the lab"), mod, Kind.LINE) { onClose(); nav.tab(Tab.LAB) }
        }
        return
    }
    when (Engine.block(h.s, h.cat, m)) {
        null -> Btn(w.t("Поставить на испытание · 14 дней", "Start a 14-day trial"), mod) {
            c.sfx.stamp()
            c.write { repo.start(catalog, m) }
            onStart()
        }
        Engine.Block.RUNNING -> Btn(w.t("Уже на испытании", "Already on trial"), mod, Kind.LINE) { h.s.lastTrial(m.id)?.let { nav.trial(it.id) } }
        Engine.Block.INSTALLED -> Btn(w.t("В прошивке ✓", "In firmware ✓"), mod, Kind.LINE) { h.s.lastTrial(m.id)?.let { nav.trial(it.id) } }
        Engine.Block.POWER -> Column {
            Text(
                w.t("Нужно ${w.power(m.load)}, свободно ${h.capacity - h.used}. Заверши испытание или установи моды, чтобы мощности стало больше.", "Needs ${w.power(m.load)}, ${h.capacity - h.used} free. Finish a trial or install mods to get more power."),
                style = T.Small, modifier = Modifier.padding(horizontal = 16.dp),
            )
            Btn(w.t("Не хватает мощности", "Not enough power"), mod, enabled = false) {}
        }
    }
}
