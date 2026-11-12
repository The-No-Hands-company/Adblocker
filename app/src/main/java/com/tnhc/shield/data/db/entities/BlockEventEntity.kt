package com.tnhc.shield.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "block_events",
    indices = [Index("domain"), Index("timestamp")]
)
data class BlockEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val timestamp: Long = System.currentTimeMillis(),
    val appPackage: String? = null
)
