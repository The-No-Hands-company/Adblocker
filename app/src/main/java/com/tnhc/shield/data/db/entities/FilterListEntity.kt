package com.tnhc.shield.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "filter_lists")
data class FilterListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val url: String,
    val format: String,
    val category: String,
    val enabled: Boolean = true,
    val isBuiltIn: Boolean = false,
    val lastUpdated: Long = 0L,
    val ruleCount: Int = 0
)
