package com.tnhc.shield.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dns_logs",
    indices = [Index(value = ["timestamp"])]
)
data class DnsLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val qtype: String = "A",
    val timestamp: Long = System.currentTimeMillis(),
    val isBlocked: Boolean,
    val ruleMatched: String? = null,
    val packageName: String? = null,
    val appName: String? = null
)
