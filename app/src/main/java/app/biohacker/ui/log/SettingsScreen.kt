package app.biohacker.ui.log

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.biohacker.BuildConfig
import app.biohacker.container
import app.biohacker.domain.Sys
import app.biohacker.notify.Bell
import app.biohacker.ui.Home
import app.biohacker.ui.components.Chip
import app.biohacker.ui.components.Gap
import app.biohacker.ui.components.Section
import app.biohacker.ui.components.TopBar
import app.biohacker.ui.components.frame
import app.biohacker.ui.components.grid
import app.biohacker.ui.theme.C
import app.biohacker.ui.theme.T

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(h: Home, onClose: () -> Unit) {
    val w = h.w
    val ctx = LocalContext.current
    val c = ctx.container
    var name by remember { mutableStateOf(h.s.bag.subject) }
    Column(Modifier.fillMaxSize().grid()) {
        TopBar(w.t("Настройки", "Settings"), onBack = { if (name != h.s.bag.subject) c.write { repo.subject(name) }; onClose() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Section(w.t("Испытуемый", "Subject"), num = "01")
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).frame(C.card, C.faint, C.faint).padding(12.dp)) {
                if (name.isEmpty()) Text(w.t("Имя", "Name"), style = T.Body.copy(color = C.faint))
                BasicTextField(name, { name = it.take(24); c.write { repo.subject(it.take(24)) } }, singleLine = true, textStyle = T.Body, cursorBrush = SolidColor(C.amber), modifier = Modifier.fillMaxWidth())
            }
            Section(w.t("Главные системы", "Main systems"), num = "02")
            Text(w.t("Посылки чаще приносят карточки из этих систем.", "Parcels bring cards from these systems more often."), style = T.Small, modifier = Modifier.padding(horizontal = 20.dp))
            FlowRow(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (s in Sys.entries) {
                    val on = s in h.s.bag.focus
                    Chip(w.sys(s), on = on, color = C.sys(s)) { c.write { repo.focus(if (on) h.s.bag.focus - s else h.s.bag.focus + s) } }
                }
            }
            Section(w.t("Напоминание о показаниях", "Readings reminder"), num = "03")
            FlowRow(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (m in listOf(-1, 9 * 60, 13 * 60, 19 * 60, 20 * 60, 21 * 60, 22 * 60)) {
                    Chip(if (m < 0) w.t("выкл", "off") else "%02d:00".format(m / 60), on = h.settings.bell == m) {
                        c.write { settings.update { it.copy(bell = m) }; Bell.plan(ctx) }
                    }
                }
            }
            Section(w.t("Звук", "Sound"), num = "04")
            Box(Modifier.padding(horizontal = 16.dp)) {
                Chip(if (h.settings.sound) w.t("включён", "on") else w.t("выключен", "off"), on = h.settings.sound) { c.write { settings.update { it.copy(sound = !it.sound) } } }
            }
            Section(w.t("Язык", "Language"), num = "05")
            FlowRow(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val cur = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                Chip("Русский", on = cur.startsWith("ru")) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru")) }
                Chip("English", on = cur.startsWith("en")) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }
                Chip(w.t("Как в системе", "System"), on = cur.isEmpty()) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList()) }
            }
            Section(w.t("Подсказки", "Hints"), num = "06")
            Text(w.t("ПОКАЗАТЬ ПОДСКАЗКИ ЗАНОВО ›", "SHOW THE HINTS AGAIN ›"), style = T.Label.copy(color = C.amber), modifier = Modifier.clickable { c.write { repo.coachReset() } }.padding(horizontal = 20.dp, vertical = 8.dp))
            Section(w.t("О приложении", "About"), num = "07")
            Text(
                w.t(
                    "Биохакер ${BuildConfig.VERSION_NAME}. Всё хранится только на телефоне: без аккаунтов, рекламы и интернета.\n\nКарточки — обобщение исследований, а не медицинский совет. Классы доказательности — честная, но грубая оценка. Если есть болезни, беременность или лекарства — посоветуйся с врачом.",
                    "Biohacker ${BuildConfig.VERSION_NAME}. Everything stays on your phone: no accounts, no ads, no internet.\n\nThe cards summarise research; they are not medical advice. Evidence classes are an honest but rough guide. If you have a condition, are pregnant or take medication, check with your doctor.",
                ),
                style = T.Small, modifier = Modifier.padding(horizontal = 20.dp),
            )
            Text(
                "GITHUB.COM/BIGDEVWHALE/BIOHACKER ›", style = T.Label.copy(color = C.amber),
                modifier = Modifier.clickable { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bigdevwhale/biohacker"))) } }.padding(horizontal = 20.dp, vertical = 10.dp),
            )
            Gap(24)
        }
    }
}
