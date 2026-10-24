package app.biohacker.ui.intake

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.biohacker.container
import app.biohacker.domain.Engine
import app.biohacker.domain.Mod
import app.biohacker.domain.Sys
import app.biohacker.notify.Bell
import app.biohacker.ui.Words
import app.biohacker.ui.art.Blueprint
import app.biohacker.ui.art.Plan
import app.biohacker.ui.components.Btn
import app.biohacker.ui.components.EvBadge
import app.biohacker.ui.components.Gap
import app.biohacker.ui.components.Kind
import app.biohacker.ui.components.frame
import app.biohacker.ui.components.grid
import app.biohacker.ui.theme.C
import app.biohacker.ui.theme.T

/** First visit: who the subject is, which systems matter most, and the first mod on trial. */
@Composable
fun Intake(w: Words, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var focus by remember { mutableStateOf(setOf<Sys>()) }
    var pick by remember { mutableStateOf<Mod?>(null) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    fun finish(m: Mod?) {
        c.write {
            repo.subject(name); repo.focus(focus); repo.begin()
            if (m != null) repo.start(catalog, m)
            settings.update { it.copy(onboarded = true) }
            Bell.plan(ctx)
        }
        if (Build.VERSION.SDK_INT >= 33) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
        onDone()
    }
    Column(Modifier.fillMaxSize().grid().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(w.t("ПРИЁМ В ЛАБОРАТОРИЮ", "LAB INTAKE"), style = T.Label.copy(color = C.amber), modifier = Modifier.weight(1f))
            Text("${step + 1}/3", style = T.Label)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (step) {
                0 -> {
                    val plan = remember(w) { Plan(emptyList(), Sys.entries.associateWith { w.sys(it).uppercase() }, emptyList()) }
                    Blueprint(plan, Modifier.fillMaxWidth().height(330.dp))
                    Text(w.t("Биохакер", "Biohacker"), style = T.Display, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                    Text(
                        w.t("Твоё тело — восемь систем. В картотеке больше тысячи протоколов для сна, движения, еды, спокойствия, радости и связей с людьми — у каждого честный класс доказательности.",
                            "Your body is eight systems. The card index holds over a thousand protocols for sleep, movement, food, calm, joy and people — each with an honest evidence class."),
                        style = T.Body, modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Text(
                        w.t("Ты — испытуемый. Каждый мод проверяется на тебе 14 дней, и только данные решают, вшить ли его в прошивку.",
                            "You are the subject. Every mod is tried on you for 14 days, and the data decides whether it goes into your firmware."),
                        style = T.Body, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                    Text(w.t("КАК ТЕБЯ ЗАПИСАТЬ?", "NAME FOR THE RECORDS?"), style = T.Label, modifier = Modifier.padding(start = 20.dp, top = 6.dp))
                    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).frame(C.card, C.faint, C.line).padding(12.dp)) {
                        if (name.isEmpty()) Text(w.t("Имя (можно пропустить)", "Name (optional)"), style = T.Body.copy(color = C.faint))
                        BasicTextField(name, { name = it.take(24) }, singleLine = true, textStyle = T.Body, cursorBrush = SolidColor(C.amber), modifier = Modifier.fillMaxWidth())
                    }
                }
                1 -> {
                    Text(w.t("Что важнее всего сейчас?", "What matters most right now?"), style = T.Title, modifier = Modifier.padding(horizontal = 20.dp))
                    Text(w.t("Выбери одну–три системы. Посылки будут чаще приносить карточки для них. Поменять можно в любой момент.", "Pick one to three systems. Parcels will lean towards them. You can change this any time."), style = T.Small, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                    for (s in Sys.entries) {
                        val on = s in focus
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp).frame(if (on) C.cardHi else C.card, if (on) C.sys(s) else C.faint, C.sys(s))
                                .clickable { c.sfx.tap(); focus = if (on) focus - s else if (focus.size < 3) focus + s else focus }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(14.dp).border(2.dp, C.sys(s)).padding(3.dp)) { if (on) Box(Modifier.fillMaxSize().background(C.sys(s))) }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text("${s.code} · ${w.sys(s).uppercase()}", style = T.Label.copy(color = C.sys(s)))
                                Text(w.sysTag(s).replaceFirstChar { it.uppercase() }, style = T.BodyM)
                            }
                        }
                    }
                }
                else -> {
                    Text(w.t("Первое испытание", "Your first trial"), style = T.Title, modifier = Modifier.padding(horizontal = 20.dp))
                    Text(w.t("Лёгкие моды с самыми сильными данными. Выбери один — он сразу встанет на испытание.", "Light mods with the strongest evidence. Pick one — it goes on trial right away."), style = T.Small, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
                    for (m in remember(focus) { Engine.starters(c.catalog, focus) }) {
                        val on = pick == m
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).frame(if (on) C.cardHi else C.card, if (on) C.amber else C.faint, if (on) C.amber else C.line)
                                .clickable { c.sfx.tap(); pick = m }.padding(12.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                EvBadge(m.ev)
                                Text("${m.code} · ${w.sys(m.sys).uppercase()} · ${w.min(m.min)}", style = T.Label, modifier = Modifier.padding(start = 10.dp))
                            }
                            Text(m.name(w.ru), style = T.Heading, modifier = Modifier.padding(top = 6.dp))
                            Text(m.how(w.ru), style = T.Small.copy(color = C.line))
                        }
                    }
                }
            }
            Gap(12)
        }
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            when (step) {
                0 -> Btn(w.t("Дальше", "Next"), Modifier.fillMaxWidth()) { c.sfx.tap(); step = 1 }
                1 -> Btn(w.t("Дальше", "Next"), Modifier.fillMaxWidth(), enabled = focus.isNotEmpty()) { c.sfx.tap(); step = 2 }
                else -> {
                    Btn(w.t("Начать испытание", "Start the trial"), Modifier.fillMaxWidth(), enabled = pick != null) { c.sfx.stamp(); finish(pick) }
                    Btn(w.t("Сам выберу в картотеке", "I'll pick in the card index"), Modifier.fillMaxWidth().padding(top = 8.dp), Kind.LINE, small = true) { finish(null) }
                }
            }
        }
    }
}
