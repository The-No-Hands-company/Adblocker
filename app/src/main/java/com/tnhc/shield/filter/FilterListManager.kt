package com.tnhc.shield.filter

import android.content.Context
import com.tnhc.shield.data.db.dao.FilterListDao
import com.tnhc.shield.data.db.entities.FilterListEntity
import com.tnhc.shield.model.DefaultFilterLists
import com.tnhc.shield.model.FilterListDef
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FilterListManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val filterListDao: FilterListDao,
    private val httpClient: OkHttpClient,
    private val filterEngine: FilterEngine
) {

    private val listsDir = File(context.filesDir, "filter_lists").also { it.mkdirs() }

    suspend fun initialize() {
        ensureDefaultListsExist()
        reloadFilterEngine()
    }

    private suspend fun ensureDefaultListsExist() = withContext(Dispatchers.IO) {
        val existing = filterListDao.getAllIds()
        DefaultFilterLists.builtIn.forEach { def ->
            if (def.id !in existing) {
                filterListDao.insert(def.toEntity())
            }
        }
    }

    suspend fun downloadAndUpdate(listId: String): Result<Int> = withContext(Dispatchers.IO) {
        val entity = filterListDao.getById(listId) ?: return@withContext Result.failure(
            IllegalArgumentException("List not found: $listId")
        )
        try {
            val content = fetchContent(entity.url)
            val file = File(listsDir, "$listId.txt")
            file.writeText(content)
            val result = FilterListParser.parse(content, FilterFormat.valueOf(entity.format))
            filterListDao.updateAfterDownload(
                id = listId,
                lastUpdated = System.currentTimeMillis(),
                ruleCount = result.blockedDomains.size
            )
            reloadFilterEngine()
            Result.success(result.blockedDomains.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadAll(onProgress: (Int, Int) -> Unit = { _, _ -> }) = withContext(Dispatchers.IO) {
        val enabledLists = filterListDao.getEnabled()
        enabledLists.forEachIndexed { index, entity ->
            onProgress(index, enabledLists.size)
            try {
                val content = fetchContent(entity.url)
                val file = File(listsDir, "${entity.id}.txt")
                file.writeText(content)
                val result = FilterListParser.parse(content, FilterFormat.valueOf(entity.format))
                filterListDao.updateAfterDownload(
                    id = entity.id,
                    lastUpdated = System.currentTimeMillis(),
                    ruleCount = result.blockedDomains.size
                )
            } catch (_: Exception) {}
        }
        onProgress(enabledLists.size, enabledLists.size)
        reloadFilterEngine()
    }

    suspend fun reloadFilterEngine() = withContext(Dispatchers.IO) {
        val enabledLists = filterListDao.getEnabled()
        val results = enabledLists.mapNotNull { entity ->
            val file = File(listsDir, "${entity.id}.txt")
            if (!file.exists()) return@mapNotNull null
            try {
                FilterListParser.parse(file.readText(), FilterFormat.valueOf(entity.format))
            } catch (_: Exception) {
                null
            }
        }
        filterEngine.loadRules(results)
    }

    suspend fun setEnabled(listId: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        filterListDao.setEnabled(listId, enabled)
        reloadFilterEngine()
    }

    fun getAllLists(): Flow<List<FilterListEntity>> = filterListDao.observeAll()

    suspend fun addCustomList(def: FilterListDef) = withContext(Dispatchers.IO) {
        filterListDao.insert(def.toEntity())
        downloadAndUpdate(def.id)
    }

    suspend fun deleteList(listId: String) = withContext(Dispatchers.IO) {
        filterListDao.delete(listId)
        File(listsDir, "$listId.txt").delete()
        reloadFilterEngine()
    }

    private fun fetchContent(url: String): String {
        val request = Request.Builder().url(url)
            .header("User-Agent", "Shield-Adblocker/1.0")
            .build()
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string() ?: throw IOException("Empty response")
        }
    }

    private fun FilterListDef.toEntity() = FilterListEntity(
        id = id,
        name = name,
        description = description,
        url = url,
        format = format.name,
        category = category.name,
        enabled = enabled,
        isBuiltIn = isBuiltIn,
        lastUpdated = 0L,
        ruleCount = 0
    )
}
