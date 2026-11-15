package com.tnhc.shield.di

import android.content.Context
import androidx.room.Room
import com.tnhc.shield.data.db.ShieldDatabase
import com.tnhc.shield.data.db.dao.BlockEventDao
import com.tnhc.shield.data.db.dao.CustomRuleDao
import com.tnhc.shield.data.db.dao.DnsLogDao
import com.tnhc.shield.data.db.dao.FilterListDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ShieldDatabase =
        Room.databaseBuilder(context, ShieldDatabase::class.java, "shield.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideBlockEventDao(db: ShieldDatabase): BlockEventDao = db.blockEventDao()

    @Provides
    fun provideFilterListDao(db: ShieldDatabase): FilterListDao = db.filterListDao()

    @Provides
    fun provideCustomRuleDao(db: ShieldDatabase): CustomRuleDao = db.customRuleDao()

    @Provides
    fun provideDnsLogDao(db: ShieldDatabase): DnsLogDao = db.dnsLogDao()
}
