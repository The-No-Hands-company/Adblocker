package com.tnhc.shield.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tnhc.shield.data.db.dao.BlockEventDao
import com.tnhc.shield.data.db.dao.CustomRuleDao
import com.tnhc.shield.data.db.dao.DnsLogDao
import com.tnhc.shield.data.db.dao.FilterListDao
import com.tnhc.shield.data.db.entities.BlockEventEntity
import com.tnhc.shield.data.db.entities.CustomRuleEntity
import com.tnhc.shield.data.db.entities.DnsLogEntity
import com.tnhc.shield.data.db.entities.FilterListEntity

@Database(
    entities = [
        BlockEventEntity::class,
        FilterListEntity::class,
        CustomRuleEntity::class,
        DnsLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ShieldDatabase : RoomDatabase() {
    abstract fun blockEventDao(): BlockEventDao
    abstract fun filterListDao(): FilterListDao
    abstract fun customRuleDao(): CustomRuleDao
    abstract fun dnsLogDao(): DnsLogDao
}
