package com.tnhc.shield.dns

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class DohClient(
    @Volatile var serverUrl: String,
    private val client: OkHttpClient
) {

    private val dnsMediaType = "application/dns-message".toMediaType()

    fun updateServerUrl(newUrl: String) {
        serverUrl = newUrl
    }

    suspend fun resolve(dnsQuery: ByteArray): ByteArray? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        // 1. Try DoH first
        val dohResponse = tryResolveDoh(dnsQuery)
        if (dohResponse != null) return@withContext dohResponse

        // 2. Resilient fallback to direct UDP DNS (1.1.1.1 / 8.8.8.8) to guarantee 100% uptime
        tryResolveUdp(dnsQuery, "1.1.1.1") ?: tryResolveUdp(dnsQuery, "8.8.8.8")
    }

    private fun tryResolveDoh(dnsQuery: ByteArray): ByteArray? {
        val request = Request.Builder()
            .url(serverUrl)
            .post(dnsQuery.toRequestBody(dnsMediaType))
            .header("Content-Type", "application/dns-message")
            .header("Accept", "application/dns-message")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.bytes()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun tryResolveUdp(dnsQuery: ByteArray, serverIp: String): ByteArray? {
        return try {
            val socket = java.net.DatagramSocket()
            socket.soTimeout = 1500
            val serverAddr = java.net.InetAddress.getByName(serverIp)
            val sendPacket = java.net.DatagramPacket(dnsQuery, dnsQuery.size, serverAddr, 53)
            socket.send(sendPacket)

            val receiveBuf = ByteArray(4096)
            val receivePacket = java.net.DatagramPacket(receiveBuf, receiveBuf.size)
            socket.receive(receivePacket)
            socket.close()
            receiveBuf.copyOf(receivePacket.length)
        } catch (_: Exception) {
            null
        }
    }
}

object DnsServers {
    val CLOUDFLARE = "https://cloudflare-dns.com/dns-query"
    val GOOGLE = "https://dns.google/dns-query"
    val QUAD9 = "https://dns.quad9.net/dns-query"
    val ADGUARD = "https://dns.adguard-dns.com/dns-query"
    val NEXTDNS = "https://dns.nextdns.io/dns-query"

    val all = listOf(
        DnsServerDef("Cloudflare", CLOUDFLARE, "1.1.1.1 / 1.0.0.1", privacyLevel = 3),
        DnsServerDef("Google", GOOGLE, "8.8.8.8 / 8.8.4.4", privacyLevel = 1),
        DnsServerDef("Quad9", QUAD9, "9.9.9.9 / 149.112.112.112", privacyLevel = 3),
        DnsServerDef("AdGuard", ADGUARD, "94.140.14.14 / 94.140.15.15", privacyLevel = 3),
        DnsServerDef("NextDNS", NEXTDNS, "45.90.28.0 / 45.90.30.0", privacyLevel = 3),
    )
}

data class DnsServerDef(
    val name: String,
    val url: String,
    val ipHint: String,
    val privacyLevel: Int // 1=low, 2=medium, 3=high
)
