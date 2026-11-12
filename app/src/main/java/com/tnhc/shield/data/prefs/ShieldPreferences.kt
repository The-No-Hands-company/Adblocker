package com.tnhc.shield.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("shield_prefs")

@Singleton
class ShieldPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val store = context.dataStore

    companion object {
        val KEY_VPN_ENABLED = booleanPreferencesKey("vpn_enabled")
        val KEY_DNS_SERVER_URL = stringPreferencesKey("dns_server_url")
        val KEY_DNS_SERVER_NAME = stringPreferencesKey("dns_server_name")
        val KEY_AUTO_START = booleanPreferencesKey("auto_start")
        val KEY_BLOCK_MODE = stringPreferencesKey("block_mode")
        val KEY_SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
        val KEY_LAST_FILTER_UPDATE = longPreferencesKey("last_filter_update")
        val KEY_AUTO_UPDATE_FILTERS = booleanPreferencesKey("auto_update_filters")
        val KEY_THEME = stringPreferencesKey("theme")
    }

    val isVpnEnabled: Flow<Boolean> = store.data.map { it[KEY_VPN_ENABLED] ?: false }
    val dnsServerUrl: Flow<String> = store.data.map { it[KEY_DNS_SERVER_URL] ?: "https://cloudflare-dns.com/dns-query" }
    val dnsServerName: Flow<String> = store.data.map { it[KEY_DNS_SERVER_NAME] ?: "Cloudflare" }
    val autoStart: Flow<Boolean> = store.data.map { it[KEY_AUTO_START] ?: false }
    val blockMode: Flow<String> = store.data.map { it[KEY_BLOCK_MODE] ?: "NXDOMAIN" }
    val showNotifications: Flow<Boolean> = store.data.map { it[KEY_SHOW_NOTIFICATIONS] ?: true }
    val lastFilterUpdate: Flow<Long> = store.data.map { it[KEY_LAST_FILTER_UPDATE] ?: 0L }
    val autoUpdateFilters: Flow<Boolean> = store.data.map { it[KEY_AUTO_UPDATE_FILTERS] ?: true }
    val theme: Flow<String> = store.data.map { it[KEY_THEME] ?: "system" }

    suspend fun setVpnEnabled(enabled: Boolean) = store.edit { it[KEY_VPN_ENABLED] = enabled }
    suspend fun setDnsServer(url: String, name: String) = store.edit {
        it[KEY_DNS_SERVER_URL] = url
        it[KEY_DNS_SERVER_NAME] = name
    }
    suspend fun setAutoStart(enabled: Boolean) = store.edit { it[KEY_AUTO_START] = enabled }
    suspend fun setBlockMode(mode: String) = store.edit { it[KEY_BLOCK_MODE] = mode }
    suspend fun setShowNotifications(show: Boolean) = store.edit { it[KEY_SHOW_NOTIFICATIONS] = show }
    suspend fun setLastFilterUpdate(time: Long) = store.edit { it[KEY_LAST_FILTER_UPDATE] = time }
    suspend fun setAutoUpdateFilters(enabled: Boolean) = store.edit { it[KEY_AUTO_UPDATE_FILTERS] = enabled }
    suspend fun setTheme(theme: String) = store.edit { it[KEY_THEME] = theme }
}
