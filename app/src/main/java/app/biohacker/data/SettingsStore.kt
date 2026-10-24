package app.biohacker.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    val sound: Boolean = true,
    /** Minute of the day for the one daily reminder to take the readings; −1 = never. */
    val bell: Int = 21 * 60,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        sound = this[SOUND] ?: true,
        bell = this[BELL] ?: Settings().bell,
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[SOUND] = s.sound
            it[BELL] = s.bell
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val SOUND = booleanPreferencesKey("sound")
        val BELL = intPreferencesKey("bell")
    }
}
