package app.biohacker.ui.art

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.biohacker.domain.Sys
import app.biohacker.ui.components.Art
import app.biohacker.ui.theme.C
import app.biohacker.ui.theme.Mono
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/** How a node on the blueprint looks: installed, on trial, or installed but left alone. */
enum class NodeState { ON, RUN, FADED }

data class Node(val sys: Sys, val tagKey: String, val state: NodeState)

/** What the blueprint shows: the nodes by system (in install order), the labels, and combo links between tags. */
class Plan(val nodes: List<Node>, val labels: Map<Sys, String>, val links: List<List<String>>)

/**
 * The body drawn as a technical drawing, in units: x from −100 to 100 around the midline, y from 0 (crown)
 * to 400 (soles). Eight systems sit where they belong — the processor in the head with the clock as a dial
 * around it, cooling in the lungs, the battery at the heart, the furnace below, the engine in the legs,
 * the armour along the limbs and the network out past the hands.
 */
object Body {
    const val TOP = -14f
    const val BOTTOM = 404f
    val head = Offset(0f, 26f)
    const val HEAD_RX = 17f
    const val HEAD_RY = 23f
    const val RING = 37f

    /** The right half of the outline, from the neck down the arm and leg to the crotch; the left is its mirror. Eight heads tall, arms a little out. */
    private val right = listOf(
        Offset(7f, 50f), Offset(8f, 60f), Offset(20f, 66f), Offset(36f, 71f), Offset(45f, 78f), Offset(49.7f, 92f), Offset(53.9f, 118f),
        Offset(59.6f, 150f), Offset(63.5f, 160f), Offset(69.4f, 195f), Offset(73.7f, 222f), Offset(78.8f, 234f), Offset(81.3f, 252f), Offset(77.1f, 262f),
        Offset(70.8f, 258f), Offset(67.3f, 240f), Offset(63.9f, 224f), Offset(57.5f, 196f), Offset(51.6f, 162f), Offset(47.6f, 150f), Offset(40.9f, 118f),
        Offset(33f, 100f), Offset(33f, 118f), Offset(32f, 140f), Offset(28f, 168f), Offset(30f, 190f), Offset(35f, 212f), Offset(36f, 240f),
        Offset(33f, 272f), Offset(29f, 300f), Offset(31f, 325f), Offset(28f, 355f), Offset(20f, 382f), Offset(27f, 392f), Offset(25f, 398f),
        Offset(9f, 398f), Offset(9f, 384f), Offset(11f, 355f), Offset(12f, 325f), Offset(10f, 300f), Offset(8f, 272f), Offset(5f, 248f),
        Offset(0f, 236f),
    )

    val hubs: Map<Sys, Offset> = mapOf(
        Sys.SLEEP to Offset(head.x + RING * cos(-2.4f), head.y + RING * sin(-2.4f)),
        Sys.MIND to Offset(11f, 15f),
        Sys.CALM to Offset(-25f, 96f),
        Sys.JOY to Offset(14f, 134f),
        Sys.FUEL to Offset(-17f, 166f),
        Sys.SHIELD to Offset(46f, 82f),
        Sys.MOVE to Offset(-20f, 300f),
        Sys.LINK to Offset(76f, 262f),
    )

    /** Where each system's label sits (unit y) and on which side. */
    val labels: Map<Sys, Pair<Float, Boolean>> = mapOf(
        Sys.SLEEP to (2f to false), Sys.CALM to (102f to false), Sys.FUEL to (164f to false), Sys.MOVE to (300f to false),
        Sys.MIND to (16f to true), Sys.SHIELD to (76f to true), Sys.JOY to (136f to true), Sys.LINK to (206f to true),
    )

    /** The [i]-th point of a sunflower spread filling an ellipse at about [n] points. */
    private fun sunflower(c: Offset, rx: Float, ry: Float, i: Int, n: Int = 12): Offset {
        val k = i + 2
        val r = sqrt((k + .5f) / (n + 2)).coerceAtMost(1.35f)
        val a = k * 2.39996f
        return Offset(c.x + rx * r * cos(a), c.y + ry * r * sin(a))
    }

    /** The [i]-th node position of a system. */
    fun slot(s: Sys, i: Int): Offset = when (s) {
        Sys.MIND -> sunflower(Offset(0f, 22f), 11f, 10f, i)
        Sys.SLEEP -> {
            val ring = i / 11
            val k = i % 11
            val off = ((k + 1) / 2) * 22f * (if (k % 2 == 0) 1 else -1) - 11f
            val a = (-90f + off) * PI.toFloat() / 180f
            val r = RING + ring * 8f
            Offset(head.x + r * cos(a), head.y + r * sin(a))
        }
        Sys.CALM -> { val side = if (i % 2 == 0) -1f else 1f; sunflower(Offset(15f * side, 102f), 8f, 13f, i / 2, 7) }
        Sys.JOY -> sunflower(Offset(5f, 130f), 7f, 7f, i, 8)
        Sys.FUEL -> sunflower(Offset(0f, 162f), 16f, 10f, i)
        Sys.SHIELD -> {
            val side = if (i % 2 == 0) 1f else -1f
            val ys = floatArrayOf(100f, 124f, 148f, 172f, 196f, 220f, 112f, 136f, 160f, 184f, 208f)
            val y = ys[(i / 2) % ys.size]
            // down the middle of the arm, which leans out a little
            val x = 47.4f + (y - 118f) * .205f + (i / 22) * 4f
            Offset(x * side, y)
        }
        Sys.MOVE -> {
            val side = if (i % 2 == 0) -1f else 1f
            val ys = floatArrayOf(246f, 272f, 320f, 346f, 368f, 232f, 259f, 284f, 333f, 357f)
            val y = ys[(i / 2) % ys.size]
            val x = when { y < 290f -> 20f; y < 340f -> 20.5f; else -> 21f - (y - 340f) * .15f } + (i / 20) * 3f
            Offset(x * side, y)
        }
        Sys.LINK -> { val side = if (i % 2 == 0) 1f else -1f; sunflower(Offset(86f * side, 292f), 13f, 13f, i / 2, 6) }
    }

    /** A smooth closed outline: the corners cut three times (Chaikin), which rounds them without overshooting. */
    fun outline(map: (Offset) -> Offset): Path {
        var pts = right + right.reversed().drop(1).map { Offset(-it.x, it.y) }
        repeat(3) {
            pts = pts.indices.flatMap { i ->
                val a = pts[i]; val b = pts[(i + 1) % pts.size]
                listOf(a * .75f + b * .25f, a * .25f + b * .75f)
            }
        }
        val p = Path()
        pts.map(map).forEachIndexed { i, o -> if (i == 0) p.moveTo(o.x, o.y) else p.lineTo(o.x, o.y) }
        p.close()
        return p
    }
}

/** The blueprint itself. Tapping near a system's hub or label asks for that system. */
@Composable
fun Blueprint(plan: Plan, modifier: Modifier = Modifier, onSys: (Sys) -> Unit = {}) {
    val measurer = rememberTextMeasurer()
    val tr = rememberInfiniteTransition(label = "bp")
    val scan by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "scan")
    val pulse by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "pulse")
    val still = Art.still
    val nodes = remember(plan) { plan.nodes.groupBy { it.sys } }
    Canvas(
        modifier.pointerInput(plan) {
            detectTapGestures { tap ->
                val g = Geo(Size(size.width.toFloat(), size.height.toFloat()))
                val hit = Sys.entries.minByOrNull { s -> minOf(dist(tap, g.at(Body.hubs.getValue(s))), dist(tap, g.label(s))) }
                if (hit != null && minOf(dist(tap, g.at(Body.hubs.getValue(hit))), dist(tap, g.label(hit))) < 48.dp.toPx()) onSys(hit)
            }
        },
    ) {
        val g = Geo(size)
        drawBody(g)
        drawSystems(g, nodes, plan, measurer, if (still) .6f else pulse)
        if (!still) {
            val y = size.height * scan
            drawLine(C.amber.copy(alpha = .22f), Offset(0f, y), Offset(size.width, y), 1.5f)
            drawRect(C.amber.copy(alpha = .05f), Offset(0f, y - 14f), Size(size.width, 14f))
        }
    }
}

private fun dist(a: Offset, b: Offset) = hypot(a.x - b.x, a.y - b.y)

/** Units to pixels for a canvas of [size]. */
private class Geo(val size: Size) {
    val k = size.height / (Body.BOTTOM - Body.TOP)
    val cx = size.width / 2
    fun at(u: Offset) = Offset(cx + u.x * k, (u.y - Body.TOP) * k)
    fun y(u: Float) = (u - Body.TOP) * k
    /** Where a system's label text is anchored. */
    fun label(s: Sys): Offset {
        val (y, right) = Body.labels.getValue(s)
        return Offset(if (right) size.width * .8f else size.width * .2f, y(y))
    }
}

private fun DrawScope.drawBody(g: Geo) {
    val w = 1.6f * g.k
    // outline, doubled like an inked drawing
    val body = Body.outline { g.at(it) }
    drawPath(body, C.line.copy(alpha = .06f))
    drawPath(body, C.line, style = Stroke(w))
    val c = g.at(Body.head)
    drawOval(C.line.copy(alpha = .06f), Offset(c.x - Body.HEAD_RX * g.k, c.y - Body.HEAD_RY * g.k), Size(2 * Body.HEAD_RX * g.k, 2 * Body.HEAD_RY * g.k))
    drawOval(C.line, Offset(c.x - Body.HEAD_RX * g.k, c.y - Body.HEAD_RY * g.k), Size(2 * Body.HEAD_RX * g.k, 2 * Body.HEAD_RY * g.k), style = Stroke(w))
    // midline, dash-dot
    drawLine(C.faint.copy(alpha = .7f), g.at(Offset(0f, -8f)), g.at(Offset(0f, 400f)), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 6f, 3f, 6f)))
    // organs, dashed
    val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
    fun oval(cu: Offset, rx: Float, ry: Float) {
        val p = g.at(cu)
        drawOval(C.muted.copy(alpha = .55f), Offset(p.x - rx * g.k, p.y - ry * g.k), Size(2 * rx * g.k, 2 * ry * g.k), style = Stroke(1.2f, pathEffect = dash))
    }
    oval(Offset(0f, 22f), 12f, 11f)
    oval(Offset(-15f, 102f), 11f, 17f); oval(Offset(15f, 102f), 11f, 17f)
    oval(Offset(5f, 130f), 7f, 7f)
    oval(Offset(0f, 162f), 17f, 12f)
    // the clock dial round the head
    drawCircle(C.sys(Sys.SLEEP).copy(alpha = .45f), Body.RING * g.k, c, style = Stroke(1.2f, pathEffect = dash))
    for (i in 0 until 12) {
        val a = i * PI.toFloat() / 6
        val r1 = Body.RING * g.k; val r2 = r1 + 4f * g.k
        drawLine(C.sys(Sys.SLEEP).copy(alpha = .5f), Offset(c.x + r1 * cos(a), c.y + r1 * sin(a)), Offset(c.x + r2 * cos(a), c.y + r2 * sin(a)), 1f)
    }
}

private val LABEL = TextStyle(fontFamily = Mono, fontSize = 10.sp, letterSpacing = 1.sp)

private fun DrawScope.drawSystems(g: Geo, nodes: Map<Sys, List<Node>>, plan: Plan, tm: TextMeasurer, pulse: Float) {
    val r = 3.4f * g.k
    // tags that make up a found combo: their first installed node gets a gold ring
    val comboTags = plan.links.flatten().toSet()
    for (s in Sys.entries) {
        val col = C.sys(s)
        val hub = g.at(Body.hubs.getValue(s))
        val list = nodes[s].orEmpty()
        val lit = list.any { it.state == NodeState.ON }
        // leader line to the label
        val (_, right) = Body.labels.getValue(s)
        val lab = g.label(s)
        val elbow = Offset(lab.x, lab.y)
        drawLine(if (lit) col.copy(alpha = .8f) else C.faint, hub, Offset(hub.x, elbow.y), 1f)
        drawLine(if (lit) col.copy(alpha = .8f) else C.faint, Offset(hub.x, elbow.y), elbow, 1f)
        val name = plan.labels[s].orEmpty()
        val count = list.count { it.state != NodeState.RUN }
        val text = tm.measure(name, LABEL.copy(color = if (lit) C.ink else C.muted))
        val sub = tm.measure("${s.code} · $count", LABEL.copy(color = if (lit) col else C.faint, fontSize = 9.sp))
        val tx = if (right) elbow.x + 4f else elbow.x - 4f - text.size.width
        val sx = if (right) elbow.x + 4f else elbow.x - 4f - sub.size.width
        drawText(text, topLeft = Offset(tx, elbow.y - text.size.height - 1f))
        drawText(sub, topLeft = Offset(sx, elbow.y + 1f))
        // traces from the hub to each node: across, then down
        list.forEachIndexed { i, n ->
            val p = g.at(Body.slot(s, i))
            val c = when (n.state) { NodeState.ON -> col; NodeState.RUN -> C.cyan; NodeState.FADED -> C.faint }
            val a = if (n.state == NodeState.FADED) .4f else .55f
            drawLine(c.copy(alpha = a), hub, Offset(p.x, hub.y), 1f)
            drawLine(c.copy(alpha = a), Offset(p.x, hub.y), p, 1f)
        }
        list.forEachIndexed { i, n ->
            val p = g.at(Body.slot(s, i))
            when (n.state) {
                NodeState.ON -> {
                    if (n.tagKey in comboTags && list.indexOfFirst { it.tagKey == n.tagKey && it.state != NodeState.RUN } == i)
                        drawCircle(C.amber, r * 2.4f, p, style = Stroke(1.4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f))))
                    drawCircle(col.copy(alpha = .22f), r * 2.2f, p)
                    drawCircle(col, r, p)
                    drawCircle(Color.White.copy(alpha = .8f), r * .35f, p)
                }
                NodeState.RUN -> {
                    drawCircle(C.cyan.copy(alpha = .15f + .25f * pulse), r * (1.8f + pulse * .9f), p)
                    drawCircle(C.cyan, r, p, style = Stroke(1.6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f)), cap = StrokeCap.Round))
                }
                NodeState.FADED -> drawCircle(C.faint, r, p, style = Stroke(1.4f))
            }
        }
        // the hub
        drawCircle(C.paper, 6f * g.k, hub)
        drawCircle(if (lit) col else C.faint, 6f * g.k, hub, style = Stroke(1.6f, pathEffect = if (lit) null else PathEffect.dashPathEffect(floatArrayOf(4f, 3f))))
        if (lit) drawCircle(col, 2.6f * g.k, hub)
    }
}
