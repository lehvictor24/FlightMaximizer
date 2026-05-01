package com.flightmaximizer.data.local.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.flightmaximizer.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val SCAN_INTERVAL_MINUTES = intPreferencesKey("scan_interval_minutes")
        val SCAN_ENABLED = booleanPreferencesKey("scan_enabled")
        val MIN_DELAY_MS = longPreferencesKey("min_delay_ms")
        val MAX_DELAY_MS = longPreferencesKey("max_delay_ms")
        val MAX_RETRIES = intPreferencesKey("max_retries")
        val USE_RANDOM_USER_AGENT = booleanPreferencesKey("use_random_user_agent")
        val CURRENCY = stringPreferencesKey("currency")
        val MAX_HISTORY_DAYS = intPreferencesKey("max_history_days")
        val KIWI_API_KEY = stringPreferencesKey("kiwi_api_key")
        val REQUIRE_WIFI = booleanPreferencesKey("require_wifi")
        val REQUIRE_CHARGING = booleanPreferencesKey("require_charging")
    }

    val scanIntervalMinutes: Flow<Int> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[Keys.SCAN_INTERVAL_MINUTES] ?: 60 }

    val scanEnabled: Flow<Boolean> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[Keys.SCAN_ENABLED] ?: false }

    val minDelayMs: Flow<Long> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[Keys.MIN_DELAY_MS] ?: 2000L }

    val maxDelayMs: Flow<Long> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[Keys.MAX_DELAY_MS] ?: 8000L }

    val maxRetries: Flow<Int> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[Keys.MAX_RETRIES] ?: 3 }

    val useRandomUserAgent: Flow<Boolean> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[Keys.USE_RANDOM_USER_AGENT] ?: true }

    val currency: Flow<String> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[Keys.CURRENCY] ?: "USD" }

    val maxHistoryDays: Flow<Int> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[Keys.MAX_HISTORY_DAYS] ?: 30 }

    val kiwiApiKey: Flow<String> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[Keys.KIWI_API_KEY] ?: "" }

    val requireWifi: Flow<Boolean> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[Keys.REQUIRE_WIFI] ?: false }

    val requireCharging: Flow<Boolean> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[Keys.REQUIRE_CHARGING] ?: false }

    suspend fun setScanIntervalMinutes(value: Int) {
        dataStore.edit { it[Keys.SCAN_INTERVAL_MINUTES] = value }
    }

    suspend fun setScanEnabled(value: Boolean) {
        dataStore.edit { it[Keys.SCAN_ENABLED] = value }
    }

    suspend fun setMinDelayMs(value: Long) {
        dataStore.edit { it[Keys.MIN_DELAY_MS] = value }
    }

    suspend fun setMaxDelayMs(value: Long) {
        dataStore.edit { it[Keys.MAX_DELAY_MS] = value }
    }

    suspend fun setMaxRetries(value: Int) {
        dataStore.edit { it[Keys.MAX_RETRIES] = value }
    }

    suspend fun setUseRandomUserAgent(value: Boolean) {
        dataStore.edit { it[Keys.USE_RANDOM_USER_AGENT] = value }
    }

    suspend fun setCurrency(value: String) {
        dataStore.edit { it[Keys.CURRENCY] = value }
    }

    suspend fun setMaxHistoryDays(value: Int) {
        dataStore.edit { it[Keys.MAX_HISTORY_DAYS] = value }
    }

    suspend fun setKiwiApiKey(value: String) {
        dataStore.edit { it[Keys.KIWI_API_KEY] = value }
    }

    suspend fun setRequireWifi(value: Boolean) {
        dataStore.edit { it[Keys.REQUIRE_WIFI] = value }
    }

    suspend fun setRequireCharging(value: Boolean) {
        dataStore.edit { it[Keys.REQUIRE_CHARGING] = value }
    }

    suspend fun getAppSettings(): AppSettings {
        val prefs = dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .first()
        return AppSettings(
            minDelayMs = prefs[Keys.MIN_DELAY_MS] ?: 2000L,
            maxDelayMs = prefs[Keys.MAX_DELAY_MS] ?: 8000L,
            maxRetries = prefs[Keys.MAX_RETRIES] ?: 3,
            useRandomUserAgent = prefs[Keys.USE_RANDOM_USER_AGENT] ?: true,
            currency = prefs[Keys.CURRENCY] ?: "USD",
            maxHistoryDays = prefs[Keys.MAX_HISTORY_DAYS] ?: 30,
            kiwiApiKey = prefs[Keys.KIWI_API_KEY] ?: "",
            scanIntervalMinutes = prefs[Keys.SCAN_INTERVAL_MINUTES] ?: 60,
            scanEnabled = prefs[Keys.SCAN_ENABLED] ?: false,
            requireWifi = prefs[Keys.REQUIRE_WIFI] ?: false,
            requireCharging = prefs[Keys.REQUIRE_CHARGING] ?: false
        )
    }
}
