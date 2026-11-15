package com.tnhc.shield.di

import com.tnhc.shield.dns.BootstrapDns
import com.tnhc.shield.dns.DohClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .dns(BootstrapDns)
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideDohClient(client: OkHttpClient): DohClient =
        DohClient("https://cloudflare-dns.com/dns-query", client)
}
