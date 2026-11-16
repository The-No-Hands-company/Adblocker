package com.tnhc.shield.di

import com.tnhc.shield.data.db.dao.DnsLogDao
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
    fun provideIpBlocklistEngine(): com.tnhc.shield.filter.IpBlocklistEngine =
        com.tnhc.shield.filter.IpBlocklistEngine()

    @Provides
    @Singleton
    fun provideDnsEngine(
        filterEngine: FilterEngine,
        dohClient: DohClient,
        statsRepository: StatsRepository,
        dnsLogDao: DnsLogDao,
        ipBlocklistEngine: com.tnhc.shield.filter.IpBlocklistEngine
    ): DnsEngine = DnsEngine(filterEngine, dohClient, statsRepository, dnsLogDao, ipBlocklistEngine)
}
