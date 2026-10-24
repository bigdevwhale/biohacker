package app.biohacker

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import app.biohacker.data.DemoSeed
import app.biohacker.domain.Status
import app.biohacker.ui.Home
import app.biohacker.ui.Nav
import app.biohacker.ui.Words
import app.biohacker.ui.blueprint.BlueprintScreen
import app.biohacker.ui.catalog.CatalogScreen
import app.biohacker.ui.catalog.ModScreen
import app.biohacker.ui.components.Art
import app.biohacker.ui.intake.Intake
import app.biohacker.ui.lab.LabScreen
import app.biohacker.ui.lab.TrialScreen
import app.biohacker.ui.log.LogScreen
import app.biohacker.ui.parcel.ParcelScreen
import app.biohacker.ui.readings.ReadingsScreen
import app.biohacker.ui.theme.BiohackerTheme
import app.biohacker.ui.theme.C
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every main screen over the demo lab, drawn to PNGs (run with `-Prender`), in both languages. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ScreensBase {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    abstract val lang: String
    private val out by lazy { File(System.getProperty("render.out") ?: "build/art", "screens-$lang").apply { mkdirs() } }
    private val nav = Nav({}, {}, {}, {}, {}, {}, {})

    @Before fun still() { Art.still = true }

    private fun home(): Home = runBlocking {
        val app = ApplicationProvider.getApplicationContext<BiohackerApp>()
        val c = app.container
        DemoSeed.seedIfEmpty(c)
        c.repo.parcel(c.catalog)
        val s = c.repo.refresh()
        Home(s, c.settings.now(), Words(lang == "ru"), c.catalog, System.currentTimeMillis())
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.mainClock.autoAdvance = false
        rule.setContent { BiohackerTheme { Box(Modifier.fillMaxSize().background(C.paper)) { content() } } }
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(500)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            val image = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(image))
            File(out, "$name.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun intake() = shot("0-intake") { Intake(Words(lang == "ru")) {} }
    @Test fun blueprint() = home().let { h -> shot("1-blueprint") { BlueprintScreen(h, nav) } }
    @Test fun readings() = home().let { h -> shot("2-readings") { ReadingsScreen(h, nav) {} } }
    @Test fun lab() = home().let { h -> shot("3-lab") { LabScreen(h, nav) } }
    @Test fun protocol() = home().let { h -> h.s.running.firstOrNull { it.ready(h.today) }?.let { t -> shot("4-protocol") { TrialScreen(h, t.id, nav) {} } } ?: Unit }
    @Test fun trial() = home().let { h -> h.s.running.lastOrNull()?.let { t -> shot("4-trial") { TrialScreen(h, t.id, nav) {} } } ?: Unit }
    @Test fun catalog() = home().let { h -> shot("5-catalog") { CatalogScreen(h, nav, null) {} } }
    @Test fun card() = home().let { h -> shot("6-card") { ModScreen(h, h.cat.all.first { it.rare == 3 && it.ruWarn != null }.id, nav) {} } }
    @Test fun parcel() = home().let { h -> shot("7-parcel") { ParcelScreen(h, false, nav) {} } }
    @Test fun log() = home().let { h -> shot("8-log") { LogScreen(h, nav) } }
    @Test fun firmware() = home().let { h -> h.s.trials.firstOrNull { it.status == Status.ON }?.let { t -> shot("9-installed") { TrialScreen(h, t.id, nav) {} } } ?: Unit }
}

@Config(sdk = [34], qualifiers = "en-w411dp-h891dp-xxhdpi")
class RenderArtScreensEn : ScreensBase() { override val lang = "en" }

@Config(sdk = [34], qualifiers = "ru-w411dp-h891dp-xxhdpi")
class RenderArtScreensRu : ScreensBase() { override val lang = "ru" }
