package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import com.example.model.Currency
import com.example.model.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hisabpro_prefs")

data class UserPreferences(
    val language: AppLanguage = AppLanguage.BN,
    val activeCurrency: Currency = Currency.BDT,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val autoCloudSync: Boolean = true,
    val lastCloudSyncMillis: Long = 0L
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val CURRENCY = stringPreferencesKey("active_currency")
        val THEME = stringPreferencesKey("theme_preference")
        val AUTO_CLOUD_SYNC = booleanPreferencesKey("auto_cloud_sync")
        val LAST_SYNC_MILLIS = longPreferencesKey("last_cloud_sync_millis")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            activeCurrency = Currency.fromCode(prefs[Keys.CURRENCY]),
            themePreference = ThemePreference.fromName(prefs[Keys.THEME]),
            autoCloudSync = prefs[Keys.AUTO_CLOUD_SYNC] ?: true,
            lastCloudSyncMillis = prefs[Keys.LAST_SYNC_MILLIS] ?: 0L
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setActiveCurrency(currency: Currency) {
        context.dataStore.edit { it[Keys.CURRENCY] = currency.code }
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setAutoCloudSync(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_CLOUD_SYNC] = enabled }
    }

    suspend fun setLastCloudSyncMillis(millis: Long) {
        context.dataStore.edit { it[Keys.LAST_SYNC_MILLIS] = millis }
    }
}
