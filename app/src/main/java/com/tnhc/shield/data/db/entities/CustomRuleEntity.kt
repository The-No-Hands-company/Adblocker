package com.tnhc.shield.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "custom_rules",
    indices = [Index(value = ["domain"], unique = true)]
)
data class CustomRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val isWhitelist: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val notes: String? = null
)
