package app.biohacker.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import app.biohacker.domain.Mod
import app.biohacker.domain.Myth
import app.biohacker.domain.Status
import app.biohacker.domain.Sys
import app.biohacker.ui.Home
import app.biohacker.ui.Nav
import app.biohacker.ui.components.Chip
import app.biohacker.ui.components.EvBadge
import app.biohacker.ui.components.Gap
import app.biohacker.ui.components.Rarity
import app.biohacker.ui.components.TopBar
import app.biohacker.ui.components.frame
import app.biohacker.ui.components.grid
import app.biohacker.ui.theme.C
import app.biohacker.ui.theme.T

private enum class Filter { QUICK, LIGHT, NEW, RARE }

@Composable
fun CatalogScreen(h: Home, nav: Nav, sys: Sys?, onSys: (Sys?) -> Unit) {
    val w = h.w
    var myths by rememberSaveable { mutableStateOf(false) }
    var q by rememberSaveable { mutableStateOf("") }
    var ev by remember { mutableStateOf(setOf<Char>()) }
    var filters by remember { mutableStateOf(setOf<Filter>()) }
    val studied = h.s.bag.studied
    Column(Modifier.fillMaxSize().grid()) {
        TopBar(w.t("Картотека", "Card index"), w.t("изучено ${studied.count { h.cat[it] != null }} из ${h.cat.all.size} · мифов ${h.s.bag.myths.size}/${h.cat.myths.size}", "studied ${studied.count { h.cat[it] != null }} of ${h.cat.all.size} · myths ${h.s.bag.myths.size}/${h.cat.myths.size}"))
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(w.t("Протоколы", "Protocols"), on = !myths) { myths = false }
            Chip(w.t("Сломанные моды", "Broken mods"), on = myths, color = C.red) { myths = true }
        }
        if (myths) { Myths(h); return@Column }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).frame(C.card, C.faint, C.faint).padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, null, tint = C.muted, modifier = Modifier.size(20.dp))
            Box(Modifier.weight(1f).padding(start = 8.dp)) {
                if (q.isEmpty()) Text(w.t("Поиск: сон, холод, дыхание…", "Search: sleep, cold, breath…"), style = T.Body.copy(color = C.faint))
                BasicTextField(q, { q = it }, singleLine = true, textStyle = T.Body, cursorBrush = SolidColor(C.amber), modifier = Modifier.fillMaxWidth())
            }
            if (q.isNotEmpty()) Text("×", style = T.Code.copy(color = C.muted), modifier = Modifier.clickable { q = "" }.padding(horizontal = 6.dp))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Chip(w.t("Все", "All"), on = sys == null) { onSys(null) }
            for (s in Sys.entries) Chip(w.sys(s), on = sys == s, color = C.sys(s)) { onSys(if (sys == s) null else s) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (e in "ABCD") Chip("$e", on = e in ev, color = C.ev(e)) { ev = if (e in ev) ev - e else ev + e }
            Chip(w.t("≤5 мин", "≤5 min"), on = Filter.QUICK in filters) { filters = filters.toggle(Filter.QUICK) }
            Chip(w.t("Лёгкие", "Light"), on = Filter.LIGHT in filters) { filters = filters.toggle(Filter.LIGHT) }
            Chip(w.t("Не изучено", "Unread"), on = Filter.NEW in filters) { filters = filters.toggle(Filter.NEW) }
            Chip(w.t("Редкие", "Rare"), on = Filter.RARE in filters) { filters = filters.toggle(Filter.RARE) }
        }
        val words = q.trim().lowercase().split(' ').filter { it.isNotEmpty() }
        val list = remember(sys, ev, filters, q, studied, h.cat) {
            h.cat.all.filter { m ->
                (sys == null || m.sys == sys) && (ev.isEmpty() || m.ev in ev) &&
                    (Filter.QUICK !in filters || m.min <= 5) && (Filter.LIGHT !in filters || m.load == 1) &&
                    (Filter.NEW !in filters || m.id !in studied) && (Filter.RARE !in filters || m.rare >= 2) &&
                    words.all { wd -> m.name(h.ru).lowercase().contains(wd) || m.how(h.ru).lowercase().contains(wd) || w.tag(m.tag).contains(wd) || w.sys(m.sys).lowercase().contains(wd) }
            }
        }
        if (list.isEmpty()) Text(w.t("Ничего не нашлось.", "Nothing found."), style = T.Small, modifier = Modifier.padding(20.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            items(list, key = { it.id }) { m -> ModRow(h, m) { nav.mod(m.id) } }
            item { Gap(24) }
        }
    }
}

private fun <T> Set<T>.toggle(x: T) = if (x in this) this - x else this + x

@Composable
private fun ModRow(h: Home, m: Mod, onClick: () -> Unit) {
    val w = h.w
    val st = h.s.lastTrial(m.id)?.status
    val fresh = m.id !in h.s.bag.studied
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EvBadge(m.ev)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(C.sys(m.sys)))
                Text(m.code, style = T.Label, modifier = Modifier.padding(start = 6.dp, end = 8.dp))
                Rarity(m.rare)
                if (fresh) Text(w.t("  НОВАЯ", "  NEW"), style = T.Label.copy(color = C.amber))
            }
            Text(m.name(h.ru), style = T.BodyM.copy(color = if (fresh) C.ink else C.line))
            Text("${w.tag(m.tag)} · ${w.at(m.at)} · ${w.min(m.min)} · ${w.load(m.load)}", style = T.Small, maxLines = 1)
        }
        when (st) {
            Status.ON -> Text("✓", style = T.Code.copy(color = C.green))
            Status.RUN -> Text("◌", style = T.Code.copy(color = C.cyan))
            Status.OFF -> Text("×", style = T.Code.copy(color = C.red))
            else -> Unit
        }
    }
}

@Composable
private fun Myths(h: Home) {
    val w = h.w
    val found = h.s.bag.myths
    var open by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                w.t("Популярные хаки, которые не выдержали проверки. Их приносят посылки — собери все ${h.cat.myths.size}.", "Popular hacks that didn't survive the evidence. Parcels bring them — collect all ${h.cat.myths.size}."),
                style = T.Small, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
        items(h.cat.myths.sortedBy { if (it.id in found) 0 else 1 }, key = { it.id }) { m -> MythRow(h, m, m.id in found, open == m.id) { open = if (open == m.id) null else m.id } }
        item { Gap(24) }
    }
}

@Composable
fun MythRow(h: Home, m: Myth, found: Boolean, open: Boolean, onClick: () -> Unit) {
    val w = h.w
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).frame(C.card, if (found) C.red.copy(alpha = .5f) else C.grid, if (found) C.red else C.faint)
            .clickable(enabled = found, onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(m.code, style = T.Label.copy(color = if (found) C.red else C.faint), modifier = Modifier.width(80.dp))
            Text(if (found) m.name(h.ru) else "? ? ?", style = T.BodyM.copy(color = if (found) C.ink else C.faint), modifier = Modifier.weight(1f))
            if (found) Box(Modifier.border(1.dp, C.red).padding(horizontal = 5.dp, vertical = 1.dp)) { Text(w.t("СЛОМАН", "BROKEN"), style = T.Label.copy(color = C.red)) }
        }
        if (found && open) Text(m.truth(h.ru), style = T.Body, modifier = Modifier.padding(top = 8.dp))
    }
}
