package app.biohacker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.biohacker.R
import app.biohacker.domain.Sys

/** The blueprint's colours: cyanotype paper, chalk-white lines, and the lab's signal lamps. */
@Immutable
object C {
    val paper = Color(0xFF0B2240)
    val paper2 = Color(0xFF0F2C52)
    val card = Color(0xFF123461)
    val cardHi = Color(0xFF184177)
    val grid = Color(0xFF1A3D6B)
    val gridHi = Color(0xFF24508A)
    val line = Color(0xFFD7E7FF)
    val ink = Color(0xFFEFF6FF)
    val muted = Color(0xFF8FB0DA)
    val faint = Color(0xFF4F75A6)
    val amber = Color(0xFFFFB547)
    val amberDeep = Color(0xFFB57514)
    val green = Color(0xFF5BE39A)
    val greenDeep = Color(0xFF1E7A4C)
    val cyan = Color(0xFF59D2FF)
    val red = Color(0xFFFF6E61)
    val violet = Color(0xFFB79CFF)
    val nav = Color(0xFF081A33)
    val shade = Color(0xC0050F1F)
    val onAmber = Color(0xFF231300)

    /** Each system's lamp on the blueprint. */
    fun sys(s: Sys) = when (s) {
        Sys.SLEEP -> Color(0xFFB79CFF)
        Sys.MOVE -> Color(0xFFFF9F5A)
        Sys.FUEL -> Color(0xFFFFD166)
        Sys.SHIELD -> Color(0xFF7FD4C1)
        Sys.MIND -> Color(0xFF59D2FF)
        Sys.CALM -> Color(0xFF8FC8FF)
        Sys.JOY -> Color(0xFFFF7EA8)
        Sys.LINK -> Color(0xFF8BE36A)
    }

    fun ev(e: Char) = when (e) { 'A' -> green; 'B' -> cyan; 'C' -> amber; else -> muted }
}

/** Labels, codes and numbers: IBM Plex Mono (OFL). */
val Mono = FontFamily(Font(R.font.plex_mono, FontWeight.Normal))

/** Reading text: Manrope (OFL). */
val Sans = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

object T {
    val Display = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp, color = C.ink)
    val Title = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 27.sp, color = C.ink)
    val Heading = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 23.sp, color = C.ink)
    val Body = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val BodyM = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp, color = C.ink)
    val Small = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 17.sp, color = C.muted)
    val Label = TextStyle(fontFamily = Mono, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.2.sp, color = C.muted)
    val Code = TextStyle(fontFamily = Mono, fontSize = 13.sp, lineHeight = 17.sp, color = C.line)
    val Big = TextStyle(fontFamily = Mono, fontSize = 26.sp, lineHeight = 30.sp, color = C.ink)
    val Button = TextStyle(fontFamily = Mono, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 1.sp)
}

@Composable
fun BiohackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.amber, onPrimary = C.onAmber, background = C.paper, surface = C.card, onSurface = C.ink,
            onBackground = C.ink, outline = C.line, error = C.red,
        ),
        typography = MaterialTheme.typography.copy(bodyLarge = T.Body, bodyMedium = T.Body, bodySmall = T.Small, labelLarge = T.Button, titleLarge = T.Title),
        content = content,
    )
}
