package com.tnhc.shield.dns

import com.tnhc.shield.filter.FilterEngine
import com.tnhc.shield.stats.StatsRepository
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

data class DnsCacheEntry(
    val response: ByteArray,
    val expiresAt: Long
)

class DnsEngine(
    private val filterEngine: FilterEngine,
    private val dohClient: DohClient,
    private val stats: StatsRepository
) {

    private val cache = ConcurrentHashMap<String, DnsCacheEntry>()
    private val cacheMaxSize = 2048

    suspend fun handleDnsQuery(rawQuery: ByteArray): ByteArray {
        val query = DnsPacketParser.parseQuery(rawQuery) ?: return rawQuery

        // Check whitelist first
        if (filterEngine.isWhitelisted(query.domain)) {
            return forwardOrCached(query, rawQuery)
        }

        // Check blocklist
        if (filterEngine.isBlocked(query.domain)) {
            stats.recordBlock(query.domain)
            return DnsPacketParser.buildNxdomainResponse(rawQuery)
        }

        return forwardOrCached(query, rawQuery)
    }

    private suspend fun forwardOrCached(query: DnsQuery, rawQuery: ByteArray): ByteArray {
        val cacheKey = "${query.domain}:${query.qtype}"
        val cached = cache[cacheKey]
        if (cached != null && System.currentTimeMillis() < cached.expiresAt) {
            return rebuildResponseWithId(cached.response, query.id)
        }

        val response = withTimeoutOrNull(3000) {
            dohClient.resolve(rawQuery)
        } ?: return DnsPacketParser.buildNxdomainResponse(rawQuery)

        // Cache with parsed TTL (default 60s)
        if (cache.size >= cacheMaxSize) evictOldEntries()
        cache[cacheKey] = DnsCacheEntry(
            response = response,
            expiresAt = System.currentTimeMillis() + extractMinTtl(response) * 1000L
        )

        return response
    }

    private fun rebuildResponseWithId(response: ByteArray, newId: Int): ByteArray {
        val copy = response.copyOf()
        copy[0] = (newId shr 8).toByte()
        copy[1] = newId.toByte()
        return copy
    }

    private fun extractMinTtl(response: ByteArray): Int {
        // Try to extract TTL from first answer record; fallback to 60s
        return try {
            val buf = java.nio.ByteBuffer.wrap(response)
            val anCount = (buf.position(6).getShort(6).toInt() and 0xFFFF)
            if (anCount == 0) return 60
            // Skip header (12 bytes) + question section
            var pos = 12
            // Skip QNAME
            while (pos < response.size && response[pos].toInt() and 0xFF != 0) {
                val len = response[pos].toInt() and 0xFF
                if (len and 0xC0 == 0xC0) { pos += 2; break }
                pos += len + 1
            }
            if (response[pos].toInt() and 0xFF == 0) pos++
            pos += 4 // skip QTYPE + QCLASS
            // Answer record: skip NAME (may be pointer 0xC0xx)
            if (pos + 2 < response.size && response[pos].toInt() and 0xFF == 0xC0) pos += 2
            else while (pos < response.size && response[pos].toInt() and 0xFF != 0) {
                pos += (response[pos].toInt() and 0xFF) + 1
            }
            pos += 1 + 4 // skip null + TYPE + CLASS
            val ttl = ((response[pos].toInt() and 0xFF) shl 24) or
                      ((response[pos+1].toInt() and 0xFF) shl 16) or
                      ((response[pos+2].toInt() and 0xFF) shl 8) or
                      (response[pos+3].toInt() and 0xFF)
            ttl.coerceIn(10, 3600)
        } catch (e: Exception) {
            60
        }
    }

    private fun evictOldEntries() {
        val now = System.currentTimeMillis()
        val expired = cache.entries.filter { it.value.expiresAt < now }.map { it.key }
        expired.forEach { cache.remove(it) }
        // If still too big, remove oldest
        if (cache.size >= cacheMaxSize) {
            cache.entries.sortedBy { it.value.expiresAt }
                .take(cache.size - cacheMaxSize / 2)
                .forEach { cache.remove(it.key) }
        }
    }

    fun clearCache() = cache.clear()
}
