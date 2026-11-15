package com.tnhc.shield.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tnhc.shield.data.db.entities.CustomRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomRuleDao {

    @Query("SELECT * FROM custom_rules ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CustomRuleEntity>>

    @Query("SELECT * FROM custom_rules")
    suspend fun getAll(): List<CustomRuleEntity>

    @Query("SELECT * FROM custom_rules WHERE domain = :domain LIMIT 1")
    suspend fun getByDomain(domain: String): CustomRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: CustomRuleEntity): Long

    @Query("DELETE FROM custom_rules WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM custom_rules WHERE domain = :domain")
    suspend fun deleteByDomain(domain: String)

    @Query("DELETE FROM custom_rules")
    suspend fun clearAll()
}
