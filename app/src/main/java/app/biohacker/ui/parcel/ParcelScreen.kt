package app.biohacker.ui.parcel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.biohacker.container
import app.biohacker.domain.Parcel
import app.biohacker.ui.Home
import app.biohacker.ui.Nav
import app.biohacker.ui.components.Btn
import app.biohacker.ui.components.EvBadge
import app.biohacker.ui.components.Gap
import app.biohacker.ui.components.Kind
import app.biohacker.ui.components.Panel
import app.biohacker.ui.components.Rarity
import app.biohacker.ui.components.TopBar
import app.biohacker.ui.components.grid
import app.biohacker.ui.theme.C
import app.biohacker.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A sealed parcel from the lab: tap to tear the tape, and a card comes out — sometimes a broken mod. */
@Composable
fun ParcelScreen(h: Home, bonus: Boolean, nav: Nav, onClose: () -> Unit) {
    val w = h.w
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val already = remember { if (!bonus) h.s.bag.parcel?.takeIf { it.day == h.today && it.opened } else null }
    var card by remember { mutableStateOf(already) }
    var opening by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val grow = remember { Animatable(if (already != null) 1f else 0f) }
    Column(Modifier.fillMaxSize().grid()) {
        TopBar(if (bonus) w.t("Бонусная посылка", "Bonus parcel") else w.t("Посылка дня", "Today's parcel"), w.date(h.date), onBack = onClose)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            val p = card
            if (p == null) {
                Gap(30)
                Box(
                    Modifier.size(260.dp).rotate(shake.value).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = !opening) {
                        opening = true
                        c.sfx.tear()
                        scope.launch {
                            repeat(6) { i -> shake.animateTo(if (i % 2 == 0) 4f else -4f, tween(60)) }
                            shake.animateTo(0f, tween(60))
                            val got = if (bonus) c.repo.openBonus(c.catalog) else c.repo.openParcel(c.catalog)
                            delay(120)
                            card = got
                            if (got == null) { onClose(); return@launch }
                            val rare = !got.myth && (h.cat[got.card]?.rare ?: 1) >= 3
                            if (rare) c.sfx.chime() else c.sfx.blip()
                            grow.animateTo(1f, tween(380))
                        }
                    },
                    contentAlignment = Alignment.Center,
                ) { Crate(Modifier.fillMaxSize()) }
                Text(w.t("Нажми, чтобы вскрыть", "Tap to open"), style = T.Label.copy(color = C.amber))
                Gap(10)
                Text(
                    w.t("Внутри — карточка протокола, которого ты ещё не видел. Иногда — сломанный мод.", "Inside: a card for a protocol you haven't seen yet. Sometimes a broken mod."),
                    style = T.Small, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp),
                )
            } else {
                Box(Modifier.fillMaxWidth().scale(.85f + .15f * grow.value)) { Reveal(h, p, nav) }
            }
        }
        if (card != null) Btn(w.t("Готово", "Done"), Modifier.fillMaxWidth().padding(16.dp), Kind.LINE, onClick = onClose)
    }
}

@Composable
private fun Reveal(h: Home, p: Parcel, nav: Nav) {
    val w = h.w
    Column(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (p.myth) {
            val m = h.cat.myth(p.card) ?: return
            Text(w.t("СЛОМАННЫЙ МОД", "BROKEN MOD"), style = T.Label.copy(color = C.red))
            Gap(8)
            Panel(line = C.red, tick = C.red) {
                Text(m.code, style = T.Label.copy(color = C.red))
                Text(m.name(h.ru), style = T.Title, modifier = Modifier.padding(vertical = 6.dp))
                Text(w.t("ЧТО ГОВОРЯТ ДАННЫЕ", "WHAT THE EVIDENCE SAYS"), style = T.Label, modifier = Modifier.padding(top = 4.dp))
                Text(m.truth(h.ru), style = T.Body, modifier = Modifier.padding(top = 4.dp))
            }
            Text(w.t("Добавлен в коллекцию сломанных модов", "Added to your broken-mod collection"), style = T.Small, modifier = Modifier.padding(top = 8.dp))
            return
        }
        val m = h.cat[p.card] ?: return
        val col = when (m.rare) { 3 -> C.amber; 2 -> C.cyan; else -> C.line }
        Text(w.rare(m.rare).uppercase() + w.t(" КАРТОЧКА", " CARD"), style = T.Label.copy(color = col))
        Rarity(m.rare, Modifier.padding(top = 4.dp))
        Gap(8)
        Panel(tick = col, line = col.copy(alpha = .6f), onClick = { nav.mod(m.id) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EvBadge(m.ev)
                Text("${m.code} · ${w.sys(m.sys).uppercase()}", style = T.Label, modifier = Modifier.padding(start = 10.dp))
            }
            Text(m.name(h.ru), style = T.Title, modifier = Modifier.padding(vertical = 6.dp))
            Text(m.how(h.ru), style = T.Body)
            Text(w.t("ОТКРЫТЬ КАРТОЧКУ ›", "OPEN THE CARD ›"), style = T.Label.copy(color = C.amber), modifier = Modifier.padding(top = 10.dp))
        }
    }
}

/** The parcel: an isometric crate in chalk, taped across, with a lab label. */
@Composable
private fun Crate(modifier: Modifier) {
    Canvas(modifier) {
        val s = size.minDimension
        val cx = size.width / 2; val cy = size.height / 2 + s * .04f
        val a = s * .34f
        val dx = a * cos(PI / 6).toFloat(); val dy = a * sin(PI / 6).toFloat()
        val top = Offset(cx, cy - a); val left = Offset(cx - dx, cy - a + dy); val right = Offset(cx + dx, cy - a + dy); val mid = Offset(cx, cy - a + 2 * dy)
        val h = a * 1.05f
        fun Offset.down() = Offset(x, y + h)
        val stroke = Stroke(2.2f)
        // faces
        val topFace = Path().apply { moveTo(top.x, top.y); lineTo(right.x, right.y); lineTo(mid.x, mid.y); lineTo(left.x, left.y); close() }
        val leftFace = Path().apply { moveTo(left.x, left.y); lineTo(mid.x, mid.y); lineTo(mid.down().x, mid.down().y); lineTo(left.down().x, left.down().y); close() }
        val rightFace = Path().apply { moveTo(mid.x, mid.y); lineTo(right.x, right.y); lineTo(right.down().x, right.down().y); lineTo(mid.down().x, mid.down().y); close() }
        drawPath(topFace, C.cardHi); drawPath(leftFace, C.card); drawPath(rightFace, C.paper2)
        drawPath(topFace, C.line, style = stroke); drawPath(leftFace, C.line, style = stroke); drawPath(rightFace, C.line, style = stroke)
        // tape along the top and down the front
        val t = .09f
        fun lerp(p: Offset, q: Offset, k: Float) = Offset(p.x + (q.x - p.x) * k, p.y + (q.y - p.y) * k)
        val tape = Path().apply {
            val a1 = lerp(top, left, .5f - t); val a2 = lerp(top, left, .5f + t)
            val b1 = lerp(right, mid, .5f - t); val b2 = lerp(right, mid, .5f + t)
            moveTo(a1.x, a1.y); lineTo(b1.x, b1.y); lineTo(b1.down().x, b1.down().y); lineTo(b2.down().x, b2.down().y); lineTo(b2.x, b2.y); lineTo(a2.x, a2.y); close()
        }
        drawPath(tape, C.amber.copy(alpha = .85f))
        drawPath(tape, C.amberDeep, style = Stroke(1.4f))
        // label on the left face, and a hazard-style dashed edge
        val l1 = lerp(left, mid, .2f); val l2 = lerp(left, mid, .7f)
        val lab = Path().apply {
            moveTo(l1.x, l1.y + h * .35f); lineTo(l2.x, l2.y + h * .35f); lineTo(l2.x, l2.y + h * .7f); lineTo(l1.x, l1.y + h * .7f); close()
        }
        drawPath(lab, C.line.copy(alpha = .15f))
        drawPath(lab, C.line, style = Stroke(1.4f))
        for (k in 1..3) {
            val y = .35f + k * .085f
            drawLine(C.line.copy(alpha = .7f), Offset(l1.x + 6f, l1.y + h * y), Offset(l2.x - 6f, l2.y + h * y), 1.2f)
        }
        drawLine(C.faint, Offset(cx - s * .42f, mid.down().y + s * .06f), Offset(cx + s * .42f, mid.down().y + s * .06f), 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f)))
    }
}
