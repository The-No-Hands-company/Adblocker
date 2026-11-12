package com.tnhc.shield.stats

import com.tnhc.shield.data.db.dao.BlockEventDao
import com.tnhc.shield.data.db.dao.DomainCount
import com.tnhc.shield.data.db.entities.BlockEventEntity
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepository @Inject constructor(
    private val blockEventDao: BlockEventDao
) {

    private val sessionBlockCount = AtomicLong(0)
    private val sessionStartTime = System.currentTimeMillis()

    suspend fun recordBlock(domain: String, appPackage: String? = null) {
        sessionBlockCount.incrementAndGet()
        blockEventDao.insert(
            BlockEventEntity(
                domain = domain,
                timestamp = System.currentTimeMillis(),
                appPackage = appPackage
            )
        )
    }

    fun blockedToday(): Flow<Long> {
        val startOfDay = System.currentTimeMillis().let {
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        return blockEventDao.countSince(startOfDay)
    }

    fun blockedAllTime(): Flow<Long> = blockEventDao.countAll()

    fun topBlockedDomains(limit: Int = 20): Flow<List<DomainCount>> =
        blockEventDao.topBlockedDomains(limit)

    fun recentBlocks(limit: Int = 50) = blockEventDao.recentEvents(limit)

    fun sessionBlocks(): Long = sessionBlockCount.get()

    suspend fun clearOldEvents(olderThanDays: Int = 30) {
        val cutoff = System.currentTimeMillis() - olderThanDays * 24 * 60 * 60 * 1000L
        blockEventDao.deleteOlderThan(cutoff)
    }

    suspend fun clearAll() {
        blockEventDao.deleteAll()
        sessionBlockCount.set(0)
    }
}
