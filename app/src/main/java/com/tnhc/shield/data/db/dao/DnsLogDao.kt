package com.tnhc.shield.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tnhc.shield.data.db.entities.DnsLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DnsLogDao {

    @Query("SELECT * FROM dns_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<DnsLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: DnsLogEntity): Long

    @Query("DELETE FROM dns_logs WHERE timestamp < :before")
    suspend fun deleteOldLogs(before: Long)

    @Query("DELETE FROM dns_logs")
    suspend fun clearAll()
}
