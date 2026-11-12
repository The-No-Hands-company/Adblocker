package com.tnhc.shield.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tnhc.shield.data.db.entities.BlockEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: BlockEventEntity)

    @Query("SELECT COUNT(*) FROM block_events WHERE timestamp > :since")
    fun countSince(since: Long): Flow<Long>

    @Query("SELECT COUNT(*) FROM block_events")
    fun countAll(): Flow<Long>

    @Query("SELECT domain, COUNT(*) as count FROM block_events GROUP BY domain ORDER BY count DESC LIMIT :limit")
    fun topBlockedDomains(limit: Int = 20): Flow<List<DomainCount>>

    @Query("SELECT * FROM block_events ORDER BY timestamp DESC LIMIT :limit")
    fun recentEvents(limit: Int = 50): Flow<List<BlockEventEntity>>

    @Query("DELETE FROM block_events WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)

    @Query("""
        SELECT (timestamp / 3600000) AS hour, COUNT(*) AS count
        FROM block_events WHERE timestamp > :since
        GROUP BY hour ORDER BY hour ASC
    """)
    fun blocksPerHour(since: Long): Flow<List<HourlyCount>>

    @Query("DELETE FROM block_events")
    suspend fun deleteAll()
}

data class DomainCount(val domain: String, val count: Int)
data class HourlyCount(val hour: Long, val count: Int)
