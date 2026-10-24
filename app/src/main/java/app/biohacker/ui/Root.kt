package app.biohacker.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.biohacker.container
import app.biohacker.data.Settings
import app.biohacker.domain.Catalog
import app.biohacker.domain.Engine
import app.biohacker.domain.Parcel
import app.biohacker.domain.State
import app.biohacker.domain.Status
import app.biohacker.domain.Sys
import app.biohacker.domain.day
import app.biohacker.notify.Bell
import app.biohacker.ru
import app.biohacker.ui.art.Node
import app.biohacker.ui.art.NodeState
import app.biohacker.ui.art.Plan
import app.biohacker.ui.blueprint.BlueprintScreen
import app.biohacker.ui.catalog.CatalogScreen
import app.biohacker.ui.catalog.ModScreen
import app.biohacker.ui.components.BottomSheet
import app.biohacker.ui.components.Btn
import app.biohacker.ui.intake.Intake
import app.biohacker.ui.lab.LabScreen
import app.biohacker.ui.lab.TrialScreen
import app.biohacker.ui.log.LogScreen
import app.biohacker.ui.log.SettingsScreen
import app.biohacker.ui.parcel.ParcelScreen
import app.biohacker.ui.readings.ReadingsScreen
import app.biohacker.ui.theme.C
import app.biohacker.ui.theme.T
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId

/** Everything a screen reads, at one moment. */
class Home(val s: State, val settings: Settings, val w: Words, val cat: Catalog, val nowMs: Long) {
    val ru get() = w.ru
    val zone: ZoneId = ZoneId.systemDefault()
    val today: Long = nowMs.day(zone)
    val date: LocalDate = LocalDate.ofEpochDay(today)
    val parcel: Parcel? by lazy { Engine.parcel(s, cat, today) }
    val firmware by lazy { s.firmware }
    val capacity get() = Engine.capacity(s)
    val used by lazy { Engine.used(s, cat) }
    val major get() = Engine.major(firmware.size)
    val minor get() = Engine.minor(firmware.size)
    val build get() = s.readings.size
    val combos by lazy { Engine.combos(s, cat) }
    val weeks by lazy { Engine.weeks(s, cat, today) }

    /** What the blueprint draws. */
    val plan: Plan by lazy {
        val nodes = (s.firmware + s.running).sortedBy { if (it.status == Status.RUN) Long.MAX_VALUE else it.end }.mapNotNull { t ->
            val m = cat[t.mod] ?: return@mapNotNull null
            val st = when { t.status == Status.RUN -> NodeState.RUN; Engine.faded(s, m, today) -> NodeState.FADED; else -> NodeState.ON }
            Node(m.sys, m.tag.key, st)
        }
        Plan(nodes, Sys.entries.associateWith { w.sys(it).uppercase() }, combos.map { c -> c.tags.map { it.key } })
    }
}

enum class Tab(val coach: String) { BLUEPRINT("blueprint"), LAB("lab"), CATALOG("catalog"), LOG("log") }

sealed interface Overlay {
    data class Mod(val id: String) : Overlay
    data class Trial(val id: Long) : Overlay
    data object Readings : Overlay
    data class Parcel(val bonus: Boolean) : Overlay
    data object Settings : Overlay
}

/** What the screens can ask the root to do. */
class Nav(
    val tab: (Tab) -> Unit,
    val mod: (String) -> Unit,
    val trial: (Long) -> Unit,
    val readings: () -> Unit,
    val parcel: (Boolean) -> Unit,
    val catalog: (Sys?) -> Unit,
    val settings: () -> Unit,
)

@Composable
fun BiohackerRoot(settings: Settings) {
    val ctx = LocalContext.current
    val c = ctx.container
    val state by c.repo.state.collectAsStateWithLifecycle()
    val words = remember(ctx.ru) { Words(ctx.ru) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        c.repo.refresh()
        while (true) { delay(30_000); nowMs = System.currentTimeMillis() }
    }
    LaunchedEffect(settings.sound) { c.sfx.on = settings.sound }
    if (!settings.onboarded) {
        Intake(words, onDone = {})
        return
    }
    val s = state ?: run { Box(Modifier.fillMaxSize().background(C.paper)); return }
    val today = nowMs.day(ZoneId.systemDefault())
    LaunchedEffect(today) { c.repo.parcel(c.catalog); Bell.plan(ctx) }
    val home = Home(s, settings, words, c.catalog, nowMs)

    var tab by remember { mutableStateOf(Tab.BLUEPRINT) }
    var catSys by remember { mutableStateOf<Sys?>(null) }
    val overlays = remember { mutableStateListOf<Overlay>() }
    fun push(o: Overlay) = overlays.add(o)
    fun pop() { if (overlays.isNotEmpty()) overlays.removeAt(overlays.lastIndex) }
    BackHandler(enabled = overlays.isEmpty() && tab != Tab.BLUEPRINT) { tab = Tab.BLUEPRINT }
    val nav = Nav(
        tab = { overlays.clear(); tab = it },
        mod = { push(Overlay.Mod(it)) },
        trial = { push(Overlay.Trial(it)) },
        readings = { push(Overlay.Readings) },
        parcel = { push(Overlay.Parcel(it)) },
        catalog = { overlays.clear(); catSys = it; tab = Tab.CATALOG },
        settings = { push(Overlay.Settings) },
    )

    Box(Modifier.fillMaxSize().background(C.paper)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    Tab.BLUEPRINT -> BlueprintScreen(home, nav)
                    Tab.LAB -> LabScreen(home, nav)
                    Tab.CATALOG -> CatalogScreen(home, nav, catSys) { catSys = it }
                    Tab.LOG -> LogScreen(home, nav)
                }
            }
            TabBar(home, tab, onTab = { c.sfx.tap(); overlays.clear(); tab = it }, onReadings = { c.sfx.tap(); push(Overlay.Readings) })
        }
        when (val o = overlays.lastOrNull()) {
            is Overlay.Mod -> ModScreen(home, o.id, nav, onClose = { pop() })
            is Overlay.Trial -> TrialScreen(home, o.id, nav, onClose = { pop() })
            Overlay.Readings -> ReadingsScreen(home, nav, onClose = { pop() })
            is Overlay.Parcel -> ParcelScreen(home, o.bonus, nav, onClose = { pop() })
            Overlay.Settings -> SettingsScreen(home, onClose = { pop() })
            null -> Unit
        }
        val coachKey = when (overlays.lastOrNull()) { Overlay.Readings -> "readings"; null -> tab.coach; else -> null }
        Coach(home, coachKey)
    }
}

/** The lab's head explains a screen the first time it opens. */
@Composable
private fun Coach(h: Home, screen: String?) {
    val c = LocalContext.current.container
    var shown by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(screen, h.s.bag.coach) {
        shown = null
        if (screen != null && screen !in h.s.bag.coach) { delay(450); shown = screen }
    }
    val lines = shown?.let { h.w.coach(it) }.orEmpty()
    BottomSheet(visible = lines.isNotEmpty(), onClose = { shown?.let { k -> c.write { repo.coach(k) } }; shown = null }) {
        Text(h.w.t("ЗАВЕДУЮЩИЙ ЛАБОРАТОРИЕЙ", "HEAD OF THE LAB"), style = T.Label.copy(color = C.amber))
        if (lines.isNotEmpty()) {
            Text(lines[0], style = T.Title, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
            for (l in lines.drop(1)) Row(Modifier.padding(vertical = 5.dp)) {
                Text("›", style = T.Code.copy(color = C.amber), modifier = Modifier.width(18.dp))
                Text(l, style = T.Body)
            }
        }
        Btn(h.w.t("Понятно", "Got it"), Modifier.fillMaxWidth().padding(top = 12.dp)) { shown?.let { k -> c.write { repo.coach(k) } }; shown = null }
    }
}

@Composable
private fun TabBar(h: Home, tab: Tab, onTab: (Tab) -> Unit, onReadings: () -> Unit) {
    val w = h.w
    val items: List<Pair<Tab?, ImageVector>> = listOf(
        Tab.BLUEPRINT to Icons.Outlined.AccountTree, Tab.LAB to Icons.Outlined.Science, null to Icons.Outlined.Speed,
        Tab.CATALOG to Icons.Outlined.Style, Tab.LOG to Icons.AutoMirrored.Outlined.MenuBook,
    )
    val pending = h.s.reading(h.today) == null
    Row(
        Modifier.fillMaxWidth().background(C.nav).navigationBarsPadding().padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom,
    ) {
        for ((t, icon) in items) {
            if (t == null) {
                Column(
                    Modifier.width(76.dp).offset(y = (-12).dp).background(if (pending) C.amber else C.cardHi).border(1.dp, if (pending) C.amber else C.line)
                        .clickable(onClick = onReadings).padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(icon, null, tint = if (pending) C.onAmber else C.line, modifier = Modifier.size(28.dp))
                    Text(w.t("ЗАМЕР", "READ").uppercase(), style = T.Label.copy(color = if (pending) C.onAmber else C.line))
                }
                continue
            }
            val sel = t == tab
            val badge = when (t) {
                Tab.LAB -> h.s.running.any { it.ready(h.today) }
                Tab.LOG -> (h.weeks.firstOrNull()?.monday ?: -1) > h.s.bag.notesSeen
                else -> false
            }
            Column(
                Modifier.weight(1f).height(56.dp).clickable { onTab(t) }.padding(top = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box {
                    Icon(icon, null, tint = if (sel) C.amber else C.muted, modifier = Modifier.size(24.dp))
                    if (badge) Box(Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp).size(8.dp).background(C.amber))
                }
                Text(
                    when (t) { Tab.BLUEPRINT -> w.t("Чертёж", "Blueprint"); Tab.LAB -> w.t("Лаборатория", "Lab"); Tab.CATALOG -> w.t("Картотека", "Cards"); Tab.LOG -> w.t("Журнал", "Log") },
                    style = T.Small.copy(color = if (sel) C.amber else C.muted, fontSize = 11.sp), maxLines = 1,
                )
            }
        }
    }
}
