package com.example.uhdlogger

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val settingsList = listOf(
    Setting("gain", "Gain", "5"),
    Setting("duration", "Duration", "60"),
    Setting("recv_frames", "Recv Frames", "512"),
    Setting("sent_frames", "Sent Frames", "512"),
    Setting("frequency", "Frequency", "98e6"),
    Setting("rate", "Rate", "10e6"),
    Setting("chroot_linux_path", "Linux Path", "/data/local/tmp/chrootubuntu"),
    Setting("uhd_path", "UHD Path", "/root/uhd"),
    Setting("log_path", "Log File Path", "/root/uhd_test_data")
)

class SettingsDataStore(private val context: Context) {

    private val dataStore: DataStore<Preferences> = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("uhd_settings") }
    )

    fun getSetting(key: String, default: String): Flow<String> {
        val preferenceKey = stringPreferencesKey(key)
        return dataStore.data
            .map { preferences ->
                preferences[preferenceKey] ?: default
            }
    }

    suspend fun saveSetting(key: String, value: String) {
        val preferenceKey = stringPreferencesKey(key)
        dataStore.edit { settings ->
            settings[preferenceKey] = value
        }
    }

    suspend fun initializeDefaults() {
        dataStore.edit { preferences ->
            for (setting in settingsList) {
                val key = stringPreferencesKey(setting.key)
                if (preferences[key] == null) {
                    preferences[key] = setting.defaultValue
                }
            }
        }
    }

}

@Suppress("StaticFieldLeak")
object AppSettingsDataStore {
    private var _instance: SettingsDataStore? = null

    fun init(context: Context) {
        if (_instance == null) {
            _instance = SettingsDataStore(context.applicationContext)
        }
    }

    val instance: SettingsDataStore
        get() = _instance
            ?: error("SettingsDataStore not initialized. Call AppSettingsDataStore.init(context) first.")
}

