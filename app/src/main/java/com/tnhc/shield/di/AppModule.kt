package com.tnhc.shield.di

import com.tnhc.shield.dns.DnsEngine
import com.tnhc.shield.dns.DohClient
import com.tnhc.shield.filter.FilterEngine
import com.tnhc.shield.stats.StatsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFilterEngine(): FilterEngine = FilterEngine()

    @Provides
    @Singleton
    fun provideDnsEngine(
        filterEngine: FilterEngine,
        dohClient: DohClient,
        statsRepository: StatsRepository
    ): DnsEngine = DnsEngine(filterEngine, dohClient, statsRepository)
}
