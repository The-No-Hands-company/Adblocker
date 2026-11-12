package com.tnhc.shield.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tnhc.shield.data.db.dao.BlockEventDao
import com.tnhc.shield.data.db.dao.FilterListDao
import com.tnhc.shield.data.db.entities.BlockEventEntity
import com.tnhc.shield.data.db.entities.FilterListEntity

@Database(
    entities = [BlockEventEntity::class, FilterListEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ShieldDatabase : RoomDatabase() {
    abstract fun blockEventDao(): BlockEventDao
    abstract fun filterListDao(): FilterListDao
}
