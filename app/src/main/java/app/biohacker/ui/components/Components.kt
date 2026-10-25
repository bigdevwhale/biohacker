package app.biohacker.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.biohacker.ui.theme.C
import app.biohacker.ui.theme.T

/** Screenshots want a still screen: no scanning line, no pulsing. */
object Art { @Volatile var still = false }

/**
 * A panel as on a drawing: a thin chalk frame with heavier corner ticks, over slightly lighter paper.
 */
fun Modifier.frame(bg: Color = C.card, line: Color = C.faint, tick: Color = C.line) = this.drawBehind {
    drawRect(bg)
    val s = 1.dp.toPx()
    drawRect(line, style = Stroke(s))
    val k = 8.dp.toPx(); val w = 2.dp.toPx()
    fun corner(x: Float, y: Float, dx: Float, dy: Float) {
        drawLine(tick, Offset(x, y), Offset(x + dx * k, y), w)
        drawLine(tick, Offset(x, y), Offset(x, y + dy * k), w)
    }
    corner(0f, 0f, 1f, 1f); corner(size.width, 0f, -1f, 1f); corner(0f, size.height, 1f, -1f); corner(size.width, size.height, -1f, -1f)
}

/** The blueprint grid behind every screen. */
fun Modifier.grid() = this.drawBehind {
    drawRect(C.paper)
    val step = 16.dp.toPx()
    var x = 0f
    var i = 0
    while (x < size.width) { drawLine(if (i % 5 == 0) C.gridHi.copy(alpha = .55f) else C.grid.copy(alpha = .5f), Offset(x, 0f), Offset(x, size.height), 1f); x += step; i++ }
    var y = 0f; i = 0
    while (y < size.height) { drawLine(if (i % 5 == 0) C.gridHi.copy(alpha = .55f) else C.grid.copy(alpha = .5f), Offset(0f, y), Offset(size.width, y), 1f); y += step; i++ }
}

enum class Kind { AMBER, GREEN, LINE, GHOST, RED }

@Composable
fun Btn(text: String, modifier: Modifier = Modifier, kind: Kind = Kind.AMBER, enabled: Boolean = true, small: Boolean = false, onClick: () -> Unit) {
    val (bg, fg, edge) = when (kind) {
        Kind.AMBER -> Triple(C.amber, C.onAmber, C.amber)
        Kind.GREEN -> Triple(C.green, Color(0xFF062414), C.green)
        Kind.LINE -> Triple(Color.Transparent, C.line, C.line)
        Kind.GHOST -> Triple(C.cardHi, C.ink, C.faint)
        Kind.RED -> Triple(Color.Transparent, C.red, C.red)
    }
    Box(
        modifier
            .heightIn(min = if (small) 38.dp else 50.dp)
            .background(if (enabled) bg else C.card)
            .border(1.dp, if (enabled) edge else C.faint)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (small) 12.dp else 16.dp, vertical = if (small) 6.dp else 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text.uppercase(), style = T.Button.copy(color = if (enabled) fg else C.faint, fontSize = if (small) 12.sp else 14.sp), textAlign = TextAlign.Center) }
}

@Composable
fun Panel(modifier: Modifier = Modifier, bg: Color = C.card, line: Color = C.faint, tick: Color = C.line, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).frame(bg, line, tick)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        content = content,
    )
}

@Composable
fun Chip(text: String, modifier: Modifier = Modifier, on: Boolean = false, color: Color = C.amber, onClick: (() -> Unit)? = null) {
    Box(
        modifier.background(if (on) color else Color.Transparent).border(1.dp, if (on) color else C.faint)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) { Text(text.uppercase(), style = T.Label.copy(color = if (on) C.onAmber else C.line)) }
}

/** A section heading: a mono label on a rule, like a drawing's title block. */
@Composable
fun Section(title: String, modifier: Modifier = Modifier, num: String? = null, link: String? = null, onLink: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 18.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (num != null) Text(num, style = T.Label.copy(color = C.amber), modifier = Modifier.padding(end = 8.dp))
        Text(title.uppercase(), style = T.Label.copy(color = C.line))
        Box(Modifier.weight(1f).padding(horizontal = 10.dp).height(1.dp).background(C.faint))
        if (link != null && onLink != null) Text(link.uppercase(), style = T.Label.copy(color = C.amber), modifier = Modifier.clickable(onClick = onLink).padding(6.dp))
    }
}

@Composable
fun TopBar(title: String, sub: String? = null, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, start: @Composable () -> Unit = {}, end: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().background(C.nav).statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).let { if (onBack != null) it.clickable(onClick = onBack) else it }, contentAlignment = Alignment.Center) {
            if (onBack != null) Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = C.line) else start()
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title.uppercase(), style = T.Code.copy(color = C.ink, letterSpacing = 2.sp, fontSize = 15.sp), textAlign = TextAlign.Center)
            if (sub != null) Text(sub, style = T.Label.copy(color = C.muted, letterSpacing = .5.sp), textAlign = TextAlign.Center)
        }
        Row(Modifier.size(44.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { end() }
    }
}

@Composable
fun Gap(h: Int = 10) = Spacer(Modifier.height(h.dp))

/** The evidence class as a small boxed letter. */
@Composable
fun EvBadge(e: Char, modifier: Modifier = Modifier, big: Boolean = false) {
    val c = C.ev(e)
    Box(modifier.size(if (big) 34.dp else 22.dp).border(1.5.dp, c), contentAlignment = Alignment.Center) {
        Text(e.toString(), style = T.Code.copy(color = c, fontSize = if (big) 20.sp else 13.sp))
    }
}

/** Rarity as one to three diamonds. */
@Composable
fun Rarity(r: Int, modifier: Modifier = Modifier) {
    val c = when (r) { 3 -> C.amber; 2 -> C.cyan; else -> C.faint }
    Text("◆".repeat(r) + "◇".repeat(3 - r), style = T.Label.copy(color = c, letterSpacing = 1.sp), modifier = modifier)
}

/** A square tick box with a chalk edge. */
@Composable
fun Tick(on: Boolean, color: Color = C.green, size: Dp = 30.dp, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Box(
        modifier.size(size).background(if (on) color else Color.Transparent).border(1.5.dp, if (on) color else C.line)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center,
    ) { if (on) Icon(Icons.Filled.Check, null, tint = C.paper, modifier = Modifier.size(size * .7f)) }
}

/** A rubber stamp, a little crooked. */
@Composable
fun Stamp(text: String, color: Color, modifier: Modifier = Modifier, angle: Float = -6f) {
    Box(modifier.rotate(angle).border(2.5.dp, color).padding(2.dp).border(1.dp, color).padding(horizontal = 12.dp, vertical = 5.dp)) {
        Text(text, style = T.Big.copy(color = color, fontSize = 22.sp, letterSpacing = 2.sp))
    }
}

/** A row of day cells for a trial: done, missed, today, ahead. */
@Composable
fun Cells(states: List<Int>, color: Color, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        for (s in states) {
            Box(
                Modifier.weight(1f).height(12.dp).background(when (s) { 1 -> color; else -> Color.Transparent })
                    .border(1.dp, when (s) { 1 -> color; 2 -> C.amber; 0 -> C.faint; else -> C.grid }),
            )
        }
    }
}

@Composable
fun Bar(p: Float, modifier: Modifier = Modifier, color: Color = C.amber, height: Dp = 6.dp) {
    Box(modifier.fillMaxWidth().height(height).background(C.grid)) {
        Box(Modifier.fillMaxWidth(p.coerceIn(0f, 1f)).height(height).background(color))
    }
}

/** A dot in a system's colour. */
@Composable
fun Dot(color: Color, size: Dp = 8.dp, modifier: Modifier = Modifier) = Box(modifier.size(size).background(color, CircleShape))

/** A small sparkline of daily values 1..5 (nulls are gaps). */
@Composable
fun Spark(values: List<Int?>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val dx = size.width / (values.size - 1)
        fun y(v: Int) = size.height - (v - 1) / 4f * size.height
        drawLine(C.grid, Offset(0f, y(3)), Offset(size.width, y(3)), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
        var prev: Offset? = null
        values.forEachIndexed { i, v ->
            if (v == null) { prev = null; return@forEachIndexed }
            val p = Offset(i * dx, y(v))
            prev?.let { drawLine(color, it, p, 2.dp.toPx()) }
            drawCircle(color, 2.dp.toPx(), p)
            prev = p
        }
    }
}

/** A sheet sliding up from the bottom over a dim backdrop; tapping the backdrop closes it. */
@Composable
fun BottomSheet(visible: Boolean, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (visible) BackHandler(onBack = onClose)
    AnimatedVisibility(visible, enter = fadeIn(tween(150)), exit = fadeOut(tween(150))) {
        Box(Modifier.fillMaxSize().background(C.shade).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose))
    }
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 24.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(visible, enter = slideInVertically(tween(220)) { it }, exit = slideOutVertically(tween(180)) { it }) {
            Column(
                Modifier.fillMaxWidth().frame(C.paper2, C.faint, C.line).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Box(Modifier.fillMaxWidth().padding(bottom = 10.dp), contentAlignment = Alignment.Center) { Box(Modifier.size(48.dp, 4.dp).background(C.faint)) }
                content()
            }
        }
    }
}

/** A key–value line in mono, like a spec sheet. */
@Composable
fun Spec(key: String, value: String, modifier: Modifier = Modifier, color: Color = C.ink) {
    Row(modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(key.uppercase(), style = T.Label, modifier = Modifier.width(118.dp))
        Text(value, style = T.Code.copy(color = color))
    }
}
