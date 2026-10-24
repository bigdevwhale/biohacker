package app.biohacker

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.biohacker.data.DemoSeed
import app.biohacker.data.LabDb
import app.biohacker.data.Repo
import app.biohacker.data.SettingsStore
import app.biohacker.domain.Catalog
import app.biohacker.notify.Bell
import app.biohacker.sound.Sfx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = LabDb.create(app)
    val repo = Repo(db)
    val settings = SettingsStore(app)
    val sfx = Sfx(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** All the mods and myths, read from assets once. */
    val catalog: Catalog by lazy {
        fun read(f: String) = runCatching { app.assets.open(f).bufferedReader().use { it.readText() } }.getOrDefault("")
        Catalog.of(Catalog.files().map(::read), Catalog.mythFiles().map(::read))
    }

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }
}

class BiohackerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Bell.channel(this)
        container.scope.launch {
            container.catalog
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(container)
            container.repo.refresh()
            container.sfx.load()
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as BiohackerApp).container

/** Resources in the in-app language, which AppCompat doesn't localise outside activities before Android 13. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}

/** Is this context in Russian? */
val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
