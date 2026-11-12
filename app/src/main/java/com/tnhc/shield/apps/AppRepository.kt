package com.tnhc.shield.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.tnhc.shield.data.prefs.ShieldPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class AppInfo(
    val packageName: String,
    val label: String
)

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: ShieldPreferences
) {
    val excludedApps: Flow<Set<String>> = prefs.excludedApps

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { AppInfo(it.packageName, it.loadLabel(pm).toString()) }
            .sortedBy { it.label.lowercase() }
    }

    suspend fun setAppExcluded(packageName: String, excluded: Boolean) {
        val current = prefs.excludedApps.first().toMutableSet()
        if (excluded) current.add(packageName) else current.remove(packageName)
        prefs.setExcludedApps(current)
    }
}
