package com.tnhc.shield.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tnhc.shield.data.db.entities.FilterListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FilterListDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: FilterListEntity)

    @Query("SELECT * FROM filter_lists ORDER BY isBuiltIn DESC, name ASC")
    fun observeAll(): Flow<List<FilterListEntity>>

    @Query("SELECT * FROM filter_lists WHERE enabled = 1")
    suspend fun getEnabled(): List<FilterListEntity>

    @Query("SELECT id FROM filter_lists")
    suspend fun getAllIds(): List<String>

    @Query("SELECT * FROM filter_lists WHERE id = :id")
    suspend fun getById(id: String): FilterListEntity?

    @Query("UPDATE filter_lists SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("UPDATE filter_lists SET lastUpdated = :lastUpdated, ruleCount = :ruleCount WHERE id = :id")
    suspend fun updateAfterDownload(id: String, lastUpdated: Long, ruleCount: Int)

    @Query("DELETE FROM filter_lists WHERE id = :id AND isBuiltIn = 0")
    suspend fun delete(id: String)
}
