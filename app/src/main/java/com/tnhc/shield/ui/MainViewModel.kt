package com.tnhc.shield.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tnhc.shield.apps.AppInfo
import com.tnhc.shield.apps.AppRepository
import com.tnhc.shield.data.db.dao.CustomRuleDao
import com.tnhc.shield.data.db.dao.DnsLogDao
import com.tnhc.shield.data.db.dao.HourlyCount
import com.tnhc.shield.data.db.entities.CustomRuleEntity
import com.tnhc.shield.data.db.entities.DnsLogEntity
import com.tnhc.shield.data.prefs.ShieldPreferences
import com.tnhc.shield.dns.DnsEngine
import com.tnhc.shield.dns.DohClient
import com.tnhc.shield.filter.FilterEngine
import com.tnhc.shield.filter.FilterListManager
import com.tnhc.shield.stats.StatsRepository
import com.tnhc.shield.vpn.ShieldVpnService
import com.tnhc.shield.work.FilterUpdateWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: ShieldPreferences,
    private val filterListManager: FilterListManager,
    private val filterEngine: FilterEngine,
    private val stats: StatsRepository,
    private val appRepository: AppRepository,
    private val dohClient: DohClient,
    private val dnsEngine: DnsEngine,
    private val customRuleDao: CustomRuleDao,
    private val dnsLogDao: DnsLogDao,
    private val okHttpClient: okhttp3.OkHttpClient
) : ViewModel() {

    val isVpnEnabled: StateFlow<Boolean> = prefs.isVpnEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val blockedToday: StateFlow<Long> = stats.blockedToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val blockedAllTime: StateFlow<Long> = stats.blockedAllTime()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val totalRules: StateFlow<Int> = flow {
        while (true) {
            emit(filterEngine.totalRules)
            kotlinx.coroutines.delay(5000)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentBlocks = stats.recentBlocks(20)
    val topBlockedDomains = stats.topBlockedDomains(15)
    val allFilterLists = filterListManager.getAllLists()

    val customRules: StateFlow<List<CustomRuleEntity>> = customRuleDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dnsLogs: StateFlow<List<DnsLogEntity>> = dnsLogDao.getRecentLogs(100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hourlyBlocks: StateFlow<List<HourlyCount>> = stats.blocksPerHour()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val excludedApps: StateFlow<Set<String>> = appRepository.excludedApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    fun loadApps() {
        if (_installedApps.value.isNotEmpty()) return
        viewModelScope.launch {
            _installedApps.value = appRepository.getInstalledApps()
        }
    }

    fun setAppExcluded(packageName: String, excluded: Boolean) {
        viewModelScope.launch { appRepository.setAppExcluded(packageName, excluded) }
    }

    fun setFilterEnabled(listId: String, enabled: Boolean) {
        viewModelScope.launch { filterListManager.setEnabled(listId, enabled) }
    }

    fun addCustomRule(domain: String, isWhitelist: Boolean, notes: String? = null) {
        val cleanDomain = domain.trim().lowercase().removePrefix("https://").removePrefix("http://").trimEnd('/')
        if (cleanDomain.isBlank()) return
        viewModelScope.launch {
            customRuleDao.insert(
                CustomRuleEntity(
                    domain = cleanDomain,
                    isWhitelist = isWhitelist,
                    notes = notes
                )
            )
            filterEngine.addUserRule(cleanDomain, block = !isWhitelist)
        }
    }

    fun deleteCustomRule(id: Long) {
        viewModelScope.launch {
            customRuleDao.delete(id)
            val all = customRuleDao.getAll()
            filterEngine.loadCustomRules(all)
        }
    }

    fun deleteCustomRuleByDomain(domain: String) {
        viewModelScope.launch {
            customRuleDao.deleteByDomain(domain.lowercase().trim())
            filterEngine.removeUserRule(domain)
        }
    }

    fun clearDnsLogs() {
        viewModelScope.launch {
            dnsLogDao.clearAll()
        }
    }

    private val _needsVpnPermission = MutableSharedFlow<Unit>()
    val needsVpnPermission = _needsVpnPermission.asSharedFlow()

    val dnsServerName: StateFlow<String> = prefs.dnsServerName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Cloudflare")
    val dnsServerUrl: StateFlow<String> = prefs.dnsServerUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "https://cloudflare-dns.com/dns-query")
    val autoStart: StateFlow<Boolean> = prefs.autoStart
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val autoUpdateFilters: StateFlow<Boolean> = prefs.autoUpdateFilters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _isUpdatingFilters = MutableStateFlow(false)
    val isUpdatingFilters: StateFlow<Boolean> = _isUpdatingFilters.asStateFlow()

    private val _filterUpdateProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val filterUpdateProgress: StateFlow<Pair<Int, Int>?> = _filterUpdateProgress.asStateFlow()

    fun updateAllFilters() {
        if (_isUpdatingFilters.value) return
        viewModelScope.launch {
            _isUpdatingFilters.value = true
            _filterUpdateProgress.value = Pair(0, 1)
            try {
                filterListManager.downloadAll { current, total ->
                    _filterUpdateProgress.value = Pair(current, total)
                }
                prefs.setLastFilterUpdate(System.currentTimeMillis())
            } catch (_: Exception) {
            } finally {
                _isUpdatingFilters.value = false
                _filterUpdateProgress.value = null
            }
        }
    }

    fun toggleAppExcluded(packageName: String) {
        val current = excludedApps.value.contains(packageName)
        setAppExcluded(packageName, !current)
    }

    private val _updateInfo = MutableStateFlow<com.tnhc.shield.util.UpdateInfo?>(null)
    val updateInfo: StateFlow<com.tnhc.shield.util.UpdateInfo?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    fun checkForAppUpdates() {
        if (_isCheckingUpdate.value) return
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            try {
                val currentVersion = try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
                } catch (_: Exception) {
                    "1.0.0"
                }
                val res = com.tnhc.shield.util.UpdateChecker.checkForUpdates(currentVersion, okHttpClient)
                _updateInfo.value = res.getOrNull()
            } catch (_: Exception) {
            } finally {
                _isCheckingUpdate.value = false
            }
        }
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val rules = customRuleDao.getAll()
        val excluded = prefs.excludedApps.first()
        val dnsUrl = prefs.dnsServerUrl.first()
        val dnsName = prefs.dnsServerName.first()

        val json = org.json.JSONObject()
        json.put("version", 1)
        json.put("exportedAt", System.currentTimeMillis())
        json.put("dnsServerUrl", dnsUrl)
        json.put("dnsServerName", dnsName)

        val rulesArray = org.json.JSONArray()
        rules.forEach { rule ->
            val obj = org.json.JSONObject()
            obj.put("domain", rule.domain)
            obj.put("isWhitelist", rule.isWhitelist)
            obj.put("notes", rule.notes ?: "")
            rulesArray.put(obj)
        }
        json.put("customRules", rulesArray)

        val excludedArray = org.json.JSONArray()
        excluded.forEach { excludedArray.put(it) }
        json.put("excludedApps", excludedArray)

        json.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val json = org.json.JSONObject(jsonString)
            if (json.has("dnsServerUrl") && json.has("dnsServerName")) {
                setDnsServer(json.getString("dnsServerUrl"), json.getString("dnsServerName"))
            }

            var count = 0
            if (json.has("customRules")) {
                val rulesArray = json.getJSONArray("customRules")
                for (i in 0 until rulesArray.length()) {
                    val obj = rulesArray.getJSONObject(i)
                    val domain = obj.getString("domain")
                    val isWhitelist = obj.getBoolean("isWhitelist")
                    val notes = if (obj.has("notes")) obj.getString("notes") else null
                    addCustomRule(domain, isWhitelist, notes)
                    count++
                }
            }

            if (json.has("excludedApps")) {
                val excludedArray = json.getJSONArray("excludedApps")
                for (i in 0 until excludedArray.length()) {
                    val pkg = excludedArray.getString(i)
                    setAppExcluded(pkg, true)
                }
            }

            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun setAutoStart(enabled: Boolean) { viewModelScope.launch { prefs.setAutoStart(enabled) } }
    fun setAutoUpdateFilters(enabled: Boolean) { viewModelScope.launch { prefs.setAutoUpdateFilters(enabled) } }
    fun setDnsServer(url: String, name: String) {
        viewModelScope.launch {
            prefs.setDnsServer(url, name)
            dohClient.updateServerUrl(url)
            dnsEngine.clearCache()
        }
    }

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing = _isInitializing.asStateFlow()

    init {
        viewModelScope.launch {
            filterListManager.initialize()
            FilterUpdateWorker.schedule(context)
            FilterUpdateWorker.scheduleOnce(context) // Trigger initial background sync
            _isInitializing.value = false
        }

        // Dynamically update DoH server and invalidate cache on preference changes
        viewModelScope.launch {
            prefs.dnsServerUrl.collect { url ->
                dohClient.updateServerUrl(url)
                dnsEngine.clearCache()
            }
        }

        // Synchronize custom rules into FilterEngine
        viewModelScope.launch {
            customRuleDao.observeAll().collect { rules ->
                filterEngine.loadCustomRules(rules)
            }
        }
    }

    fun toggleVpn(activity: Activity) {
        viewModelScope.launch {
            if (isVpnEnabled.value) {
                stopVpn()
            } else {
                val intent = VpnService.prepare(activity)
                if (intent != null) {
                    _needsVpnPermission.emit(Unit)
                } else {
                    startVpn()
                }
            }
        }
    }

    fun startVpn() {
        viewModelScope.launch {
            prefs.setVpnEnabled(true)
            context.startForegroundService(
                Intent(context, ShieldVpnService::class.java)
                    .setAction(ShieldVpnService.ACTION_START)
            )
        }
    }

    fun stopVpn() {
        viewModelScope.launch {
            prefs.setVpnEnabled(false)
            context.startService(
                Intent(context, ShieldVpnService::class.java)
                    .setAction(ShieldVpnService.ACTION_STOP)
            )
        }
    }

    fun refreshFilterLists() {
        viewModelScope.launch {
            FilterUpdateWorker.scheduleOnce(context)
        }
    }
}
